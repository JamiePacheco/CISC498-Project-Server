package com.aux_arena.models.session;

import lombok.Builder;
import lombok.Data;

import java.time.Instant;


// represents a text message in game lobby
@Data
@Builder
public class GameLobbyMessage {

    private String textMessage;
    private Long messageIndex;
    private String author;
    private String authorId; // for system messages this field is null
    private Instant timestamp;

}
