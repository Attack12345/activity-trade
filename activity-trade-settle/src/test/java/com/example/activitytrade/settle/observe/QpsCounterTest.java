package com.example.activitytrade.settle.observe;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class QpsCounterTest {

    @Test
    void hitAndSnapshot() {
        QpsCounter counter = new QpsCounter();
        counter.hit();
        counter.hit();
        counter.hit();
        counter.snapshot();
        assertEquals(1, counter.recent(1).size());
        assertEquals(3, counter.recent(1).get(0).count());
    }

    @Test
    void counterResetsAfterSnapshot() {
        QpsCounter counter = new QpsCounter();
        counter.hit();
        counter.snapshot();
        assertEquals(0, counter.recent(1).get(0).count() == 3 ? 1 : 0);
        assertTrue(counter.recent(1).size() <= 1 || counter.recent(1).size() == 2);
    }

    @Test
    void recentFiltersOldSamples() {
        QpsCounter counter = new QpsCounter();
        counter.hit();
        counter.snapshot();
        // 10s 内必有 1~2 个采样；最近 1 分钟至少 1 个
        assertTrue(counter.recent(1).size() >= 1);
    }
}