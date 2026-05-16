package com.aux_arena.configuration;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

@Configuration
public class SchedulerConfig {
    @Bean
    public ScheduledExecutorService gameScheduler() {
        // Scale thread pool to expected concurrent lobbies
        return Executors.newScheduledThreadPool(
                Runtime.getRuntime().availableProcessors() * 2
        );
    }
}
