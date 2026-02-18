package com.aux_arena.controller.rest;


import com.aux_arena.models.enums.Roles;
import com.aux_arena.models.rest.Response;
import com.aux_arena.models.session.LobbySession;
import com.aux_arena.models.tables.GameLobby;
import com.aux_arena.models.tables.LobbyUser;
import com.aux_arena.service.definitions.LobbySessionService;
import com.aux_arena.utility.JwtUtil;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/lobby-session")
public class LobbySessionController {

    private JwtUtil jwtUtil;

    private LobbySessionService lobbySessionService;

    private static final Logger log = LoggerFactory.getLogger(LobbySessionController.class);

    public LobbySessionController(JwtUtil jwtUtil, LobbySessionService lobbySessionService) {
        this.jwtUtil = jwtUtil;
        this.lobbySessionService = lobbySessionService;
    }

    @PostMapping("/connect")
    public ResponseEntity<Response<LobbySession>> connectToGameLobby(
            @RequestParam("lobby-code") String lobbyCode,
            @RequestParam("password") String password,
            @RequestParam("temp-id") String tempId, // temp id for the user session
            @RequestBody LobbyUser lobbyUser,
            HttpServletResponse response,
            HttpServletRequest request
    ) {
        try {
            String jwt = jwtUtil.extractJwtFromCookie(request);
            String username = jwt == null ? null : jwtUtil.extractUsername(jwt);

            log.info("Connecting user with username [{}] and principle [{}]", username);

            LobbySession gameLobby = lobbySessionService.connectToGameLobby(
                    lobbyCode,
                    password,
                    tempId,
                    username,
                    lobbyUser
            );

            // generate a temp jwt token for guest users
            if (lobbyUser.getRole() == Roles.GUEST) {
                // check if the user already has a JWT within their response header
                // TODO put into its own service method for better testing practices
                if (username == null || !gameLobby.getActiveUsers().containsKey(username)) {
                    // generate a cookie that lasts for 30 minutes (extend when the game actually begins)
                    String token = jwtUtil.generateToken(lobbyUser.getGuestIdentifier(), 30 * 60_000);
                    Cookie cookie = jwtUtil.generateJwtCookie(token);
                    log.info("Cookie created for user [{}] with guest identifier [{}]", username, lobbyUser.getGuestIdentifier());
                    response.addCookie(cookie);
                } else {
                    log.info("User {} [{}] is reconnecting", username, jwt);
                };
            }

            return ResponseEntity.ok(
                    Response.<LobbySession>builder()
                            .message("Successfully connected to lobby " + lobbyCode)
                            .responseContent(gameLobby)
                            .status(HttpStatus.ACCEPTED)
                            .build()
            );
        } catch (Exception ex) {
            return ResponseEntity
                    .badRequest()
                    .body(
                            Response.<LobbySession>builder()
                                    .message(ex.getMessage())
                                    .responseContent(null)
                                    .status(HttpStatus.CONFLICT)
                                    .build()
                    );
        }
    }

    // TODO finish implementing start lobby endpoint
    //    transition the state of the gamelobby
    // this needs to be sent to the frontend through the sockets (game lobby update)
    // navigate to the game lobby page
    //    save all the users from memory to table objects (for persistence)
    // when the users transition to the game page they should call a new rest endpoint to get the game session (renew jwt)
    //    populate a new game session into game session manager
    //    create an initial round to start the game with


    // this sends the most recent version of the game lobby with all updated fields (...should all users call this endpoint?)
    @PostMapping("/start-lobby")
    public ResponseEntity<Response<List<LobbyUser>>> startGameLobby(
            @RequestParam("lobby-id") Long lobbyId,
            HttpServletResponse response,
            HttpServletRequest request
    ) {
        try {
            List<LobbyUser> savedUsers = lobbySessionService.startGameLobby(lobbyId);

            return ResponseEntity.ok(
                    Response.<List<LobbyUser>>builder()
                            .message("Successfully Started Lobby With " + savedUsers.size() + " Users.")
                            .responseContent(savedUsers)
                            .status(HttpStatus.ACCEPTED)
                            .build()
            );
        } catch (Exception ex) {
            return ResponseEntity
                    .badRequest()
                    .body(
                            Response.<List<LobbyUser>>builder()
                                    .message(ex.getMessage())
                                    .responseContent(null)
                                    .status(HttpStatus.CONFLICT)
                                    .build()
                    );
        }
    }


}
