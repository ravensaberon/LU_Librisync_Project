package com.lulibrisync.service;

import com.lulibrisync.model.AuditLog;
import com.lulibrisync.model.User;
import com.lulibrisync.repository.AuditLogRepository;
import com.lulibrisync.repository.UserRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class AuditLogService {

    private final AuditLogRepository auditLogRepository;
    private final UserRepository userRepository;

    public AuditLogService(AuditLogRepository auditLogRepository,
                           UserRepository userRepository) {
        this.auditLogRepository = auditLogRepository;
        this.userRepository = userRepository;
    }

    public AuditLog log(String actorEmail,
                        String action,
                        String entityType,
                        String entityId,
                        String summary,
                        String details) {
        AuditLog log = new AuditLog();
        log.setActorEmail(blankToNull(actorEmail));
        log.setAction(action);
        log.setEntityType(entityType);
        log.setEntityId(blankToNull(entityId));
        log.setSummary(summary);
        log.setDetails(blankToNull(details));

        if (actorEmail != null && !actorEmail.isBlank()) {
            userRepository.findByEmailIgnoreCase(actorEmail.trim())
                    .map(User::getName)
                    .ifPresent(log::setActorName);
        }

        return auditLogRepository.save(log);
    }

    public AuditLog logSystem(String action,
                              String entityType,
                              String entityId,
                              String summary,
                              String details) {
        AuditLog log = new AuditLog();
        log.setActorEmail("system@lulibrisync.local");
        log.setActorName("LU Librisync System");
        log.setAction(action);
        log.setEntityType(entityType);
        log.setEntityId(blankToNull(entityId));
        log.setSummary(summary);
        log.setDetails(blankToNull(details));
        return auditLogRepository.save(log);
    }

    public List<AuditLog> getRecentLogs() {
        return auditLogRepository.findTop30ByOrderByCreatedAtDesc();
    }

    public List<AuditLog> getAllLogs() {
        return auditLogRepository.findAllByOrderByCreatedAtDesc();
    }

    public long countLogs() {
        return auditLogRepository.count();
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
