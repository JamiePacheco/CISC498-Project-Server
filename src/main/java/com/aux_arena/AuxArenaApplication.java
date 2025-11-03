package com.aux_arena;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.web.socket.config.annotation.EnableWebSocket;

@EnableWebSocket
@SpringBootApplication
public class AuxArenaApplication {

	public static void main(String[] args) {
		SpringApplication.run(AuxArenaApplication.class, args);
	}

}
