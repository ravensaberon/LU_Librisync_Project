package com.latteandletters.dto;

import java.util.List;

public class AdminDashboardChartSeries {

    private final String key;
    private final String label;
    private final String title;
    private final String description;
    private final String bucketLabel;
    private final List<AdminDashboardChartPoint> points;
    private final long issuedTotal;
    private final long returnedTotal;
    private final long peakIssued;
    private final long peakReturned;

    public AdminDashboardChartSeries(String key,
                                     String label,
                                     String title,
                                     String description,
                                     String bucketLabel,
                                     List<AdminDashboardChartPoint> points,
                                     long issuedTotal,
                                     long returnedTotal,
                                     long peakIssued,
                                     long peakReturned) {
        this.key = key;
        this.label = label;
        this.title = title;
        this.description = description;
        this.bucketLabel = bucketLabel;
        this.points = points;
        this.issuedTotal = issuedTotal;
        this.returnedTotal = returnedTotal;
        this.peakIssued = peakIssued;
        this.peakReturned = peakReturned;
    }

    public String getKey() {
        return key;
    }

    public String getLabel() {
        return label;
    }

    public String getTitle() {
        return title;
    }

    public String getDescription() {
        return description;
    }

    public String getBucketLabel() {
        return bucketLabel;
    }

    public List<AdminDashboardChartPoint> getPoints() {
        return points;
    }

    public long getIssuedTotal() {
        return issuedTotal;
    }

    public long getReturnedTotal() {
        return returnedTotal;
    }

    public long getPeakIssued() {
        return peakIssued;
    }

    public long getPeakReturned() {
        return peakReturned;
    }
}
