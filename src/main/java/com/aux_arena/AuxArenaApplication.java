package com.aux_arena;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.web.socket.config.annotation.EnableWebSocket;

@EnableWebSocket
@EnableScheduling
@EnableJpaRepositories
@SpringBootApplication
public class AuxArenaApplication {

	public static void main(String[] args) {
		SpringApplication.run(AuxArenaApplication.class, args);
	}

}
