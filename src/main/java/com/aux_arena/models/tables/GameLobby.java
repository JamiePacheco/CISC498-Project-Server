package com.aux_arena.models.tables;

import com.aux_arena.models.enums.GameLobbyStatus;
import jakarta.persistence.*;
import lombok.*;
import java.time.Instant;

@Data
@Entity
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "GAME_LOBBY")
public class GameLobby {

    public static int MAX_PLAYERS = 20;
    public static int MAX_CAPACITY = 100;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "LOBBY_CODE")
    private String lobbyCode;

    @Column(name = "NAME")
    private String name;

    @Column(name = "STATUS")
    private GameLobbyStatus status;

    @Column(name = "MAX_PLAYERS")
    private int maxPlayers = MAX_PLAYERS;

    @Column(name = "MAX_CAPACITY")
    private int maxCapacity = MAX_CAPACITY;

    @Column(name = "CREATED_AT")
    private Instant createdAt;

    @Column(name = "PRIVATE")
    private boolean privateStatus;

    @Column(name = "PASSWORD")
    private String password;

    @Column(name = "AUTHOR")
    @ManyToOne(fetch = FetchType.LAZY)
    private LobbyUser author;
}
