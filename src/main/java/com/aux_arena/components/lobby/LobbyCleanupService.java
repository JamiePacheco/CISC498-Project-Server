package com.aux_arena.components.lobby;

import lombok.AllArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@AllArgsConstructor
public class LobbyCleanupService {

    private LobbyManager lobbyManager;

    @Scheduled(fixedRate = 600_000)
    public void cleanupLobbies() {
        lobbyManager.cleanupInactiveUsers();
    }



}
