package com.dAdK.dubAI.repository;

import com.dAdK.dubAI.models.User;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends MongoRepository<User, String> {
    List<User> findAllProjectedBy();

    Optional<User> findByUsername(String username);
    Optional<User> findByEmail(String email);
    Optional<User> findByContactNumber(String contactNumber);
    Optional<User> findByUsernameOrEmailOrContactNumber(String username, String email, String contactNumber);

    boolean existsByUsername(String username);

    boolean existsByEmail(String email);

    boolean existsByContactNumber(String contactNumber);
}
