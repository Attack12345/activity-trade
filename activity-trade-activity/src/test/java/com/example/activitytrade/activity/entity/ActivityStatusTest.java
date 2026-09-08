package com.example.activitytrade.activity.entity;

import com.example.activitytrade.common.exception.BizException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ActivityStatusTest {

    @Test
    void sequentialTransitions() {
        assertEquals(1, ActivityStatus.DRAFT.transitTo(1).code());
        assertEquals(2, ActivityStatus.PREHEAT.transitTo(2).code());
        assertEquals(3, ActivityStatus.ONGOING.transitTo(3).code());
    }

    @Test
    void invalidJumpRejected() {
        assertThrows(BizException.class, () -> ActivityStatus.DRAFT.transitTo(2));
        assertThrows(BizException.class, () -> ActivityStatus.DRAFT.transitTo(3));
        assertThrows(BizException.class, () -> ActivityStatus.PREHEAT.transitTo(3));
        assertThrows(BizException.class, () -> ActivityStatus.ONGOING.transitTo(1));
    }

    @Test
    void unknownCodeRejected() {
        assertThrows(BizException.class, () -> ActivityStatus.of(9));
        assertThrows(BizException.class, () -> ActivityStatus.of(-1));
    }
}