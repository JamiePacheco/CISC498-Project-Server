package com.aux_arena.service.definitions;

import com.aux_arena.models.tables.GameLobby;
import org.springframework.stereotype.Service;

@Service
public interface GameLobbyService {

    public GameLobby createGameLobby(GameLobby gameLobby);

    public GameLobby getGameLobby(String lobbyCode);

}
