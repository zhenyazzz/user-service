package com.innowise.internship.userservice.security;

import java.io.IOException;
import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import com.innowise.internship.userservice.dto.response.ErrorResponse;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import lombok.RequiredArgsConstructor;
import tools.jackson.databind.ObjectMapper;

@Component
@RequiredArgsConstructor
public class HeaderAuthenticationFilter extends OncePerRequestFilter {

    private final ObjectMapper objectMapper;

    @Override
    protected void doFilterInternal(HttpServletRequest request, 
                                    HttpServletResponse response, 
                                    FilterChain filterChain) throws ServletException, IOException {

        String userIdHeader = request.getHeader("X-User-Id");
        String emailHeader = request.getHeader("X-User-Email");
        String roleHeader = request.getHeader("X-User-Role");

        if (userIdHeader != null) {
            try {
                CurrentUser currentUser = new CurrentUser(
                    UUID.fromString(userIdHeader),
                    emailHeader,
                    roleHeader
                );

                var authorities = (roleHeader != null)
                    ? List.of(new SimpleGrantedAuthority(roleHeader))
                    : Collections.<SimpleGrantedAuthority>emptyList();

                UsernamePasswordAuthenticationToken auth =
                    new UsernamePasswordAuthenticationToken(currentUser, null, authorities);

                SecurityContextHolder.getContext().setAuthentication(auth);
            } catch (IllegalArgumentException e) {
                sendInvalidAuthHeaderResponse(response);
                return;
            }
        }

        filterChain.doFilter(request, response);
    }

    private void sendInvalidAuthHeaderResponse(HttpServletResponse response) throws IOException {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        ErrorResponse errorResponse = new ErrorResponse(
            "INVALID_AUTH_HEADER",
            "Invalid X-User-Id format",
            Instant.now(),
            null
        );
        objectMapper.writeValue(response.getWriter(), errorResponse);
    }
}
