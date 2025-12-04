package com.aux_arena.service.implementations;

import com.aux_arena.models.enums.Roles;
import com.aux_arena.models.session.UserSession;
import com.aux_arena.models.tables.GameLobby;
import com.aux_arena.models.tables.LobbyUser;
import com.aux_arena.models.tables.User;
import com.aux_arena.repository.LobbyUserRepository;
import com.aux_arena.service.definitions.GameLobbyService;
import com.aux_arena.service.definitions.LobbyUserService;
import com.aux_arena.service.definitions.UserService;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;

@Service
@AllArgsConstructor
public class LobbyUserServiceImpl implements LobbyUserService {

    private LobbyUserRepository lobbyUserRepository;

    private UserService userService;

    @Override
    public LobbyUser getLobbyUser(String username) {
        return lobbyUserRepository.findLobbyUserByGuestIdentifier(username);
    }

    @Override
    public LobbyUser saveLobbyUser(LobbyUser lobbyUser) {
        return null;
    }

    @Override
    public List<LobbyUser> saveLobbyUsers(List<UserSession> userSessions, GameLobby gameLobby) {

        if (gameLobby == null) {
            throw new RuntimeException("Game Lobby Associated With Users Not Found");
        }

        List<LobbyUser> lobbyUsersToSave = userSessions.stream()
                .filter(u -> u.getActive()) // only get the active users
                .map(userSession -> {
            LobbyUser newLobbyUser = LobbyUser.builder()
                    .gameLobby(gameLobby)
                    .joinedAt(Instant.now())
                    .guestIdentifier(userSession.getTempId())
                    .lastSocketConnectionId(userSession.getSessionId())
                    .nickname(userSession.getDisplayName())
                    .build();

            // check if user exists within db
            if (userSession.getUserId() != null) {
                User user = userService.findUser(userSession.getUserId());
                if (user != null) {
                    newLobbyUser.setUser(user);
                    newLobbyUser.setRole(Roles.REGULAR);
                } else {
                    // for some reason they had an associated user id, but it is not real (maybe an edge case not properly represented...)
                    newLobbyUser.setUser(null);
                    userSession.setUserId(null); // set it to null
                    newLobbyUser.setRole(Roles.GUEST);
                }
            } else {
                newLobbyUser.setUser(null);
                newLobbyUser.setRole(Roles.GUEST);
            }

            return  newLobbyUser;
        }).toList();

        // persist the new user to the db
        List<LobbyUser> savedPlayers = lobbyUserRepository.saveAll(lobbyUsersToSave);
        return savedPlayers;
    }

}
