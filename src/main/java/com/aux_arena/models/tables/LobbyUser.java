package com.aux_arena.models.tables;

import com.aux_arena.models.enums.Roles;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@Entity
@NoArgsConstructor
@AllArgsConstructor
public class LobbyUser {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "game_lobby")
    @ManyToOne(fetch = FetchType.LAZY)
    private GameLobby gameLobby;

    @Column(name = "user")
    @ManyToOne(fetch = FetchType.LAZY)
    private User user;

    @Column(name = "nickname", length = 100)
    private String nickname;

    @Column(name = "joined_at")
    private Instant joinedAt;

    @Column(name = "last_socket_connection_id")
    private String lastSocketConnectionId;

    @Column(name = "role")
    private Roles role;
}
