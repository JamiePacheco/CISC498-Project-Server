package com.aux_arena.models.session;

import lombok.Builder;
import lombok.Data;

import java.time.Instant;

@Data
@Builder
public class UserSession {
    private Long userId;
    private String sessionID;
    private String displayName;
    private Long lobbyId;
    private Instant lastPingTime;
    private Boolean active = true;
    private Boolean isSpectator = false;


    public UserSession(String sessionId) {
        this.sessionID = sessionId;
    }

}
