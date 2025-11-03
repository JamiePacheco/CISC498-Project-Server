package com.aux_arena.models.socket.event;

import com.aux_arena.models.tables.LobbyUser;

public class UserJoinedEvent extends GameLobbyEvent{
    private LobbyUser user;
}
