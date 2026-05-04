package com.lulibrisync.service;

import com.lulibrisync.model.Fine;
import com.lulibrisync.model.FineStatus;
import com.lulibrisync.model.IssueRecord;
import com.lulibrisync.repository.FineRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class FineService {

    private final FineRepository fineRepository;
    private final AuditLogService auditLogService;

    public FineService(FineRepository fineRepository,
                       AuditLogService auditLogService) {
        this.fineRepository = fineRepository;
        this.auditLogService = auditLogService;
    }

    public List<Fine> getAllFines() {
        return fineRepository.findAllByOrderByCalculatedAtDesc();
    }

    public List<Fine> getRecentOutstandingFines() {
        return fineRepository.findTop12ByStatusOrderByCalculatedAtDesc(FineStatus.UNPAID);
    }

    public List<Fine> getStudentFines(Long studentId) {
        return fineRepository.findByStudent_IdOrderByCalculatedAtDesc(studentId);
    }

    public Fine getFineById(Long fineId) {
        if (fineId == null) {
            throw new IllegalArgumentException("Fine is required.");
        }
        return fineRepository.findById(fineId)
                .orElseThrow(() -> new IllegalArgumentException("Fine record not found."));
    }

    public long countOutstandingFines() {
        return fineRepository.countByStatus(FineStatus.UNPAID);
    }

    public BigDecimal getOutstandingFineTotal() {
        return normalizeAmount(fineRepository.sumAmountByStatus(FineStatus.UNPAID));
    }

    public long countByStatus(FineStatus status) {
        if (status == null) {
            return 0L;
        }
        return fineRepository.countByStatus(status);
    }

    public BigDecimal getTotalAmountByStatus(FineStatus status) {
        if (status == null) {
            return BigDecimal.ZERO;
        }
        return normalizeAmount(fineRepository.sumAmountByStatus(status));
    }

    public boolean hasOutstandingFine(Long studentId) {
        return countOutstandingFinesByStudent(studentId) > 0L;
    }

    public long countOutstandingFinesByStudent(Long studentId) {
        if (studentId == null) {
            return 0L;
        }
        return fineRepository.countByStudent_IdAndStatus(studentId, FineStatus.UNPAID);
    }

    public BigDecimal getOutstandingFineTotalByStudent(Long studentId) {
        if (studentId == null) {
            return BigDecimal.ZERO;
        }
        return normalizeAmount(fineRepository.sumAmountByStudentIdAndStatus(studentId, FineStatus.UNPAID));
    }

    @Transactional
    public void syncFineForIssue(IssueRecord issueRecord) {
        if (issueRecord == null || issueRecord.getId() == null || issueRecord.getStudent() == null) {
            return;
        }

        BigDecimal amount = normalizeAmount(issueRecord.getFineAmount());
        Fine existingFine = fineRepository.findByIssueRecord_Id(issueRecord.getId()).orElse(null);

        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            if (existingFine != null && FineStatus.UNPAID.equals(existingFine.getStatus())) {
                fineRepository.delete(existingFine);
                auditLogService.logSystem(
                        "FINE_REMOVED",
                        "FINE",
                        existingFine.getId().toString(),
                        "Outstanding fine cleared from issue record " + issueRecord.getId(),
                        "Fine was removed because the issue no longer has an unpaid balance."
                );
            }
            return;
        }

        if (existingFine == null) {
            Fine fine = new Fine();
            fine.setIssueRecord(issueRecord);
            fine.setStudent(issueRecord.getStudent());
            fine.setAmount(amount);
            fine.setStatus(FineStatus.UNPAID);
            fine.setCalculatedAt(LocalDateTime.now());
            Fine savedFine = fineRepository.save(fine);
            auditLogService.logSystem(
                    "FINE_CREATED",
                    "FINE",
                    savedFine.getId().toString(),
                    "Fine created for issue record " + issueRecord.getId(),
                    "Amount: " + amount + " | Student: " + issueRecord.getStudent().getStudentId()
            );
            return;
        }

        if (!FineStatus.UNPAID.equals(existingFine.getStatus())) {
            return;
        }

        if (existingFine.getAmount() == null || existingFine.getAmount().compareTo(amount) != 0) {
            existingFine.setAmount(amount);
            existingFine.setCalculatedAt(LocalDateTime.now());
            fineRepository.save(existingFine);
            auditLogService.logSystem(
                    "FINE_UPDATED",
                    "FINE",
                    existingFine.getId().toString(),
                    "Fine amount updated for issue record " + issueRecord.getId(),
                    "New amount: " + amount
            );
        }
    }

    @Transactional
    public void removeFineForIssue(Long issueRecordId) {
        fineRepository.findByIssueRecord_Id(issueRecordId)
                .filter(fine -> FineStatus.UNPAID.equals(fine.getStatus()))
                .ifPresent(fineRepository::delete);
    }

    @Transactional
    public Fine markFinePaid(Long fineId, String actorEmail) {
        Fine fine = getFineById(fineId);
        if (!FineStatus.UNPAID.equals(fine.getStatus())) {
            throw new IllegalArgumentException("Only unpaid fines can be marked as paid.");
        }
        fine.setStatus(FineStatus.PAID);
        fine.setPaidAt(LocalDateTime.now());
        Fine savedFine = fineRepository.save(fine);
        auditLogService.log(
                actorEmail,
                "FINE_PAID",
                "FINE",
                savedFine.getId().toString(),
                "Fine marked as paid",
                "Issue record: " + savedFine.getIssueRecord().getId() + " | Amount: " + savedFine.getAmount()
        );
        return savedFine;
    }

    @Transactional
    public Fine waiveFine(Long fineId, String actorEmail) {
        Fine fine = getFineById(fineId);
        if (!FineStatus.UNPAID.equals(fine.getStatus())) {
            throw new IllegalArgumentException("Only unpaid fines can be waived.");
        }
        fine.setStatus(FineStatus.WAIVED);
        fine.setPaidAt(LocalDateTime.now());
        Fine savedFine = fineRepository.save(fine);
        auditLogService.log(
                actorEmail,
                "FINE_WAIVED",
                "FINE",
                savedFine.getId().toString(),
                "Fine waived",
                "Issue record: " + savedFine.getIssueRecord().getId() + " | Amount: " + savedFine.getAmount()
        );
        return savedFine;
    }

    private BigDecimal normalizeAmount(BigDecimal amount) {
        return amount == null ? BigDecimal.ZERO : amount;
    }
}
