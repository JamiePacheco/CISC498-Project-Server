package com.aux_arena.service.implementations;

import com.aux_arena.components.lobby.GameManager;
import com.aux_arena.models.session.GameSession;
import com.aux_arena.service.definitions.GameSessionService;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@AllArgsConstructor
public class GameSessionServiceImpl implements GameSessionService {

    private GameManager gameManager;

    @Override
    public GameSession getGameSession(Long lobbySessionId) {

        // get the game session currently in memory that is associated with the lobby session
        return null;
    }
}
