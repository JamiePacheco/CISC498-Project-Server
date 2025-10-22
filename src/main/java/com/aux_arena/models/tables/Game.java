package com.aux_arena.models.tables;

import com.aux_arena.models.enums.GameMode;
import com.aux_arena.models.enums.GameStatus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class Game {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "GAME_LOBBY_ID")
    private Long  gameLobbyId;

    @Column(name = "GAME_MODE")
    private GameMode gameMode;

    @Column(name = "GAME_STATUS")
    private GameStatus gameStatus;

    @ManyToOne
    @JoinColumn(name = "winner_id")
    private User winner;

}
