package com.example.activitytrade.pay.util;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;

/**
 * HMAC-SHA256 签名工具（Mock 支付网关预签名/验签）。
 * sign = hex(HMAC_SHA256(secret, payNo:orderNo:amount))
 */
public final class SignUtil {

    private SignUtil() {
    }

    public static String sign(String secret, String payNo, String orderNo, String amount) {
        String data = payNo + ":" + orderNo + ":" + amount;
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            byte[] raw = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder(raw.length * 2);
            for (byte b : raw) {
                hex.append(Character.forDigit((b >> 4) & 0xF, 16)).append(Character.forDigit(b & 0xF, 16));
            }
            return hex.toString();
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("hmac error", e);
        }
    }

    public static boolean verify(String secret, String payNo, String orderNo, String amount, String expected) {
        if (expected == null) {
            return false;
        }
        return constantTimeEquals(sign(secret, payNo, orderNo, amount), expected);
    }

    private static boolean constantTimeEquals(String a, String b) {
        if (a.length() != b.length()) {
            return false;
        }
        int diff = 0;
        for (int i = 0; i < a.length(); i++) {
            diff |= a.charAt(i) ^ b.charAt(i);
        }
        return diff == 0;
    }
}