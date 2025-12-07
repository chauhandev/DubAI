package com.dAdK.dubAI.services.otp;

import com.dAdK.dubAI.exceptions.OtpException;
import com.dAdK.dubAI.models.Otp;
import com.dAdK.dubAI.repository.OtpRepository;
import com.dAdK.dubAI.services.email.EmailService;
import com.dAdK.dubAI.services.sms.SmsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class OtpServiceImpl implements OtpService {
    private final OtpRepository otpRepository;
    private final EmailService emailService;
    private final SmsService smsService;

    private static final int OTP_LENGTH = 6;
    private static final int OTP_EXPIRY_MINUTES = 10;
    private static final SecureRandom random = new SecureRandom();

    @Override
    @Transactional
    public void generateAndSendOtp(String userId, String target, String type) {
        log.info("Generating OTP for userId: {}, type: {}", userId, type);

        // Delete any existing OTP for this user and type
        otpRepository.findByUserIdAndTypeAndUsedFalse(userId, type)
                .ifPresent(existingOtp -> {
                    log.info("Deleting existing OTP for userId: {}", userId);
                    otpRepository.delete(existingOtp);
                });

        // Generate new OTP
        String otpCode = generateOtpCode();

        // Create OTP entity
        Otp otp = Otp.builder()
                .userId(userId)
                .otp(otpCode)
                .type(type)
                .attemptCount(0)
                .maxAttempts(5)
                .createdAt(LocalDateTime.now())
                .expiresAt(LocalDateTime.now().plusMinutes(OTP_EXPIRY_MINUTES))
                .used(false)
                .build();

        otpRepository.save(otp);
        log.info("OTP saved successfully for userId: {}", userId);

        // Send OTP based on type
        sendOtp(target, otpCode, type);
    }

    @Override
    @Transactional
    public void resendOtp(String userId, String target, String type) {
        log.info("Resending OTP for userId: {}, type: {}", userId, type);
        generateAndSendOtp(userId, target, type);
    }

    @Override
    @Transactional
    public boolean validateOtp(String userId, String otp, String type) {
        log.info("Validating OTP for userId: {}, type: {}", userId, type);

        Optional<Otp> otpOptional = otpRepository.findByUserIdAndTypeAndUsedFalse(userId, type);

        if (otpOptional.isEmpty()) {
            log.warn("No OTP found for userId: {}, type: {}", userId, type);
            return false;
        }

        Otp otpEntity = otpOptional.get();

        // Check if OTP is expired
        if (otpEntity.getExpiresAt().isBefore(LocalDateTime.now())) {
            log.warn("OTP expired for userId: {}", userId);
            otpRepository.delete(otpEntity);
            return false;
        }

        // Check if max attempts exceeded
        if (otpEntity.getAttemptCount() >= otpEntity.getMaxAttempts()) {
            log.warn("Max OTP attempts exceeded for userId: {}", userId);
            otpRepository.delete(otpEntity);
            throw new OtpException("Maximum OTP attempts exceeded. Please request a new OTP.");
        }

        // Increment attempt count
        otpEntity.setAttemptCount(otpEntity.getAttemptCount() + 1);
        otpRepository.save(otpEntity);

        // Validate OTP
        if (otpEntity.getOtp().equals(otp)) {
            log.info("OTP validated successfully for userId: {}", userId);
            otpEntity.setUsed(true);
            otpEntity.setUsedAt(LocalDateTime.now());
            otpRepository.save(otpEntity);
            return true;
        }

        log.warn("Invalid OTP provided for userId: {}. Attempts: {}/{}",
                userId, otpEntity.getAttemptCount(), otpEntity.getMaxAttempts());
        return false;
    }

    @Override
    @Transactional
    public void deleteOtpsByUserId(String userId) {
        log.info("Deleting all OTPs for userId: {}", userId);
        otpRepository.deleteByUserId(userId);
    }

    @Override
    public boolean hasValidOtp(String userId, String type) {
        Optional<Otp> otpOptional = otpRepository.findByUserIdAndTypeAndUsedFalse(userId, type);

        if (otpOptional.isEmpty()) {
            return false;
        }

        Otp otp = otpOptional.get();
        return otp.getExpiresAt().isAfter(LocalDateTime.now());
    }

    @Override
    public Optional<Otp> findByUserId(String userId) {
        return otpRepository.findByUserId(userId);
    }

    private String generateOtpCode() {
        StringBuilder otp = new StringBuilder();
        for (int i = 0; i < OTP_LENGTH; i++) {
            otp.append(random.nextInt(10));
        }
        return otp.toString();
    }

    private void sendOtp(String target, String otpCode, String type) {
        try {
            if ("EMAIL_VERIFICATION".equals(type)) {
                emailService.sendOtpEmail(target, otpCode);
                log.info("OTP email sent successfully to: {}", target);
            } else if ("PHONE_VERIFICATION".equals(type)) {
                smsService.sendOtpSms(target, otpCode);
                log.info("OTP SMS sent successfully to: {}", target);
            }
        } catch (Exception e) {
            log.error("Error sending OTP to: {}", target, e);
            throw new OtpException("Failed to send OTP. Please try again later.");
        }
    }

}
