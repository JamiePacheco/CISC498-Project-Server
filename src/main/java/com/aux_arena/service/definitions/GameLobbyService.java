package com.aux_arena.service.definitions;

import com.aux_arena.models.session.LobbySession;
import com.aux_arena.models.tables.GameLobby;
import com.aux_arena.models.tables.LobbyUser;
import org.springframework.stereotype.Service;

@Service
public interface GameLobbyService {

    public GameLobby createGameLobby(GameLobby gameLobby);

    public GameLobby getGameLobby(String lobbyCode, String password);

    public GameLobby getGameLobby(Long id);

}
