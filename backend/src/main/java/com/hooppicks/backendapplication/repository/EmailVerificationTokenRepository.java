package com.hooppicks.backendapplication.repository;

import com.hooppicks.backendapplication.entity.EmailVerificationToken;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface EmailVerificationTokenRepository extends JpaRepository<EmailVerificationToken, String> {
    Optional<EmailVerificationToken> findByToken(String token);

    void deleteByUserId(String userId);
}
