package com.aux_arena.service.definitions;

import com.aux_arena.models.session.LobbySession;
import com.aux_arena.models.tables.LobbyUser;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public interface LobbySessionService {

    LobbySession connectToGameLobby(
            String lobbyCode,
            String password,
            String tempId,
            String principleUser,
            LobbyUser lobbyUser
    );

    List<LobbyUser> startGameLobby(
            Long lobbyId
    );
}
