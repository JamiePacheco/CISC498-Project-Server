package com.aux_arena.models.session;

import lombok.Builder;
import lombok.Data;

import java.time.Instant;

@Data
@Builder
public class GameLobbyMessage {

    private String textMessage;
    private Long messageIndex;
    private String author;
    private Instant timestamp;

}
