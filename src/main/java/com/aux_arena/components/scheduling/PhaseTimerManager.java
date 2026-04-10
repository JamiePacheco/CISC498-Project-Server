package com.aux_arena.components.scheduling;

import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;

import java.util.concurrent.*;

@Component
public class PhaseTimerManager {

    // standard JDK service for managing scheduled events
    // choose this over spring task manager as we need to cancel scheduled events if phase change happens before timer
    private final ScheduledExecutorService scheduler;

    private final ConcurrentHashMap<Long, ScheduledFuture<?>> activeTimers = new ConcurrentHashMap<>();

    public PhaseTimerManager(ScheduledExecutorService scheduler) {
        this.scheduler = scheduler;
    }

    public void schedulePhase(Long lobbyId, Runnable task, long delaySeconds) {
        Runnable safeTask = () -> {
            try {
                task.run();
            } catch (Exception e) {
                // TODO implement proper logging/notifications for lobby
            } finally {
                activeTimers.remove(lobbyId);
            }
        };

        // put new task in scheduler and get the previous one (if there was one)
        ScheduledFuture<?> previous = activeTimers.put(
                lobbyId,
                scheduler.schedule(safeTask, delaySeconds, TimeUnit.SECONDS)
        );

        // if we want to transition to new phase then we schedule the next phase
        if (previous != null) previous.cancel(false);
    }

    public boolean cancelTimer(Long lobbyId) {
        ScheduledFuture<?> future = activeTimers.remove(lobbyId);
        if (future != null) {
            return future.cancel(false);
        }
        return false;
    }

    public void cancelAll() {
        activeTimers.forEach((id, future) -> future.cancel(false));
        activeTimers.clear();
    }

    @Bean
    public ScheduledExecutorService gameScheduler() {
        int coreCount = Runtime.getRuntime().availableProcessors();

        return new ScheduledThreadPoolExecutor(coreCount * 2) {
            {
                setRejectedExecutionHandler(new ThreadPoolExecutor.DiscardOldestPolicy());

                setRemoveOnCancelPolicy(true);
            }
        };
    }

}
