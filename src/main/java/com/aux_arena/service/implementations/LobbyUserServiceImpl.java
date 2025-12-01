package com.aux_arena.service.implementations;

import com.aux_arena.models.tables.LobbyUser;
import com.aux_arena.repository.LobbyUserRepository;
import com.aux_arena.service.definitions.LobbyUserService;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@AllArgsConstructor
public class LobbyUserServiceImpl implements LobbyUserService {

    private LobbyUserRepository lobbyUserRepository;

    @Override
    public LobbyUser getLobbyUser(String username) {
        return lobbyUserRepository.findLobbyUserByGuestIdentifier(username);
    }

    @Override
    public LobbyUser saveLobbyUser(LobbyUser lobbyUser) {
        return null;
    }
}
