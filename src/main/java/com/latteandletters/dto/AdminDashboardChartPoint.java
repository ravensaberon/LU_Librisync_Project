package com.latteandletters.dto;

public class AdminDashboardChartPoint {

    private final String label;
    private final long issuedCount;
    private final long returnedCount;
    private final int issuedHeight;
    private final int returnedHeight;

    public AdminDashboardChartPoint(String label,
                                    long issuedCount,
                                    long returnedCount,
                                    int issuedHeight,
                                    int returnedHeight) {
        this.label = label;
        this.issuedCount = issuedCount;
        this.returnedCount = returnedCount;
        this.issuedHeight = issuedHeight;
        this.returnedHeight = returnedHeight;
    }

    public String getLabel() {
        return label;
    }

    public long getIssuedCount() {
        return issuedCount;
    }

    public long getReturnedCount() {
        return returnedCount;
    }

    public int getIssuedHeight() {
        return issuedHeight;
    }

    public int getReturnedHeight() {
        return returnedHeight;
    }
}
