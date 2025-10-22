package com.aux_arena.models.tables;

import com.aux_arena.models.enums.GameLobbyStatus;
import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.relational.core.mapping.Table;

import java.time.Instant;

@Data
@Entity
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "GAME_LOBBY")
public class GameLobby {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "LOBBY-CODE")
    private String lobbyCode;

    @Column(name = "NAME")
    private String name;

    @Column(name = "STATUS")
    private GameLobbyStatus status;

    @Column(name = "MAX_PLAYERS")
    private int maxPlayers;

    @Column(name = "CREATED_AT")
    private Instant createdAt;

    @Column(name = "PRIVATE")
    private boolean privateStatus;

    @Column(name = "AUTHOR_ID")
    @ManyToOne(fetch = FetchType.LAZY)
    private User author;
}
