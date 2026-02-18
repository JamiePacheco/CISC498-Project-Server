package com.aux_arena.models.session;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class UserSession {
    private Long userId;
    private String tempId; // used for initial transaction and identity validation
    private String sessionId;
    private String displayName;
    private Long lobbyId;
    private String lobbyCode;
    private Instant lastPingTime;
    private Instant joinedAt;
    private Boolean host = false;
    private Boolean active = true;
    private Boolean isSpectator = false;
    private String functionMessage;

    private long errorSequence = 1L;
    private long userEventSequence = 1L;

    public UserSession(String sessionId) {
        this.sessionId = sessionId;
    }

    @Override
    public String toString() {
        return String.format("[%s] %s,%b,%b, (%s)", tempId, displayName, host, active, functionMessage);
    }

    public long getErrorSequenceIndex(){
        Long index = errorSequence;
        this.errorSequence++;
        return index;
    }

    public long getUserEventSequence() {
        Long index = userEventSequence;
        this.userEventSequence++;
        return index;
    }
}
