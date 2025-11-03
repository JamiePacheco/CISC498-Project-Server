package com.aux_arena.models.socket.event;

import com.aux_arena.models.enums.message.MessageEvent;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public abstract class GameLobbyEvent<T> {
    private MessageEvent type;
    private String message;
    private T payload;
    private Instant timestamp;
}