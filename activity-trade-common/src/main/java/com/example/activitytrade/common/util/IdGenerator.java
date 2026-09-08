package com.example.activitytrade.common.util;

/**
 * 雪花 ID 生成（单机版）：41bit 毫秒时间戳 + 10bit worker + 12bit 序列。
 * 用于 order_no / pay_no / right_no / biz_no 等业务号。
 */
public final class IdGenerator {

    /** 起始时间戳：2025-01-01 00:00:00 UTC+8 */
    private static final long EPOCH = 1735689600000L;
    private static final long WORKER_ID = 1L;
    private static final long SEQ_BITS = 12L;
    private static final long SEQ_MASK = (1L << SEQ_BITS) - 1L;
    private static final long WORKER_SHIFT = SEQ_BITS;
    private static final long TS_SHIFT = SEQ_BITS + 10L;

    private static long lastTs = -1L;
    private static long seq = 0L;

    private IdGenerator() {
    }

    public static synchronized long nextId() {
        long ts = System.currentTimeMillis();
        if (ts < lastTs) {
            // 时钟回拨：退化为上一毫秒，保证递增
            ts = lastTs;
        }
        if (ts == lastTs) {
            seq = (seq + 1) & SEQ_MASK;
            if (seq == 0) {
                ts++;
            }
        } else {
            seq = 0;
        }
        lastTs = ts;
        return ((ts - EPOCH) << TS_SHIFT) | (WORKER_ID << WORKER_SHIFT) | seq;
    }

    public static String nextIdStr() {
        return Long.toString(nextId());
    }
}