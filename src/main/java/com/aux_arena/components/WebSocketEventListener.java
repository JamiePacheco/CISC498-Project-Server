package com.aux_arena.components;

import com.aux_arena.components.lobby.GameManager;
import com.aux_arena.components.lobby.LobbyManager;
import com.aux_arena.models.session.LobbySession;
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

    private GameManager gameManager;

    private final static Logger log = LoggerFactory.getLogger(WebSocketEventListener.class);

    @EventListener
    public void handleSessionDisconnect(SessionDisconnectEvent event) {
        Principal principal = event.getUser();
        gameManager.disconnectUser(principal);
        log.info("Disconnecting user {}", principal.getName());
    }

}
