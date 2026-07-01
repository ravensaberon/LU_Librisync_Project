package com.latteandletters.repository;

import com.latteandletters.model.StudentProfileOtpRequest;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface StudentProfileOtpRequestRepository extends JpaRepository<StudentProfileOtpRequest, Long> {

    Optional<StudentProfileOtpRequest> findFirstByStudent_IdAndUsedFalseOrderByCreatedAtDesc(Long studentId);
}
