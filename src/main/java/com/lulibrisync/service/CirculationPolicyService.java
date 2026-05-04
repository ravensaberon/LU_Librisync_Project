package com.lulibrisync.service;

import com.lulibrisync.dto.BorrowerStanding;
import com.lulibrisync.model.IssueStatus;
import com.lulibrisync.model.Student;
import com.lulibrisync.model.UserStatus;
import com.lulibrisync.repository.IssueRecordRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Service
public class CirculationPolicyService {

    private final IssueRecordRepository issueRecordRepository;
    private final FineService fineService;
    private final int maxActiveLoans;
    private final int maxLoanDays;
    private final boolean blockOnOverdue;
    private final boolean blockOnUnpaidFines;

    public CirculationPolicyService(IssueRecordRepository issueRecordRepository,
                                    FineService fineService,
                                    @Value("${lulibrisync.circulation.max-active-loans:3}") int maxActiveLoans,
                                    @Value("${lulibrisync.circulation.max-loan-days:14}") int maxLoanDays,
                                    @Value("${lulibrisync.circulation.block-on-overdue:true}") boolean blockOnOverdue,
                                    @Value("${lulibrisync.circulation.block-on-unpaid-fines:true}") boolean blockOnUnpaidFines) {
        this.issueRecordRepository = issueRecordRepository;
        this.fineService = fineService;
        this.maxActiveLoans = Math.max(1, maxActiveLoans);
        this.maxLoanDays = Math.max(1, maxLoanDays);
        this.blockOnOverdue = blockOnOverdue;
        this.blockOnUnpaidFines = blockOnUnpaidFines;
    }

    public BorrowerStanding evaluateStanding(Student student) {
        if (student == null || student.getId() == null) {
            return new BorrowerStanding(false, "Unavailable", 0, 0, 0, BigDecimal.ZERO, maxActiveLoans, 0, List.of("Student account could not be evaluated."));
        }

        int activeLoansCount = (int) issueRecordRepository.countByStudent_IdAndStatusIn(student.getId(), List.of(IssueStatus.ISSUED, IssueStatus.OVERDUE));
        int overdueCount = (int) issueRecordRepository.countByStudent_IdAndStatusIn(student.getId(), List.of(IssueStatus.OVERDUE));
        int unpaidFineCount = (int) fineService.countOutstandingFinesByStudent(student.getId());
        BigDecimal outstandingFineAmount = fineService.getOutstandingFineTotalByStudent(student.getId());
        int remainingLoanSlots = Math.max(0, maxActiveLoans - activeLoansCount);

        List<String> blockers = new ArrayList<>();
        if (!UserStatus.ACTIVE.equals(student.getUser().getStatus())) {
            blockers.add("Account is inactive.");
        }
        if (blockOnOverdue && overdueCount > 0) {
            blockers.add("Library hold is active because the student has overdue borrowed items.");
        }
        if (blockOnUnpaidFines && outstandingFineAmount.compareTo(BigDecimal.ZERO) > 0) {
            blockers.add("Library hold is active because the student has unpaid fine obligations.");
        }
        if (activeLoansCount >= maxActiveLoans) {
            blockers.add("Maximum active loan limit has been reached.");
        }

        boolean eligibleToBorrow = blockers.isEmpty();
        String statusLabel = eligibleToBorrow ? "Borrowing cleared" : "Borrowing blocked";

        return new BorrowerStanding(
                eligibleToBorrow,
                statusLabel,
                activeLoansCount,
                overdueCount,
                unpaidFineCount,
                outstandingFineAmount,
                maxActiveLoans,
                remainingLoanSlots,
                blockers
        );
    }

    public void validateBorrowingEligibility(Student student) {
        BorrowerStanding standing = evaluateStanding(student);
        if (!standing.isEligibleToBorrow()) {
            throw new IllegalArgumentException("Student is currently blocked from borrowing: " + String.join(" ", standing.getBlockers()));
        }
    }

    public int getMaxLoanDays() {
        return maxLoanDays;
    }

    public int getMaxActiveLoans() {
        return maxActiveLoans;
    }
}
