package com.example.activitytrade.settle.observe;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

/**
 * QPS 计数（M9，§8.4）：每 10s 落一个采样点，内存环形队列保留 2 小时（720 点）。
 */
@Component
public class QpsCounter {

    private static final Logger log = LoggerFactory.getLogger(QpsCounter.class);
    private static final int SAMPLE_MS = 10_000;
    private static final int MAX_SAMPLES = 720;

    private final AtomicLong counter = new AtomicLong();
    private final Deque<Sample> samples = new ArrayDeque<>(MAX_SAMPLES + 1);

    /** 每请求调用一次 */
    public void hit() {
        counter.incrementAndGet();
    }

    /** 每 10s 落点 */
    @Scheduled(fixedDelay = SAMPLE_MS)
    public void snapshot() {
        long count = counter.getAndSet(0);
        synchronized (samples) {
            samples.addLast(new Sample(System.currentTimeMillis(), count));
            while (samples.size() > MAX_SAMPLES) {
                samples.removeFirst();
            }
        }
    }

    /** 最近 minutes 分钟内的采样点（按时间升序）；seconds 粒度余量 */
    public List<Sample> recent(int minutes) {
        long since = System.currentTimeMillis() - minutes * 60_000L;
        List<Sample> result = new ArrayList<>();
        synchronized (samples) {
            for (Sample s : samples) {
                if (s.timestamp >= since) {
                    result.add(s);
                }
            }
        }
        return result;
    }

    /** 采样点 */
    public record Sample(long timestamp, long count) {
    }
}