package com.aux_arena.models.session;

import lombok.Builder;
import lombok.Data;

import java.time.Instant;

@Data
@Builder
public class UserSession {
    private Long userId;
    private Long sessionID;
    private String displayName;
    private String lobbyId;
    private Instant lastPingTime;
    private Boolean isReady;
    private Boolean isSpectator;


    public UserSession(Long sessionId) {
        this.sessionID = sessionId;
    }

}
