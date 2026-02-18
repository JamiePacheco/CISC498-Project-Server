package com.aux_arena.models.tables;

import com.aux_arena.models.enums.Roles;
import com.fasterxml.jackson.annotation.JsonBackReference;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.time.Instant;
import java.util.Collection;
import java.util.List;

@Data
@Entity
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LobbyUser implements UserDetails {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;

    @JoinColumn(name = "GAME_LOBBY_ID")
    @ManyToOne(fetch = FetchType.LAZY,  cascade = CascadeType.ALL)
    @JsonBackReference
    private GameLobby gameLobby;

    @JoinColumn(name = "USER_ID")
    @ManyToOne(fetch = FetchType.LAZY,  cascade = CascadeType.ALL)
    private User user;

    // UUID generated for guest accounts to make them unique
    @Column(name = "guest_identifier")
    private String guestIdentifier;

    @Column(name = "nickname", length = 100)
    private String nickname;

    @Column(name = "joined_at")
    private Instant joinedAt;

    @Column(name = "last_socket_connection_id")
    private String lastSocketConnectionId;

    @Column(name = "role")
    private Roles role;
    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of();
    }

    @Override
    public String getPassword() {
        return "";
    }

    @Override
    public String getUsername() {
        return this.nickname;
    }


}
