package com.lulibrisync.repository;

import com.lulibrisync.model.RegistrationOtpToken;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface RegistrationOtpTokenRepository extends JpaRepository<RegistrationOtpToken, Long> {

    Optional<RegistrationOtpToken> findFirstByUser_IdAndUsedFalseOrderByCreatedAtDesc(Long userId);

    List<RegistrationOtpToken> findByUser_IdAndUsedFalseOrderByCreatedAtDesc(Long userId);
}
