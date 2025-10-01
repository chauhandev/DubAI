package com.dAdK.dubAI.services.impl;

import com.dAdK.dubAI.models.Otp;
import com.dAdK.dubAI.models.User;
import com.dAdK.dubAI.repository.OtpRepository;
import com.dAdK.dubAI.services.email.EmailService;
import com.dAdK.dubAI.services.otp.OtpService;
import com.dAdK.dubAI.services.sms.SmsService;
import com.dAdK.dubAI.services.userservice.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.Random;

@Service
public class OtpServiceImpl implements OtpService {

    @Autowired
    private OtpRepository otpRepository;

    @Autowired
    private EmailService emailService;

    @Autowired
    private SmsService smsService;

    @Autowired
    private UserService userService;

    private static final Logger logger = LoggerFactory.getLogger(OtpServiceImpl.class);

    @Override
    public String generateOtp(User user) {
        String otp = String.format("%06d", new Random().nextInt(999999));
        Otp otpEntity = Otp.builder()
                .otp(otp)
                .expiresAt(LocalDateTime.now().plusMinutes(10))
                .user(user)
                .userId(user.getId())
                .build();
        otpRepository.save(otpEntity);
        // Send OTP via email or SMS based on user preference or input
        if (user.getContactNumber() != null && !user.getContactNumber().isEmpty()) {
            smsService.sendOtpSms(user.getContactNumber(), otp);
        } else {
            emailService.sendOtpEmail(user.getEmail(), otp);
        }
        return otp; // Return the generated OTP string
    }

    @Override
    public boolean validateOtp(User user, String otp) {
        logger.info("Validating OTP for userId: {} with OTP: {}", user.getId(), otp);
        Optional<Otp> otpEntityOptional = otpRepository.findByUserId(user.getId());
        if (otpEntityOptional.isPresent()) {
            Otp otpEntity = otpEntityOptional.get();
            boolean isValid = otpEntity.getOtp().equals(otp) && otpEntity.getExpiresAt().isAfter(LocalDateTime.now());
            logger.info("OTP validation result for userId {}: {}", user.getId(), isValid);
            return isValid;
        } else {
            logger.warn("No OTP found for userId: {}", user.getId());
            return false;
        }
    }

    @Override
    public Optional<User> findByUserId(String userId) {
        Optional<Otp> otpData = otpRepository.findByUserId(userId);
        return otpData.map(Otp::getUser);
    }

}
