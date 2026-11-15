package com.sras.backend.security;

import com.sras.backend.entity.User;
import com.sras.backend.service.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
@RequiredArgsConstructor
public class RequireRolesInterceptor implements HandlerInterceptor {

    private final AuthService authService;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        response.setHeader("Cache-Control", "no-cache, no-store, max-age=0, must-revalidate");
        response.setHeader("Pragma", "no-cache");
        response.setHeader("Expires", "0");

        if (!(handler instanceof HandlerMethod handlerMethod)) {
            return true;
        }

        RequireRoles requireRoles = handlerMethod.getMethodAnnotation(RequireRoles.class);
        if (requireRoles == null) {
            requireRoles = AnnotatedElementUtils.findMergedAnnotation(handlerMethod.getBeanType(), RequireRoles.class);
        }

        if (requireRoles != null && requireRoles.value().length > 0) {
            HttpSession session = request.getSession(false);
            String xUserId = request.getHeader("X-User-Id");

            User user = authService.requireRole(session, xUserId, requireRoles.value());
            request.setAttribute(CurrentUserArgumentResolver.CURRENT_USER_ATTR, user);
        }

        return true;
    }
}
