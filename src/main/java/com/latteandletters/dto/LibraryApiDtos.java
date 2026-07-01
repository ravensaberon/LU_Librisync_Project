package com.latteandletters.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

public final class LibraryApiDtos {

    private LibraryApiDtos() {
    }

    public record ModuleInfoResponse(
            String module,
            String system,
            String version,
            String status,
            LocalDateTime serverTime,
            Map<String, String> endpoints
    ) {
    }

    public record BookSummaryResponse(
            Long id,
            String title,
            String isbn,
            String barcode,
            String category,
            String author,
            Integer publicationYear,
            Integer quantity,
            Integer availableQuantity,
            String shelfLocation,
            boolean digital,
            boolean visibleInCatalog
    ) {
    }

    public record BorrowerEligibilityResponse(
            String studentId,
            String name,
            String email,
            String course,
            String yearLevel,
            String accountStatus,
            boolean eligibleToBorrow,
            long activeLoanCount,
            long unpaidFineCount,
            BigDecimal unpaidFineTotal,
            List<String> reasons
    ) {
    }

    public record LibrarySummaryResponse(
            long totalBooks,
            long availableBooks,
            long catalogVisibleBooks,
            long archivedBooks,
            long activeStudents,
            long inactiveStudents,
            long activeLoans,
            long overdueLoans,
            long unpaidFines
    ) {
    }

    public record ExternalModuleResponse(
            String module,
            String url,
            boolean configured,
            boolean reachable,
            Object data,
            String error
    ) {
    }

    public record ApiErrorResponse(
            String error,
            int code,
            String path,
            LocalDateTime timestamp
    ) {
    }
}
