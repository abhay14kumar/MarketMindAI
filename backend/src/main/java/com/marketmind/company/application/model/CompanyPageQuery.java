package com.marketmind.company.application.model;

public record CompanyPageQuery(
        int page,
        int size,
        String sortBy,
        SortDirection direction) {

    public enum SortDirection {
        ASC,
        DESC
    }
}
