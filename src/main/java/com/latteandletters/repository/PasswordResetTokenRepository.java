package com.latteandletters.repository;

import com.latteandletters.model.PasswordResetToken;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PasswordResetTokenRepository extends JpaRepository<PasswordResetToken, Long> {

    Optional<PasswordResetToken> findFirstByUser_IdAndUsedFalseOrderByCreatedAtDesc(Long userId);

    List<PasswordResetToken> findByUser_IdAndUsedFalseOrderByCreatedAtDesc(Long userId);
}
