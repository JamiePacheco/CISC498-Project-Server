package com.aux_arena.service.implementations;

import com.aux_arena.models.tables.GameLobby;
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

    @Override
    public GameLobby createGameLobby(GameLobby gameLobby) {
        gameLobby.setCreatedAt(Instant.now());
        gameLobby.setLobbyCode(UuidGenerator.generateUuid());

        GameLobby newGameLobby = gameLobbyRepository.save(gameLobby);
        if (newGameLobby == null) {
            throw new RuntimeException("Error saving new Game Lobby");
        }
        return newGameLobby;
    }

    @Override
    public GameLobby getGameLobby(String lobbyCode) {
        GameLobby gameLobby = gameLobbyRepository.findGameLobbiesByLobbyCode(lobbyCode);
        if (gameLobby == null) {
            throw new RuntimeException("Lobby with code '" + lobbyCode + "' does not exist");
        }
        return gameLobby;
    }
}
