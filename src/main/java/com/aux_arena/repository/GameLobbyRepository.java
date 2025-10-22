package com.aux_arena.repository;

import com.aux_arena.models.tables.GameLobby;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface GameLobbyRepository extends JpaRepository<GameLobby, Long> {

    GameLobby findGameLobbiesByLobbyCode(String lobbyCode);

}
