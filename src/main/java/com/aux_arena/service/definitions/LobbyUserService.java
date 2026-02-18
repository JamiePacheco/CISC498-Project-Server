package com.aux_arena.service.definitions;

import com.aux_arena.models.session.UserSession;
import com.aux_arena.models.tables.GameLobby;
import com.aux_arena.models.tables.LobbyUser;
import com.aux_arena.repository.LobbyUserRepository;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public interface LobbyUserService {

    LobbyUser getLobbyUser(String username);

    LobbyUser saveLobbyUser(LobbyUser lobbyUser);

    List<LobbyUser> saveLobbyUsers(List<UserSession> userSessions, GameLobby gameLobby);

}
