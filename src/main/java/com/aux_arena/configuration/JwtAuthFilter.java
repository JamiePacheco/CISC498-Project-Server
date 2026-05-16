package com.aux_arena.configuration;

import com.aux_arena.service.definitions.LobbyUserService;
import com.aux_arena.utility.JwtUtil;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Set;

@Component
public class JwtAuthFilter extends OncePerRequestFilter {

    private final JwtUtil jwtUtil;
    private final UserDetailsService userDetailsService;

    private final LobbyUserService lobbyUserService;

    private final Set<String> PUBLIC_ENDPOINTS = Set.of(
            "/api/v1/auth/**",
            "/api/v1/game-lobby",
            "/api/v1/lobby-session/connect",
            "/ws/**",
            "/swagger-ui/**",
            "/v3/api-docs/**",
            "/swagger-resources/**",
            "/swagger-ui.html",
            "/webjars/**"
    );

    private static final Logger log = LoggerFactory.getLogger(JwtAuthFilter.class);

    public JwtAuthFilter(JwtUtil jwtUtil, UserDetailsService userDetailsService, LobbyUserService lobbyUserService) {
        this.jwtUtil = jwtUtil;
        this.userDetailsService = userDetailsService;
        this.lobbyUserService = lobbyUserService;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) throws ServletException {
        String path = request.getServletPath();
        log.info("Request Servlet Path: {}", path);
        // TODO make sure that this works, if not implement iteration method
        return PUBLIC_ENDPOINTS.contains(path);
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {

        String authHeader = request.getHeader("Authorization");
        String token = null;
        String username = null;

        // check for token in the  authorization header
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            token = authHeader.substring(7);
            username = jwtUtil.extractUsername(token);
        }

        // check for the token in the request cookies
        if (request.getCookies() != null) {
            for (Cookie cookie : request.getCookies()) {
                if ("jwt".equals(cookie.getName())) {
                    token = cookie.getValue();
                    username = jwtUtil.extractUsername(token);
                }
            }
        }

        if (username != null && SecurityContextHolder.getContext().getAuthentication() == null) {

            // TODO this should check UserSession DB instead of email so change it
            UserDetails userDetails = userDetailsService.loadUserByUsername(username);
            if (userDetails == null) {
                log.info("Creating guest account for endpoint [{}]", request.getServletPath());
                userDetails = User.withUsername(username)
                        .password("")
                        .roles("GUEST")
                        .build();
            }

            if (jwtUtil.validateToken(token)) {
                UsernamePasswordAuthenticationToken authToken =
                        new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
                authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                SecurityContextHolder.getContext().setAuthentication(authToken);
            }
        }

        filterChain.doFilter(request, response);
    }
}