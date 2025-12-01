package com.aux_arena.configuration;

import com.aux_arena.models.tables.LobbyUser;
import com.aux_arena.service.definitions.LobbyUserService;
import com.aux_arena.service.definitions.UserService;
import com.aux_arena.service.implementations.LobbyUserServiceImpl;
import com.aux_arena.utility.JwtUtil;
import jakarta.servlet.http.Cookie;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;

import java.util.List;
import java.util.Map;
import java.util.Set;

@Component
public class JwtHandshakeInterceptor implements HandshakeInterceptor {

    private final JwtUtil jwtUtil;
    private UserService userService;

    private LobbyUserService lobbyUserService;

    private final Logger log = LoggerFactory.getLogger(JwtHandshakeInterceptor.class);

    public JwtHandshakeInterceptor(JwtUtil jwtUtil, UserService userService, LobbyUserService lobbyUserService) {
        this.jwtUtil = jwtUtil;
        this.userService = userService;
        this.lobbyUserService = lobbyUserService;
    }

    @Override
    public boolean beforeHandshake(
            ServerHttpRequest request,
            ServerHttpResponse response,
            WebSocketHandler wsHandler,
            Map<String, Object> attributes
    ) {
        ServletServerHttpRequest servletRequest = (ServletServerHttpRequest) request;
        HttpServletRequest httpServletRequest = servletRequest.getServletRequest();

        String token = getTokenFromCookie(httpServletRequest);
        if (token != null && jwtUtil.validateToken(token)) {
            String username = jwtUtil.extractUsername(token);
            UserDetails user = userService.loadUserByUsername(username);
            if (user == null) {
                user = User.withUsername(username)
                        .password("")
                        .roles("GUEST")
                        .build();
            }
            UsernamePasswordAuthenticationToken auth
                    = new UsernamePasswordAuthenticationToken(user, null, user.getAuthorities());
            attributes.put("auth", auth);
            attributes.put("userId", username);
            return true;
        }

        return false;
    }

    public void afterHandshake(
            ServerHttpRequest request,
            ServerHttpResponse response,
            WebSocketHandler webSocketHandler,
            Exception exception
    ) {}

    private String getTokenFromRequest(ServerHttpRequest request) {
        List<String> authHeaders = request.getHeaders().get("Authorization");
        if (authHeaders != null && !authHeaders.isEmpty()) {
            String header = authHeaders.get(0);
            if (header.startsWith("Bearer ")) {
                return header.substring(7);
            }
        }

        return null;
    }

    public String getTokenFromCookie(HttpServletRequest request) {
        // check for the token in the request cookies
        if (request.getCookies() != null) {
            for (Cookie cookie : request.getCookies()) {
                if ("jwt".equals(cookie.getName())) {
                    String token = cookie.getValue();
                    return token;
                }
            }
        }
        return null;
    }
}
