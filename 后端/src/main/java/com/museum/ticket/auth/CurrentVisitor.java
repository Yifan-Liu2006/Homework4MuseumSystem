package com.museum.ticket.auth;

public final class CurrentVisitor {
    private static final ThreadLocal<String> VISITOR_ID = new ThreadLocal<>();

    private CurrentVisitor() {
    }

    public static void setVisitorId(String visitorId) {
        VISITOR_ID.set(visitorId);
    }

    public static String requireVisitorId() {
        String visitorId = VISITOR_ID.get();
        if (visitorId == null) {
            throw new IllegalStateException("当前请求没有游客身份");
        }
        return visitorId;
    }

    public static void clear() {
        VISITOR_ID.remove();
    }
}
