package com.museum.ticket.admin;

import com.museum.ticket.common.UnauthorizedException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
public class AdminAuthInterceptor implements HandlerInterceptor {
    private final AdminJwtService adminJwtService;

    public AdminAuthInterceptor(AdminJwtService adminJwtService) {
        this.adminJwtService = adminJwtService;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        String authorization = request.getHeader("Authorization");
        if (authorization == null || !authorization.startsWith("Bearer ")) {
            throw new UnauthorizedException("请先登录管理端");
        }
        AdminCurrent.set(adminJwtService.parse(authorization.substring(7)));
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler,
                                Exception exception) {
        AdminCurrent.clear();
    }
}
