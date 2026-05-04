package com.lulibrisync.repository;

import com.lulibrisync.model.AuditLog;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {

    List<AuditLog> findTop30ByOrderByCreatedAtDesc();

    List<AuditLog> findAllByOrderByCreatedAtDesc();
}
