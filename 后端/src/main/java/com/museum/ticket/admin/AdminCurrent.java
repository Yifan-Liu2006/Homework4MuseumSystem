package com.museum.ticket.admin;

public final class AdminCurrent {
    private static final ThreadLocal<AdminPrincipal> PRINCIPAL = new ThreadLocal<>();

    private AdminCurrent() {
    }

    public static void set(AdminPrincipal principal) {
        PRINCIPAL.set(principal);
    }

    public static AdminPrincipal require() {
        AdminPrincipal principal = PRINCIPAL.get();
        if (principal == null) {
            throw new IllegalStateException("当前请求没有管理员身份");
        }
        return principal;
    }

    public static void clear() {
        PRINCIPAL.remove();
    }

    public record AdminPrincipal(String workerId, String roleId, String roleName) {
    }
}
