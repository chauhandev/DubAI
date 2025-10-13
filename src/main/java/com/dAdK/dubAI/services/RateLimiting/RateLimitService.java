package com.dAdK.dubAI.services.RateLimiting;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Deque;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;

@Service
@Slf4j
public class RateLimitService {

    // Map: key = IP + action, value = timestamps of attempts
    private final ConcurrentHashMap<String, Deque<Long>> attempts = new ConcurrentHashMap<>();

    /**
     * Check if the given IP + action is rate limited.
     *
     * @param ipAddress IP address of the client
     * @param action Action name (e.g., "register")
     * @param maxAttempts Max allowed attempts
     * @param windowSeconds Time window in seconds
     * @return true if rate limited
     */
    public boolean isRateLimited(String ipAddress, String action, int maxAttempts, long windowSeconds) {
        String key = ipAddress + ":" + action;
        long now = Instant.now().getEpochSecond();

        attempts.putIfAbsent(key, new ConcurrentLinkedDeque<>());
        Deque<Long> timestamps = attempts.get(key);

        synchronized (timestamps) {
            // Remove timestamps older than the window
            while (!timestamps.isEmpty() && now - timestamps.peekFirst() > windowSeconds) {
                timestamps.pollFirst();
            }

            if (timestamps.size() >= maxAttempts) {
                log.warn("Rate limit exceeded for {} on action {}", ipAddress, action);
                return true;
            }

            // Record this attempt
            timestamps.addLast(now);
            return false;
        }
    }
}
