import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicLong;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** M12 压测器：JDK HttpClient 并发爬坡（单文件运行：java Bench.java <activityId>） */
public class Bench {
    static final String BASE = System.getenv().getOrDefault("BENCH_BASE", "http://localhost:18080");
    static final int CONC = Integer.parseInt(System.getenv().getOrDefault("BENCH_CONC", "600"));
    static final int SECS = Integer.parseInt(System.getenv().getOrDefault("BENCH_SECS", "90"));
    static final int USERS = Integer.parseInt(System.getenv().getOrDefault("BENCH_USERS", "600"));

    static final Pattern TOKEN_P = Pattern.compile("\"token\":\"([^\"]+)\"");
    static final Pattern CODE_P = Pattern.compile("\"code\":(-?\\d+)");

    public static void main(String[] args) throws Exception {
        String actId = args.length > 0 ? args[0] : "";
        HttpClient client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build();
        System.out.println("preparing " + USERS + " users...");
        List<String> tokens = new ArrayList<>();
        for (int i = 1; i <= USERS; i++) {
            String u = String.format("bp%03d", i);
            String reqBody = "{\"username\":\"" + u + "\",\"password\":\"user123456\"}";
            HttpRequest r = HttpRequest.newBuilder(URI.create(BASE + "/api/auth/login"))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(reqBody)).build();
            String resp = client.send(r, HttpResponse.BodyHandlers.ofString()).body();
            Matcher m = TOKEN_P.matcher(resp);
            if (m.find()) tokens.add(m.group(1));
        }
        System.out.println("tokens=" + tokens.size());

        ConcurrentLinkedQueue<Long> lats = new ConcurrentLinkedQueue<>();
        AtomicLong ok = new AtomicLong(), sold = new AtomicLong(), err = new AtomicLong(), req = new AtomicLong();
        CountDownLatch worker = new CountDownLatch(CONC);
        ExecutorService pool = Executors.newFixedThreadPool(CONC);
        long end = System.currentTimeMillis() + SECS * 1000L;
        for (int t = 0; t < CONC; t++) {
            final int f = t;
            pool.submit(() -> {
                try {
                    while (System.currentTimeMillis() < end) {
                        String tk = tokens.get(f);
                        String body = "{\"activityId\":\"" + actId + "\",\"skuId\":1001}";
                        HttpRequest rr = HttpRequest.newBuilder(URI.create(BASE + "/api/seckill/order"))
                                .header("Content-Type", "application/json")
                                .header("Authorization", "Bearer " + tk)
                                .POST(HttpRequest.BodyPublishers.ofString(body)).build();
                        long s0 = System.nanoTime();
                        try {
                            HttpResponse<String> resp = client.send(rr, HttpResponse.BodyHandlers.ofString());
                            long ms = (System.nanoTime() - s0) / 1_000_000L;
                            lats.add(ms);
                            req.incrementAndGet();
                            Matcher cm = CODE_P.matcher(resp.body());
                            int code = cm.find() ? Integer.parseInt(cm.group(1)) : -999;
                            if (code == 0) ok.incrementAndGet();
                            else if (code == 2004 || code == 2005) sold.incrementAndGet();
                            else err.incrementAndGet();
                        } catch (Exception ex) {
                            err.incrementAndGet();
                        }
                    }
                } finally {
                    worker.countDown();
                }
            });
        }
        worker.await();
        pool.shutdown();
        List<Long> sorted = new ArrayList<>(lats);
        sorted.sort(null);
        int n = sorted.size();
        double p95 = n > 0 ? sorted.get(Math.min(n - 1, (int) (n * 0.95))) : 0;
        double p99 = n > 0 ? sorted.get(Math.min(n - 1, (int) (n * 0.99))) : 0;
        double avg = n > 0 ? sorted.stream().mapToLong(Long::longValue).average().orElse(0) : 0;
        double qps = req.get() / (double) SECS;
        System.out.printf("CONC=%d SECS=%ds req=%d ok=%d sold=%d err=%d QPS=%.1f avg_ms=%.1f p95_ms=%.1f p99_ms=%.1f%n",
                CONC, SECS, req.get(), ok.get(), sold.get(), err.get(), qps, avg, p95, p99);
    }
}