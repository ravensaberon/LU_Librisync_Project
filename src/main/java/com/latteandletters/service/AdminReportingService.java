package com.latteandletters.service;

import com.latteandletters.model.AuditLog;
import com.latteandletters.model.Fine;
import com.latteandletters.model.IssueRecord;
import com.latteandletters.model.IssueStatus;
import com.latteandletters.model.Reservation;
import org.apache.poi.ss.usermodel.BorderStyle;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.HorizontalAlignment;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.VerticalAlignment;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFFont;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

@Service
public class AdminReportingService {

    private static final String DATE_TIME_FORMAT = "yyyy-mm-dd hh:mm:ss AM/PM";
    private static final int MAX_COLUMN_WIDTH = 45 * 256;
    private static final int MIN_COLUMN_WIDTH = 12 * 256;

    private final IssueService issueService;
    private final FineService fineService;
    private final ReservationService reservationService;
    private final AuditLogService auditLogService;

    public AdminReportingService(IssueService issueService,
                                 FineService fineService,
                                 ReservationService reservationService,
                                 AuditLogService auditLogService) {
        this.issueService = issueService;
        this.fineService = fineService;
        this.reservationService = reservationService;
        this.auditLogService = auditLogService;
    }

    public List<IssueRecord> getCirculationRecords(LocalDate dateFrom, LocalDate dateTo) {
        return issueService.getAllIssues().stream()
                .filter(issue -> withinRange(issue.getIssueDate(), dateFrom, dateTo))
                .toList();
    }

    public List<IssueRecord> getOverdueRecords(LocalDate dateFrom, LocalDate dateTo) {
        return issueService.getAllIssues().stream()
                .filter(issue -> IssueStatus.OVERDUE.equals(issue.getStatus()))
                .filter(issue -> withinRange(issue.getDueDate(), dateFrom, dateTo))
                .toList();
    }

    public List<Fine> getFineRecords(LocalDate dateFrom, LocalDate dateTo) {
        return fineService.getAllFines().stream()
                .filter(fine -> withinRange(fine.getCalculatedAt(), dateFrom, dateTo))
                .toList();
    }

    public List<Reservation> getReservationRecords(LocalDate dateFrom, LocalDate dateTo) {
        return reservationService.getAllReservations().stream()
                .filter(reservation -> withinRange(reservation.getReservedAt(), dateFrom, dateTo))
                .toList();
    }

    public List<AuditLog> getAuditRecords(LocalDate dateFrom, LocalDate dateTo) {
        return auditLogService.getAllLogs().stream()
                .filter(log -> withinRange(log.getCreatedAt(), dateFrom, dateTo))
                .toList();
    }

    public byte[] buildWorkbook(String reportType, LocalDate dateFrom, LocalDate dateTo) {
        String normalizedType = reportType == null ? "" : reportType.trim().toLowerCase(Locale.ENGLISH);

        try (XSSFWorkbook workbook = new XSSFWorkbook();
             ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            WorkbookStyles styles = createStyles(workbook);

            switch (normalizedType) {
                case "circulation" -> populateCirculationSheet(workbook, styles, getCirculationRecords(dateFrom, dateTo));
                case "overdue" -> populateOverdueSheet(workbook, styles, getOverdueRecords(dateFrom, dateTo));
                case "fines" -> populateFinesSheet(workbook, styles, getFineRecords(dateFrom, dateTo));
                case "reservations" -> populateReservationsSheet(workbook, styles, getReservationRecords(dateFrom, dateTo));
                case "audit" -> populateAuditSheet(workbook, styles, getAuditRecords(dateFrom, dateTo));
                default -> throw new IllegalArgumentException("Unsupported report type.");
            }

            workbook.write(outputStream);
            return outputStream.toByteArray();
        } catch (IOException exception) {
            throw new IllegalStateException("Unable to generate Excel report.", exception);
        }
    }

