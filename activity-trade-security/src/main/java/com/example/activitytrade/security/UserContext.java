package com.example.activitytrade.security;

/**
 * 当前登录用户上下文（ThreadLocal）。由 AuthInterceptor 填充，请求结束清理。
 * 使用方式：Long uid = UserContext.getUserId();
 */
public final class UserContext {

    private static final ThreadLocal<Long> USER_ID = new ThreadLocal<>();
    private static final ThreadLocal<String> USERNAME = new ThreadLocal<>();
    private static final ThreadLocal<Integer> ROLE = new ThreadLocal<>();

    private UserContext() {
    }

    public static void set(Long userId, String username, Integer role) {
        USER_ID.set(userId);
        USERNAME.set(username);
        ROLE.set(role);
    }

    public static Long getUserId() {
        return USER_ID.get();
    }

    public static String getUsername() {
        return USERNAME.get();
    }

    public static Integer getRole() {
        return ROLE.get();
    }

    /** 接口返回时是否已登录（用于可选登录接口） */
    public static boolean isLogin() {
        return USER_ID.get() != null;
    }

    public static void clear() {
        USER_ID.remove();
        USERNAME.remove();
        ROLE.remove();
    }
}