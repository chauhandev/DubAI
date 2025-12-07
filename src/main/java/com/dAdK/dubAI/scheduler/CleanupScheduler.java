package com.dAdK.dubAI.scheduler;

import com.dAdK.dubAI.models.User;
import com.dAdK.dubAI.services.otp.OtpService;
import com.dAdK.dubAI.services.userservice.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class CleanupScheduler {

    private final UserService userService;
    private final OtpService otpService;

    @Scheduled(cron = "0 */15 * * * *") // Every 15 minutes
    public void cleanupUnverifiedUsers() {
        log.info("Starting cleanup of unverified users");

        LocalDateTime cutoff = LocalDateTime.now().minusMinutes(30);
        List<User> unverifiedUsers = userService.findUnverifiedUsers(cutoff);

        for (User user : unverifiedUsers) {
            try {
                log.info("Cleaning up unverified user - ID: {}, Username: {}, Email: {}, Phone: {}, Created: {}",
                        user.getId(), user.getUsername(), user.getEmail(),
                        user.getContactNumber(), user.getCreatedAt());

                otpService.deleteOtpsByUserId(user.getId());
                userService.deleteUser(user.getId());
            } catch (Exception e) {
                log.error("Error cleaning up user: {}", user.getId(), e);
            }
        }

        log.info("Cleanup completed. Removed {} unverified users", unverifiedUsers.size());
    }
}