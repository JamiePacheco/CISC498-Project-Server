package com.aux_arena.service.implementations;

import com.aux_arena.components.lobby.LobbyManager;
import com.aux_arena.models.enums.GameLobbyStatus;
import com.aux_arena.models.enums.Roles;
import com.aux_arena.models.session.LobbySession;
import com.aux_arena.models.tables.GameLobby;
import com.aux_arena.models.tables.LobbyUser;
import com.aux_arena.repository.GameLobbyRepository;
import com.aux_arena.service.definitions.GameLobbyService;
import com.aux_arena.utility.UuidGenerator;
import lombok.AllArgsConstructor;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Instant;

@Service
@AllArgsConstructor
public class GameLobbyServiceImpl implements GameLobbyService {

    private GameLobbyRepository gameLobbyRepository;

    private LobbyManager lobbyManager;

    @Override
    public GameLobby createGameLobby(GameLobby gameLobby) {
        gameLobby.setCreatedAt(Instant.now());
        gameLobby.setLobbyCode(UuidGenerator.generateUuid().substring(0, 8));
        gameLobby.setStatus(GameLobbyStatus.WAITING);

        GameLobby newGameLobby = gameLobbyRepository.save(gameLobby);
        if (newGameLobby == null) {
            throw new RuntimeException("Error saving new Game Lobby");
        }
        return newGameLobby;
    }

    @Override
    public GameLobby getGameLobby(String lobbyCode, String password) {
        GameLobby gameLobby = gameLobbyRepository.findGameLobbiesByLobbyCode(lobbyCode);
        if (gameLobby == null) {
            throw new RuntimeException("Lobby with code '" + lobbyCode + "' does not exist");
        }

        if (gameLobby.isPrivateStatus() && !gameLobby.getPassword().equals(password)) {
            throw new RuntimeException("Incorrect password for private lobby");
        }

        return gameLobby;
    }

    @Override
    public GameLobby getGameLobby(Long gameLobbyId) {
        GameLobby gameLobby = gameLobbyRepository.findGameLobbiesById(gameLobbyId);
        if (gameLobby == null) {
            throw new RuntimeException("Lobby with id '" + gameLobbyId + "' does not exist");
        }
        return gameLobby;
    }

}