    private void populateCirculationSheet(XSSFWorkbook workbook, WorkbookStyles styles, List<IssueRecord> issues) {
        Sheet sheet = workbook.createSheet("Circulation Report");
        String[] headers = {"Issue Code", "Book", "Student ID", "Student Name", "Issued By", "Issue Date", "Due Date", "Return Date", "Status", "Fine Amount", "Remarks"};
        createHeaderRow(sheet, headers, styles);

        int rowIndex = 1;
        for (IssueRecord issue : issues) {
            Row row = sheet.createRow(rowIndex++);
            writeTextCell(row, 0, issue.getQrIssueCode(), styles.body);
            writeTextCell(row, 1, issue.getBook().getTitle(), styles.body);
            writeTextCell(row, 2, issue.getStudent().getStudentId(), styles.body);
            writeTextCell(row, 3, issue.getStudent().getUser().getName(), styles.body);
            writeTextCell(row, 4, issue.getIssuedBy().getName(), styles.body);
            writeDateTimeCell(row, 5, issue.getIssueDate(), styles.dateTime);
            writeDateTimeCell(row, 6, issue.getDueDate(), styles.dateTime);
            writeDateTimeCell(row, 7, issue.getReturnDate(), styles.dateTime);
            writeTextCell(row, 8, issue.getStatus() == null ? "" : issue.getStatus().name(), styles.center);
            writeMoneyCell(row, 9, issue.getFineAmount(), styles.money);
            writeTextCell(row, 10, issue.getRemarks(), styles.wrapped);
            row.setHeightInPoints(24);
        }

        finalizeSheet(sheet, headers.length, 10);
    }

    private void populateOverdueSheet(XSSFWorkbook workbook, WorkbookStyles styles, List<IssueRecord> overdueIssues) {
        Sheet sheet = workbook.createSheet("Overdue Report");
        String[] headers = {"Issue Code", "Book", "Student ID", "Student Name", "Due Date", "Days Overdue", "Fine Amount", "Issued By"};
        createHeaderRow(sheet, headers, styles);

        LocalDateTime now = LocalDateTime.now();
        int rowIndex = 1;
        for (IssueRecord issue : overdueIssues) {
            Row row = sheet.createRow(rowIndex++);
            long daysOverdue = issue.getDueDate() == null ? 0L : Math.max(1L, Duration.between(issue.getDueDate(), now).toDays());

            writeTextCell(row, 0, issue.getQrIssueCode(), styles.body);
            writeTextCell(row, 1, issue.getBook().getTitle(), styles.body);
            writeTextCell(row, 2, issue.getStudent().getStudentId(), styles.body);
            writeTextCell(row, 3, issue.getStudent().getUser().getName(), styles.body);
            writeDateTimeCell(row, 4, issue.getDueDate(), styles.dateTime);
            writeNumberCell(row, 5, daysOverdue, styles.centerNumber);
            writeMoneyCell(row, 6, issue.getFineAmount(), styles.money);
            writeTextCell(row, 7, issue.getIssuedBy().getName(), styles.body);
            row.setHeightInPoints(24);
        }

        finalizeSheet(sheet, headers.length, -1);
    }

    private void populateFinesSheet(XSSFWorkbook workbook, WorkbookStyles styles, List<Fine> fines) {
        Sheet sheet = workbook.createSheet("Fine Report");
        String[] headers = {"Fine ID", "Student ID", "Student Name", "Issue Code", "Book", "Amount", "Status", "Calculated At", "Paid Or Waived At"};
        createHeaderRow(sheet, headers, styles);

        int rowIndex = 1;
        for (Fine fine : fines) {
            Row row = sheet.createRow(rowIndex++);
            writeNumberCell(row, 0, fine.getId(), styles.centerNumber);
            writeTextCell(row, 1, fine.getStudent().getStudentId(), styles.body);
            writeTextCell(row, 2, fine.getStudent().getUser().getName(), styles.body);
            writeTextCell(row, 3, fine.getIssueRecord().getQrIssueCode(), styles.body);
            writeTextCell(row, 4, fine.getIssueRecord().getBook().getTitle(), styles.body);
            writeMoneyCell(row, 5, fine.getAmount(), styles.money);
            writeTextCell(row, 6, fine.getStatus() == null ? "" : fine.getStatus().name(), styles.center);
            writeDateTimeCell(row, 7, fine.getCalculatedAt(), styles.dateTime);
            writeDateTimeCell(row, 8, fine.getPaidAt(), styles.dateTime);
            row.setHeightInPoints(24);
        }

        finalizeSheet(sheet, headers.length, -1);
    }

