package com.aux_arena.controller.rest;

import com.aux_arena.models.rest.Response;
import com.aux_arena.models.tables.GameLobby;
import com.aux_arena.service.implementations.GameLobbyServiceImpl;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/game-lobby")
@AllArgsConstructor
public class GameLobbyController {

    private GameLobbyServiceImpl gameLobbyService;

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
    public ResponseEntity<Response<GameLobby>> getGameLobby(@RequestParam("lobby-id") String lobbyCode) {
        try {
            GameLobby gameLobby = gameLobbyService.getGameLobby(lobbyCode);
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
