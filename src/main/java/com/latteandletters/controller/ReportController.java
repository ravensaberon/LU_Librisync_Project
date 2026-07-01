package com.latteandletters.controller;

import com.latteandletters.model.Fine;
import com.latteandletters.model.FineStatus;
import com.latteandletters.model.IssueRecord;
import com.latteandletters.model.IssueStatus;
import com.latteandletters.model.Reservation;
import com.latteandletters.model.ReservationStatus;
import com.latteandletters.service.AdminReportingService;
import com.latteandletters.service.IssueService;
import com.latteandletters.util.PaginationUtils;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/admin/reports")
public class ReportController {

    private static final DateTimeFormatter FILE_DATE_FORMATTER =
            DateTimeFormatter.ofPattern("yyyyMMdd", Locale.ENGLISH);
    private static final int BORROWING_PAGE_SIZE = 6;
    private static final int AUDIT_AND_FINE_PAGE_SIZE = 6;

    private final AdminReportingService adminReportingService;
    private final IssueService issueService;

    public ReportController(AdminReportingService adminReportingService,
                            IssueService issueService) {
        this.adminReportingService = adminReportingService;
        this.issueService = issueService;
    }

    @GetMapping
    public String reports(@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateFrom,
                          @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateTo,
                          @RequestParam(defaultValue = "exports") String reportTab,
                          @RequestParam(defaultValue = "1") Integer overduePage,
                          @RequestParam(defaultValue = "1") Integer reservationPage,
                          @RequestParam(defaultValue = "1") Integer finePage,
                          @RequestParam(defaultValue = "1") Integer auditPage,
                          Model model) {
        List<IssueRecord> circulationRecords = adminReportingService.getCirculationRecords(dateFrom, dateTo);
        List<IssueRecord> overdueRecords = adminReportingService.getOverdueRecords(dateFrom, dateTo).stream()
                .sorted(Comparator.comparing(IssueRecord::getDueDate, Comparator.nullsLast(Comparator.naturalOrder())))
                .toList();
        List<Fine> fineRecords = adminReportingService.getFineRecords(dateFrom, dateTo).stream()
                .sorted(Comparator.comparing(Fine::getCalculatedAt, Comparator.nullsLast(Comparator.naturalOrder())).reversed())
                .toList();
        List<Reservation> reservationRecords = adminReportingService.getReservationRecords(dateFrom, dateTo).stream()
                .sorted(Comparator.comparing(Reservation::getReservedAt, Comparator.nullsLast(Comparator.naturalOrder())).reversed())
                .toList();
        List<com.latteandletters.model.AuditLog> auditRecords = adminReportingService.getAuditRecords(dateFrom, dateTo).stream()
                .sorted(Comparator.comparing(com.latteandletters.model.AuditLog::getCreatedAt, Comparator.nullsLast(Comparator.naturalOrder())).reversed())
                .toList();

        var overdueRecordsPage = PaginationUtils.paginate(overdueRecords, overduePage, BORROWING_PAGE_SIZE);
        var reservationRecordsPage = PaginationUtils.paginate(reservationRecords, reservationPage, BORROWING_PAGE_SIZE);
        var fineRecordsPage = PaginationUtils.paginate(fineRecords, finePage, AUDIT_AND_FINE_PAGE_SIZE);
        var auditRecordsPage = PaginationUtils.paginate(auditRecords, auditPage, AUDIT_AND_FINE_PAGE_SIZE);

        BigDecimal unpaidFineTotal = totalFineAmount(fineRecords, FineStatus.UNPAID);
        BigDecimal paidFineTotal = totalFineAmount(fineRecords, FineStatus.PAID);
        BigDecimal waivedFineTotal = totalFineAmount(fineRecords, FineStatus.WAIVED);
        Map<String, Long> topBorrowers = circulationRecords.stream()
                .collect(Collectors.groupingBy(
                        issue -> issue.getStudent().getStudentId() + " - " + issue.getStudent().getUser().getName(),
                        Collectors.counting()))
                .entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                .limit(6)
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        Map.Entry::getValue,
                        (left, right) -> left,
                        java.util.LinkedHashMap::new
                ));

        Map<String, Long> topTitles = circulationRecords.stream()
                .collect(Collectors.groupingBy(issue -> issue.getBook().getTitle(), Collectors.counting()))
                .entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                .limit(6)
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        Map.Entry::getValue,
                        (left, right) -> left,
                        java.util.LinkedHashMap::new
                ));

        model.addAttribute("dateFrom", dateFrom);
        model.addAttribute("dateTo", dateTo);
        model.addAttribute("reportTab", reportTab);
        model.addAttribute("circulationRecords", circulationRecords.stream().limit(12).toList());
        model.addAttribute("overdueRecords", overdueRecordsPage.getItems());
        model.addAttribute("overdueRecordsPage", overdueRecordsPage);
        model.addAttribute("reservationRecords", reservationRecordsPage.getItems());
        model.addAttribute("reservationRecordsPage", reservationRecordsPage);
        model.addAttribute("fineRecords", fineRecordsPage.getItems());
        model.addAttribute("fineRecordsPage", fineRecordsPage);
        model.addAttribute("auditRecords", auditRecordsPage.getItems());
        model.addAttribute("auditRecordsPage", auditRecordsPage);
        model.addAttribute("circulationCount", circulationRecords.size());
        model.addAttribute("returnedCount", circulationRecords.stream().filter(issue -> IssueStatus.RETURNED.equals(issue.getStatus())).count());
        model.addAttribute("activeIssueCount", circulationRecords.stream().filter(issue -> IssueStatus.ISSUED.equals(issue.getStatus()) || IssueStatus.OVERDUE.equals(issue.getStatus())).count());
        model.addAttribute("overdueCount", overdueRecords.size());
        model.addAttribute("reservationCount", reservationRecords.size());
        model.addAttribute("pendingReservationCount", reservationRecords.stream().filter(reservation -> ReservationStatus.PENDING.equals(reservation.getStatus())).count());
        model.addAttribute("readyReservationCount", reservationRecords.stream().filter(reservation -> ReservationStatus.READY.equals(reservation.getStatus())).count());
        model.addAttribute("claimedReservationCount", reservationRecords.stream().filter(reservation -> ReservationStatus.CLAIMED.equals(reservation.getStatus())).count());
        model.addAttribute("cancelledReservationCount", reservationRecords.stream().filter(reservation -> ReservationStatus.CANCELLED.equals(reservation.getStatus())).count());
        model.addAttribute("unpaidFineTotal", unpaidFineTotal);
        model.addAttribute("paidFineTotal", paidFineTotal);
        model.addAttribute("waivedFineTotal", waivedFineTotal);
        model.addAttribute("fineRecordCount", fineRecords.size());
        model.addAttribute("auditRecordCount", auditRecords.size());
        model.addAttribute("topBorrowers", topBorrowers);
        model.addAttribute("topTitles", topTitles);
        model.addAttribute("circulationChartSeries", issueService.getCirculationChartSeries());
        return "admin/reports";
    }

    @GetMapping("/export")
    public ResponseEntity<byte[]> export(@RequestParam String type,
                                         @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateFrom,
                                         @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateTo) {
        byte[] workbookBytes = adminReportingService.buildWorkbook(type, dateFrom, dateTo);
        String normalizedType = type.trim().toLowerCase(Locale.ENGLISH);
        String filename = "latteandletters-" + normalizedType + "-report-"
                + FILE_DATE_FORMATTER.format(dateFrom == null ? LocalDate.now() : dateFrom)
                + "-to-"
                + FILE_DATE_FORMATTER.format(dateTo == null ? LocalDate.now() : dateTo)
                + ".xlsx";

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment().filename(filename).build().toString())
                .body(workbookBytes);
    }

    private BigDecimal totalFineAmount(List<Fine> fineRecords, FineStatus status) {
        return fineRecords.stream()
                .filter(fine -> status.equals(fine.getStatus()))
                .map(Fine::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}
