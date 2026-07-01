package com.latteandletters.util;

import java.util.List;

public class PaginationSlice<T> {

    private final List<T> items;
    private final int page;
    private final int pageSize;
    private final int totalItems;
    private final int totalPages;
    private final int startPage;
    private final int endPage;

    public PaginationSlice(List<T> items,
                           int page,
                           int pageSize,
                           int totalItems,
                           int totalPages,
                           int startPage,
                           int endPage) {
        this.items = items;
        this.page = page;
        this.pageSize = pageSize;
        this.totalItems = totalItems;
        this.totalPages = totalPages;
        this.startPage = startPage;
        this.endPage = endPage;
    }

    public List<T> getItems() {
        return items;
    }

    public int getPage() {
        return page;
    }

    public int getPageSize() {
        return pageSize;
    }

    public int getTotalItems() {
        return totalItems;
    }

    public int getTotalPages() {
        return totalPages;
    }

    public int getStartPage() {
        return startPage;
    }

    public int getEndPage() {
        return endPage;
    }

    public boolean isHasPrevious() {
        return page > 1;
    }

    public boolean isHasNext() {
        return page < totalPages;
    }

    public int getPreviousPage() {
        return Math.max(1, page - 1);
    }

    public int getNextPage() {
        return Math.min(totalPages, page + 1);
    }
}
