package com.example.activitytrade.common.api;

import com.example.activitytrade.common.exception.BizException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ResultTest {

    @Test
    void okWithData() {
        Result<String> r = Result.ok("a");
        assertEquals(0, r.getCode());
        assertEquals("ok", r.getMessage());
        assertEquals("a", r.getData());
    }

    @Test
    void okWithoutData() {
        Result<Void> r = Result.ok();
        assertNull(r.getData());
    }

    @Test
    void failCarriesCode() {
        Result<Void> r = Result.fail(ErrorCode.SOLD_OUT, "已售罄");
        assertEquals(2004, r.getCode());
        assertEquals("已售罄", r.getMessage());
    }

    @Test
    void errorCodeConstantsAreStable() {
        assertEquals(0, ErrorCode.SUCCESS);
        assertEquals(401, ErrorCode.UNAUTHORIZED);
        assertEquals(403, ErrorCode.FORBIDDEN);
        assertEquals(2004, ErrorCode.SOLD_OUT);
        assertEquals(2006, ErrorCode.RATE_LIMITED);
        assertEquals(6001, ErrorCode.SETTLE_MISMATCH);
    }

    @Test
    void bizExceptionExposesCode() {
        BizException e = new BizException(ErrorCode.SYSTEM_ERROR, "boom");
        assertEquals(1000, e.getCode());
        assertEquals("boom", e.getMessage());
        assertThrows(RuntimeException.class, () -> {
            throw e;
        });
    }
}
