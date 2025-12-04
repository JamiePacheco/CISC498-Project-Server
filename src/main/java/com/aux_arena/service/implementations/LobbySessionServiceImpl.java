package com.aux_arena.service.implementations;

import com.aux_arena.components.lobby.LobbyManager;
import com.aux_arena.models.enums.GameLobbyStatus;
import com.aux_arena.models.enums.Roles;
import com.aux_arena.models.session.LobbySession;
import com.aux_arena.models.session.UserSession;
import com.aux_arena.models.tables.GameLobby;
import com.aux_arena.models.tables.LobbyUser;
import com.aux_arena.service.definitions.GameLobbyService;
import com.aux_arena.service.definitions.LobbySessionService;
import com.aux_arena.utility.UuidGenerator;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Instant;

@Service
@AllArgsConstructor
public class LobbySessionServiceImpl implements LobbySessionService {

    private LobbyManager lobbyManager;

    private GameLobbyService gameLobbyService;

    @Override
    public LobbySession connectToGameLobby(String lobbyCode, String password, String tempId, String principleUser, LobbyUser lobbyUser) {
        GameLobby gameLobby = gameLobbyService.getGameLobby(lobbyCode, password);

        LobbySession connectedLobbySession = lobbyManager.loadGameLobby(gameLobby);

        UserSession oldConnection = principleUser != null ? connectedLobbySession.getActiveUsers().get(principleUser) : null;

        if (oldConnection != null) {
            // we do this here in order to have up-to-date access to this user
            oldConnection.setTempId(tempId);
            oldConnection.setActive(true);
        } else if (lobbyUser.getRole() == Roles.GUEST) {
            // set the guest identifier for jwt generation.
            lobbyUser.setGuestIdentifier(UuidGenerator.generateUuid());
        }

//        LobbySession session = lobbyManager.onUserConnect(gameLobby.getId(), lobbyUser, );
        return connectedLobbySession;
    }

    // TODO finish implementing starting game lobby
    @Override
    public void startGameLobby(Long lobbyId) {

        LobbySession lobbySession = lobbyManager.startGameLobby(lobbyId);




    }

}
