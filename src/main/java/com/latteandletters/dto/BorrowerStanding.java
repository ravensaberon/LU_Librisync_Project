package com.latteandletters.dto;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class BorrowerStanding {

    private final boolean eligibleToBorrow;
    private final String statusLabel;
    private final int activeLoansCount;
    private final int overdueCount;
    private final int unpaidFineCount;
    private final BigDecimal outstandingFineAmount;
    private final int maxActiveLoans;
    private final int remainingLoanSlots;
    private final List<String> blockers;

    public BorrowerStanding(boolean eligibleToBorrow,
                            String statusLabel,
                            int activeLoansCount,
                            int overdueCount,
                            int unpaidFineCount,
                            BigDecimal outstandingFineAmount,
                            int maxActiveLoans,
                            int remainingLoanSlots,
                            List<String> blockers) {
        this.eligibleToBorrow = eligibleToBorrow;
        this.statusLabel = statusLabel;
        this.activeLoansCount = activeLoansCount;
        this.overdueCount = overdueCount;
        this.unpaidFineCount = unpaidFineCount;
        this.outstandingFineAmount = outstandingFineAmount;
        this.maxActiveLoans = maxActiveLoans;
        this.remainingLoanSlots = remainingLoanSlots;
        this.blockers = blockers == null ? List.of() : Collections.unmodifiableList(new ArrayList<>(blockers));
    }

    public boolean isEligibleToBorrow() {
        return eligibleToBorrow;
    }

    public String getStatusLabel() {
        return statusLabel;
    }

    public int getActiveLoansCount() {
        return activeLoansCount;
    }

    public int getOverdueCount() {
        return overdueCount;
    }

    public int getUnpaidFineCount() {
        return unpaidFineCount;
    }

    public BigDecimal getOutstandingFineAmount() {
        return outstandingFineAmount;
    }

    public int getMaxActiveLoans() {
        return maxActiveLoans;
    }

    public int getRemainingLoanSlots() {
        return remainingLoanSlots;
    }

    public List<String> getBlockers() {
        return blockers;
    }

    public boolean isBlocked() {
        return !eligibleToBorrow;
    }
}
