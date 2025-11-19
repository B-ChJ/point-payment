package com.sparta.payment_system.security;

import com.sparta.payment_system.service.BlacklistService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
public class JwtFilter extends OncePerRequestFilter {
    private final JwtUtil jwtUtil;
    private BlacklistService blacklistService;

    public JwtFilter(JwtUtil jwtUtil) {
        this.jwtUtil = jwtUtil;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String accessToken = resolveAccessToken(request);

        //AccessToken Filter
        if (StringUtils.hasText(accessToken) && jwtUtil.validateAccessToken(accessToken)) {
            Authentication authentication = jwtUtil.getAuthentication(accessToken);
            SecurityContextHolder.getContext().setAuthentication(authentication);
        } else {
            String refreshToken = resolveRefreshToken(request);
            if (StringUtils.hasText(refreshToken) && jwtUtil.validateRefreshToken(refreshToken)) {
                if (blacklistService.isBlacklisted(refreshToken)) {
                    response.setStatus((HttpServletResponse.SC_UNAUTHORIZED)); // 401 errorCode response return void
                    return; //chain 끊기
                }
                Authentication authentication = jwtUtil.getAuthentication(refreshToken);
                String newAccessToken = jwtUtil.createAccessToken(authentication);

                response.setHeader("Authorization", "Bearer " + newAccessToken);
                SecurityContextHolder.getContext().setAuthentication(authentication);
            } else {
                response.setStatus((HttpServletResponse.SC_UNAUTHORIZED));
                return;
            }
        }
        filterChain.doFilter(request, response);
    }

    private String resolveAccessToken(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7, bearerToken.length());
        }
        return null;
    }

    private String resolveRefreshToken(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7, bearerToken.length());
        }
        return null;
    }
}
