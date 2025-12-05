package com.aux_arena.models.session;

import lombok.Data;

@Data
public class GameLobbyMessage {

    private String textMessage;
    private Long messageIndex;
    private String author;

}
