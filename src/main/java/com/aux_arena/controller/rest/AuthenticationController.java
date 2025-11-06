package com.aux_arena.controller.rest;

import com.aux_arena.models.dtos.UserDTO;
import com.aux_arena.models.rest.Response;
import com.aux_arena.models.tables.User;
import com.aux_arena.service.definitions.UserService;
import com.aux_arena.service.implementations.AuthenticationServiceImpl;
import com.aux_arena.service.implementations.UserServiceImpl;
import com.aux_arena.utility.JwtUtil;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.lang.annotation.Repeatable;

@RestController()
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthenticationController {

    @NonNull
    private final AuthenticationManager authenticationManager;
    @NonNull
    private final JwtUtil jwtUtil;
    @NonNull
    private final AuthenticationServiceImpl authenticationService;

    @NonNull
    private final UserServiceImpl userService;

    private Logger logger = LoggerFactory.getLogger(AuthenticationController.class);

    @PostMapping()
    public ResponseEntity<Response<UserDTO>> createNewUser(@RequestBody User user) {

        logger.info("Accessed create user endpoint");

        try {
            User newUser = authenticationService.createNewUser(user);
            UserDTO newUserModel = new UserDTO(newUser);


            return ResponseEntity.ok(
                    Response.<UserDTO>builder()
                            .message("Successfully created new user")
                            .responseContent(newUserModel)
                            .status(HttpStatus.ACCEPTED)
                            .build()
            );
        } catch (Exception ex) {
            return ResponseEntity
                    .badRequest()
                    .body(
                            Response.<UserDTO>builder()
                                    .message(ex.getMessage())
                                    .responseContent(null)
                                    .status(HttpStatus.CONFLICT)
                                    .build()
                    );
        }
    }

    @GetMapping()
    public ResponseEntity<Response<String>> login(@RequestParam("username") String username, @RequestParam("password") String password, HttpServletResponse response) {
        try {
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(username, password)
            );

            UserDetails userDetails = userService.loadUserByUsername(username);
            String token = jwtUtil.generateToken(userDetails.getUsername());

            // added jwt as an HTTP only cookie to the response
            Cookie cookie = new Cookie("jwt", token);
            cookie.setHttpOnly(true);
            cookie.setSecure(true);
            cookie.setPath("/");
            cookie.setMaxAge(24 * 60 * 60);
            response.addCookie(cookie);

            return ResponseEntity.ok(
                    Response.<String>builder()
                            .responseContent(null)
                            .message("Successfully Authenticated User")
                            .status(HttpStatus.ACCEPTED)
                            .build()
            );
        } catch (Exception ex) {
            return ResponseEntity.badRequest()
                    .body(
                            Response.<String>builder()
                                    .message("Error Authenticating User")
                                    .responseContent(ex.getMessage())
                                    .status(HttpStatus.BAD_REQUEST)
                                    .build()
                    );
        }
    }

    @GetMapping()
    public ResponseEntity<Response<String>> createGuestUser(@RequestParam("username") String username, HttpServletResponse response) {



    }







}
