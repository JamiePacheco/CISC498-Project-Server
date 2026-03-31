package com.aux_arena.configuration;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.support.DefaultHandshakeHandler;

import java.security.Principal;
import java.util.Arrays;
import java.util.Map;

public class JwtPrincipleHandshakeHandler extends DefaultHandshakeHandler {

    Logger logger = LoggerFactory.getLogger(JwtPrincipleHandshakeHandler.class);

    @Override
    protected Principal determineUser(
            ServerHttpRequest request,
            WebSocketHandler webSocketHandler,
            Map<String, Object> attributes
    ) {

        String userId = (String) attributes.get("userId");

        if (request instanceof ServletServerHttpRequest servletRequest) {
            HttpServletRequest httpRequest = servletRequest.getServletRequest();
            Cookie[] cookies = httpRequest.getCookies();

            if (cookies != null) {
                String principle = Arrays.stream(cookies)
                        .filter(cookie -> "jwt".equals(cookie.getName()))
                        .findFirst()
                        .map(cookie -> cookie.getValue())
                        .orElse(null);

                logger.info("User [{}] JWT [{}]", userId, principle);
            }
        }

        return new Principal() {
            @Override
            public String getName() {
                return userId;
            }
        };
    }
}
