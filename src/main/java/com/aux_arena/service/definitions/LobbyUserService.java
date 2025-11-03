package com.aux_arena.service.definitions;

import com.aux_arena.models.tables.LobbyUser;
import com.aux_arena.repository.LobbyUserRepository;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

@Service
public interface LobbyUserService {

    LobbyUser getLo();

    LobbyUser saveLobbyUser(LobbyUser lobbyUser);

}
