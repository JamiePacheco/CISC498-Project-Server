package com.aux_arena.service.definitions;

import com.aux_arena.models.session.GameSession;
import org.springframework.stereotype.Service;

@Service
public interface GameSessionService {

    GameSession getGameSession(Long lobbySessionId);


}