    private void populateReservationsSheet(XSSFWorkbook workbook, WorkbookStyles styles, List<Reservation> reservations) {
        Sheet sheet = workbook.createSheet("Reservation Report");
        String[] headers = {"Reservation ID", "Book", "Student ID", "Student Name", "Queue Position", "Status", "Reserved At", "Claim Until"};
        createHeaderRow(sheet, headers, styles);

        List<Reservation> sortedReservations = reservations.stream()
                .sorted(Comparator.comparing(Reservation::getReservedAt, Comparator.nullsLast(Comparator.naturalOrder())).reversed())
                .toList();

        int rowIndex = 1;
        for (Reservation reservation : sortedReservations) {
            Row row = sheet.createRow(rowIndex++);
            writeNumberCell(row, 0, reservation.getId(), styles.centerNumber);
            writeTextCell(row, 1, reservation.getBook().getTitle(), styles.body);
            writeTextCell(row, 2, reservation.getStudent().getStudentId(), styles.body);
            writeTextCell(row, 3, reservation.getStudent().getUser().getName(), styles.body);
            writeNumberCell(row, 4, reservation.getQueuePosition(), styles.centerNumber);
            writeTextCell(row, 5, reservation.getStatus() == null ? "" : reservation.getStatus().name(), styles.center);
            writeDateTimeCell(row, 6, reservation.getReservedAt(), styles.dateTime);
            writeDateTimeCell(row, 7, reservation.getExpiresAt(), styles.dateTime);
            row.setHeightInPoints(24);
        }

        finalizeSheet(sheet, headers.length, -1);
    }

    private void populateAuditSheet(XSSFWorkbook workbook, WorkbookStyles styles, List<AuditLog> logs) {
        Sheet sheet = workbook.createSheet("Audit Report");
        String[] headers = {"Log ID", "Timestamp", "Actor Name", "Actor Email", "Action", "Entity Type", "Entity ID", "Summary", "Details"};
        createHeaderRow(sheet, headers, styles);

        int rowIndex = 1;
        for (AuditLog log : logs) {
            Row row = sheet.createRow(rowIndex++);
            writeNumberCell(row, 0, log.getId(), styles.centerNumber);
            writeDateTimeCell(row, 1, log.getCreatedAt(), styles.dateTime);
            writeTextCell(row, 2, log.getActorName(), styles.body);
            writeTextCell(row, 3, log.getActorEmail(), styles.body);
            writeTextCell(row, 4, log.getAction(), styles.body);
            writeTextCell(row, 5, log.getEntityType(), styles.body);
            writeTextCell(row, 6, log.getEntityId(), styles.body);
            writeTextCell(row, 7, log.getSummary(), styles.wrapped);
            writeTextCell(row, 8, log.getDetails(), styles.wrapped);
            row.setHeightInPoints(36);
        }

        finalizeSheet(sheet, headers.length, 8);
    }

    private void createHeaderRow(Sheet sheet, String[] headers, WorkbookStyles styles) {
        Row headerRow = sheet.createRow(0);
        headerRow.setHeightInPoints(26);

        for (int columnIndex = 0; columnIndex < headers.length; columnIndex++) {
            Cell cell = headerRow.createCell(columnIndex);
            cell.setCellValue(headers[columnIndex]);
            cell.setCellStyle(styles.header);
        }
    }

    private void finalizeSheet(Sheet sheet, int columnCount, int wrappedColumnIndex) {
        sheet.createFreezePane(0, 1);
        sheet.setAutoFilter(new CellRangeAddress(0, Math.max(sheet.getLastRowNum(), 0), 0, columnCount - 1));

        for (int columnIndex = 0; columnIndex < columnCount; columnIndex++) {
            sheet.autoSizeColumn(columnIndex);
            int preferredWidth = Math.max(sheet.getColumnWidth(columnIndex), MIN_COLUMN_WIDTH);
            if (columnIndex == wrappedColumnIndex) {
                preferredWidth = Math.max(preferredWidth, 24 * 256);
            }
            sheet.setColumnWidth(columnIndex, Math.min(preferredWidth + 512, MAX_COLUMN_WIDTH));
        }
    }

