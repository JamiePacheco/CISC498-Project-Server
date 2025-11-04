package com.aux_arena.components;

import com.aux_arena.components.lobby.LobbyManager;
import lombok.AllArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

@Component
@AllArgsConstructor
public class WebSocketEventListener {

    private LobbyManager lobbyManager;

    @EventListener
    public void handleSessionDisconnect(SessionDisconnectEvent event) {
        String sessionId = event.getSessionId();
        lobbyManager.onUserDisconnect(event.getSessionId());
    }

}
