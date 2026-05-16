package com.aux_arena.components.lobby.model;

import com.aux_arena.models.session.GameLobbyMessage;

public record AtomicOperationResult<T>(T resultContent, GameLobbyMessage resultMessage, Long resultSequence) {}