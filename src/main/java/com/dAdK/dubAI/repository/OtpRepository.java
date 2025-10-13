package com.dAdK.dubAI.repository;

import com.dAdK.dubAI.models.Otp;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;

@Repository
public interface OtpRepository extends MongoRepository<Otp, String> {
    Optional<Otp> findByUserIdAndTypeAndUsedFalse(String userId, String type);
    Optional<Otp>findByUserId(String userId);

    void deleteByUserId(String userId);

    void deleteByExpiresAtBefore(LocalDateTime expiresAt);

    boolean existsByUserIdAndType(String userId, String type);
}
