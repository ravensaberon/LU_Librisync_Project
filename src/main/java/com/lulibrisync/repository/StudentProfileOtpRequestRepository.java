package com.lulibrisync.repository;

import com.lulibrisync.model.StudentProfileOtpRequest;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface StudentProfileOtpRequestRepository extends JpaRepository<StudentProfileOtpRequest, Long> {

    Optional<StudentProfileOtpRequest> findFirstByStudent_IdAndUsedFalseOrderByCreatedAtDesc(Long studentId);
}
