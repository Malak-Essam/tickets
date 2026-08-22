package com.example.tickets.filter;

import java.io.IOException;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.filter.OncePerRequestFilter;

import com.example.tickets.domain.User;
import com.example.tickets.service.UserProvisioningService;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class UserProvisioningFilter extends OncePerRequestFilter {

    public static final String CURRENT_USER_ATTRIBUTE = "currentUser";

    private final UserProvisioningService userProvisioningService;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
        throws ServletException, IOException {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication instanceof JwtAuthenticationToken jwtAuthentication) {
            User user = userProvisioningService.upsert(jwtAuthentication.getToken());
            if (user != null) {
                request.setAttribute(CURRENT_USER_ATTRIBUTE, user);
            }
        }
        filterChain.doFilter(request, response);
    }
}
