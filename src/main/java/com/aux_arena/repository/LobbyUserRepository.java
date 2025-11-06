package com.aux_arena.repository;

import com.aux_arena.models.tables.LobbyUser;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface LobbyUserRepository extends JpaRepository<LobbyUser, String> {
}
