package com.sras.backend.security;

import com.sras.backend.entity.User;
import com.sras.backend.exception.ForbiddenException;
import com.sras.backend.service.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.core.MethodParameter;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

@Component
@RequiredArgsConstructor
public class CurrentUserArgumentResolver implements HandlerMethodArgumentResolver {

    public static final String CURRENT_USER_ATTR = "CURRENT_LOGGED_IN_USER";

    private final AuthService authService;

    @Override
    public boolean supportsParameter(MethodParameter parameter) {
        return parameter.hasParameterAnnotation(CurrentUser.class)
                && User.class.isAssignableFrom(parameter.getParameterType());
    }

    @Override
    public Object resolveArgument(MethodParameter parameter,
                                  ModelAndViewContainer mavContainer,
                                  NativeWebRequest webRequest,
                                  WebDataBinderFactory binderFactory) {
        // 1. Check if user was already resolved in interceptor
        Object cachedUser = webRequest.getAttribute(CURRENT_USER_ATTR, RequestAttributes.SCOPE_REQUEST);
        if (cachedUser instanceof User user) {
            return user;
        }

        // 2. Resolve via session or X-User-Id header
        HttpServletRequest httpRequest = webRequest.getNativeRequest(HttpServletRequest.class);
        HttpSession session = httpRequest != null ? httpRequest.getSession(false) : null;
        String xUserId = webRequest.getHeader("X-User-Id");

        CurrentUser anno = parameter.getParameterAnnotation(CurrentUser.class);
        boolean required = anno == null || anno.required();

        try {
            User user = authService.resolveCurrentUser(session, xUserId);
            if (user != null) {
                webRequest.setAttribute(CURRENT_USER_ATTR, user, RequestAttributes.SCOPE_REQUEST);
            }
            return user;
        } catch (Exception e) {
            if (required) {
                throw e;
            }
            return null;
        }
    }
}
