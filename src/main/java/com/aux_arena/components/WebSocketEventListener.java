package com.aux_arena.components;

import com.aux_arena.components.lobby.LobbyManager;
import lombok.AllArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

import java.security.Principal;

@Component
@AllArgsConstructor
public class WebSocketEventListener {

    private LobbyManager lobbyManager;

    private final static Logger log = LoggerFactory.getLogger(WebSocketEventListener.class);

    @EventListener
    public void handleSessionDisconnect(SessionDisconnectEvent event) {
        Principal principal = event.getUser();
        lobbyManager.onUserDisconnect(principal);
        log.info("Disconnecting user {}", principal.getName());
    }

}
