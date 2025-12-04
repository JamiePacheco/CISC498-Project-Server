package com.aux_arena.controller.rest;

import com.aux_arena.models.enums.Roles;
import com.aux_arena.models.rest.Response;
import com.aux_arena.models.session.GameSession;
import com.aux_arena.models.session.LobbySession;
import com.aux_arena.service.definitions.GameSessionService;
import com.aux_arena.utility.JwtUtil;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/game-session")
public class GameSessionController {

    private JwtUtil jwtUtil;

    private GameSessionService gameSessionService;

    public GameSessionController(JwtUtil jwtUtil, GameSessionService gameSessionService) {
        this.jwtUtil = jwtUtil;
        this.gameSessionService = gameSessionService;
    }

//    @GetMapping()
//    public ResponseEntity<Response<GameSession>> getGameSession(
//            @RequestParam("lobby-id") Long lobbyId,
//            HttpServletRequest request,
//            HttpServletResponse response,
//
//    ) {
//        try {
//
//            // get the current jwt user has
//            String jwt = jwtUtil.extractJwtFromCookie(request);
//            String username = jwtUtil.extractUsername(jwt);
//
//
//
//
//
//
//
//
//            // generate a new extended jwt for guest user that has same principle (same username)
//            if (lobbyUser.getRole() == Roles.GUEST) {
//                // check the user's current jwt
//                if (username == null || !gameLobby.getActiveUsers().containsKey(username)) {
//                    // generate a cookie that lasts for 30 minutes (extend when the game actually begins)
//                    String token = jwtUtil.generateToken(lobbyUser.getGuestIdentifier(), 30 * 60_000);
//                    Cookie cookie = jwtUtil.generateJwtCookie(token);
//                    response.addCookie(cookie);
//                } else {
//                    log.info("User {} [{}] is reconnecting", username, jwt);
//                };
//            }
//
//            return ResponseEntity.ok(
//                    Response.<LobbySession>builder()
//                            .message("Successfully connected to lobby " + lobbyCode)
//                            .responseContent(gameLobby)
//                            .status(HttpStatus.ACCEPTED)
//                            .build()
//            );
//        } catch (Exception ex) {
//            return ResponseEntity
//                    .badRequest()
//                    .body(
//                            Response.<LobbySession>builder()
//                                    .message(ex.getMessage())
//                                    .responseContent(null)
//                                    .status(HttpStatus.CONFLICT)
//                                    .build()
//                    );
//        }
//    }



}
