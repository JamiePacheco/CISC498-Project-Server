package com.aux_arena.models;

import com.aux_arena.models.enums.GameLobbyStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GenerationType;
import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

import java.time.Instant;
import java.time.LocalDateTime;

@Data
@Entity
@Builder
@RequiredArgsConstructor
@NoArgsConstructor
@Table(name = "GAME_LOBBY")
public class GameLobby {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private String id;

    @Column(name = "NAME")
    private String name;

    @Column(name = "STATUS")
    private GameLobbyStatus status;

    @Column(name = "MAX_PLAYERS")
    private int maxPlayers;

    @Column(name = "CREATED_AT")
    private Instant createdAt;
}
