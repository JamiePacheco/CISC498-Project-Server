package com.aux_arena.service.definitions;

import com.aux_arena.models.tables.LobbyUser;
import com.aux_arena.models.tables.User;
import org.springframework.stereotype.Service;

@Service
public interface AuthenticationService {
    User createNewUser(User user);

    LobbyUser createNewLobbyUser(String username, String lobbyCode, Boolean isAuthor);

    User loadUserByUsername(String email);
}