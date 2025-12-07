package com.dAdK.dubAI.repository;

import com.dAdK.dubAI.models.User;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends MongoRepository<User, String> {
    Optional<User> findByUsername(String username);

    Optional<User> findByEmail(String email);

    Optional<User> findByContactNumber(String contactNumber);

    Optional<User> findByEmailAndDeletedFalse(String email);

    Optional<User> findByContactNumberAndDeletedFalse(String contactNumber);

    Optional<User> findByUsernameAndDeletedFalse(String username);

    boolean existsByUsername(String username);

    boolean existsByEmail(String email);

    boolean existsByContactNumber(String contactNumber);

    List<User> findByStatusAndCreatedAtBefore(String status, LocalDateTime createdAt);

    @Query("{ '$or': [ { 'email': ?0 }, { 'contactNumber': ?0 } ] }")
    Optional<User> findByEmailOrContactNumber(String identifier);

    @Query("{ '$or': [ { 'username': ?0 }, { 'email': ?1 }, { 'contactNumber': ?2 } ] }")
    Optional<User> findByUsernameOrEmailOrContactNumber(String userName, String email, String phoneNumber);
}
