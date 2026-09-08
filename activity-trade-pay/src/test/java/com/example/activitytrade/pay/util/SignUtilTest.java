package com.example.activitytrade.pay.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SignUtilTest {

    @Test
    void signDeterministic() {
        String a = SignUtil.sign("secret", "p1", "o1", "99.00");
        String b = SignUtil.sign("secret", "p1", "o1", "99.00");
        assertEquals(a, b);
        assertEquals(64, a.length());
    }

    @Test
    void verifyMatchesAndRejects() {
        String sign = SignUtil.sign("secret", "p1", "o1", "99.00");
        assertTrue(SignUtil.verify("secret", "p1", "o1", "99.00", sign));
        assertFalse(SignUtil.verify("secret", "p1", "o1", "100.00", sign));
        assertFalse(SignUtil.verify("other", "p1", "o1", "99.00", sign));
        assertFalse(SignUtil.verify("secret", "p2", "o1", "99.00", sign));
        assertFalse(SignUtil.verify("secret", "p1", "o1", "99.00", null));
    }
}