    private void writeTextCell(Row row, int columnIndex, String value, CellStyle style) {
        Cell cell = row.createCell(columnIndex);
        cell.setCellValue(value == null ? "" : value);
        cell.setCellStyle(style);
    }

    private void writeDateTimeCell(Row row, int columnIndex, LocalDateTime value, CellStyle style) {
        Cell cell = row.createCell(columnIndex);
        if (value != null) {
            cell.setCellValue(java.sql.Timestamp.valueOf(value));
        } else {
            cell.setCellValue("");
        }
        cell.setCellStyle(style);
    }

    private void writeNumberCell(Row row, int columnIndex, Number value, CellStyle style) {
        Cell cell = row.createCell(columnIndex);
        if (value != null) {
            cell.setCellValue(value.doubleValue());
        } else {
            cell.setCellValue("");
        }
        cell.setCellStyle(style);
    }

    private void writeMoneyCell(Row row, int columnIndex, BigDecimal value, CellStyle style) {
        Cell cell = row.createCell(columnIndex);
        if (value != null) {
            cell.setCellValue(value.doubleValue());
        } else {
            cell.setCellValue(0D);
        }
        cell.setCellStyle(style);
    }

    private WorkbookStyles createStyles(XSSFWorkbook workbook) {
        XSSFFont headerFont = workbook.createFont();
        headerFont.setBold(true);
        headerFont.setColor(IndexedColors.WHITE.getIndex());

        XSSFFont bodyFont = workbook.createFont();
        bodyFont.setFontHeightInPoints((short) 10);

        CellStyle header = workbook.createCellStyle();
        header.setFont(headerFont);
        header.setFillForegroundColor(IndexedColors.DARK_BLUE.getIndex());
        header.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        header.setAlignment(HorizontalAlignment.CENTER);
        header.setVerticalAlignment(VerticalAlignment.CENTER);
        applyBorders(header);

        CellStyle body = workbook.createCellStyle();
        body.setFont(bodyFont);
        body.setVerticalAlignment(VerticalAlignment.CENTER);
        applyBorders(body);

        CellStyle wrapped = workbook.createCellStyle();
        wrapped.cloneStyleFrom(body);
        wrapped.setWrapText(true);
        wrapped.setVerticalAlignment(VerticalAlignment.TOP);

        CellStyle center = workbook.createCellStyle();
        center.cloneStyleFrom(body);
        center.setAlignment(HorizontalAlignment.CENTER);

        CellStyle centerNumber = workbook.createCellStyle();
        centerNumber.cloneStyleFrom(center);
        centerNumber.setDataFormat(workbook.createDataFormat().getFormat("0"));

        CellStyle dateTime = workbook.createCellStyle();
        dateTime.cloneStyleFrom(body);
        dateTime.setDataFormat(workbook.createDataFormat().getFormat(DATE_TIME_FORMAT));

        CellStyle money = workbook.createCellStyle();
        money.cloneStyleFrom(body);
        money.setAlignment(HorizontalAlignment.RIGHT);
        money.setDataFormat(workbook.createDataFormat().getFormat("#,##0.00"));

        return new WorkbookStyles(header, body, wrapped, center, centerNumber, dateTime, money);
    }

    private void applyBorders(CellStyle style) {
        style.setBorderTop(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);
        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN);
    }

    private boolean withinRange(LocalDateTime value, LocalDate dateFrom, LocalDate dateTo) {
        if (value == null) {
            return false;
        }
        LocalDateTime rangeStart = dateFrom == null ? null : dateFrom.atStartOfDay();
        LocalDateTime rangeEnd = dateTo == null ? null : dateTo.atTime(LocalTime.MAX);
        if (rangeStart != null && value.isBefore(rangeStart)) {
            return false;
        }
        if (rangeEnd != null && value.isAfter(rangeEnd)) {
            return false;
        }
        return true;
    }

    private record WorkbookStyles(CellStyle header,
                                  CellStyle body,
                                  CellStyle wrapped,
                                  CellStyle center,
                                  CellStyle centerNumber,
                                  CellStyle dateTime,
                                  CellStyle money) {
    }
}
