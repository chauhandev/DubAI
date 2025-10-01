package com.dAdK.dubAI.repository;

import com.dAdK.dubAI.models.Otp;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Optional;

public interface OtpRepository extends MongoRepository<Otp, String> {
    Optional<Otp> findByUserId(String userId);
}
