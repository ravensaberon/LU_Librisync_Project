package com.lulibrisync.util;

import java.util.List;

public final class PaginationUtils {

    private static final int DEFAULT_PAGE_SIZE = 10;
    private static final int PAGE_WINDOW = 2;

    private PaginationUtils() {
    }

    public static <T> PaginationSlice<T> paginate(List<T> items, Integer requestedPage, int requestedPageSize) {
        List<T> sourceItems = items == null ? List.of() : items;
        int pageSize = requestedPageSize < 1 ? DEFAULT_PAGE_SIZE : requestedPageSize;
        int totalItems = sourceItems.size();
        int totalPages = Math.max(1, (int) Math.ceil(totalItems / (double) pageSize));
        int page = requestedPage == null ? 1 : Math.max(1, Math.min(requestedPage, totalPages));
        int fromIndex = Math.min((page - 1) * pageSize, totalItems);
        int toIndex = Math.min(fromIndex + pageSize, totalItems);
        int startPage = Math.max(1, page - PAGE_WINDOW);
        int endPage = Math.min(totalPages, page + PAGE_WINDOW);

        return new PaginationSlice<>(
                sourceItems.subList(fromIndex, toIndex),
                page,
                pageSize,
                totalItems,
                totalPages,
                startPage,
                endPage
        );
    }
}
