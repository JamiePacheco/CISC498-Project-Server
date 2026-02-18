package com.aux_arena.controller.rest;

import com.aux_arena.models.enums.Roles;
import com.aux_arena.models.rest.Response;
import com.aux_arena.models.tables.GameLobby;
import com.aux_arena.models.tables.LobbyUser;
import com.aux_arena.service.implementations.GameLobbyServiceImpl;
import com.aux_arena.utility.JwtUtil;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import lombok.AllArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/game-lobby")
@AllArgsConstructor
public class GameLobbyController {

    private GameLobbyServiceImpl gameLobbyService;

    private JwtUtil jwtUtil;

    private final Logger log = LoggerFactory.getLogger(GameLobbyController.class);

    @PostMapping()
    public ResponseEntity<Response<GameLobby>> createGameLobby(@RequestBody GameLobby gameLobby) {
        try {
            GameLobby newGameLobby = gameLobbyService.createGameLobby(gameLobby);
            return ResponseEntity.ok(
                    Response.<GameLobby>builder()
                            .message("Successfully created game lobby")
                            .responseContent(newGameLobby)
                            .status(HttpStatus.ACCEPTED)
                            .build()
            );
        } catch (Exception ex) {
            return ResponseEntity
                    .badRequest()
                    .body(
                            Response.<GameLobby>builder()
                                    .message(ex.getMessage())
                                    .responseContent(null)
                                    .status(HttpStatus.CONFLICT)
                                    .build()
                    );
        }
    }

    @GetMapping()
    public ResponseEntity<Response<GameLobby>> getGameLobby(
            @RequestParam("lobby-id") String lobbyCode,
            @RequestParam("password") String password
    ) {
        try {
            GameLobby gameLobby = gameLobbyService.getGameLobby(lobbyCode, password);
            return ResponseEntity.ok(
                    Response.<GameLobby>builder()
                            .message("Successfully retrieved game lobby with code: " + lobbyCode)
                            .responseContent(gameLobby)
                            .status(HttpStatus.ACCEPTED)
                            .build()
            );
        } catch (Exception ex) {
            return ResponseEntity
                    .badRequest()
                    .body(
                            Response.<GameLobby>builder()
                                    .message(ex.getMessage())
                                    .responseContent(null)
                                    .status(HttpStatus.CONFLICT)
                                    .build()
                    );
        }
    }
}
