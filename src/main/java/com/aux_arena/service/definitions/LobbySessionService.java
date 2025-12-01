package com.aux_arena.service.definitions;

import com.aux_arena.models.session.LobbySession;
import com.aux_arena.models.session.UserSession;
import com.aux_arena.models.tables.GameLobby;
import com.aux_arena.models.tables.LobbyUser;
import org.springframework.stereotype.Service;

@Service
public interface LobbySessionService {

    LobbySession connectToGameLobby(String lobbyCode, String password, String tempId, String principleUser, LobbyUser lobbyUser);

}
