package com.marketmind.company.application.dto;

import java.util.List;

import com.marketmind.company.application.model.CompanyPageQuery.SortDirection;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "CompanyPage", description = "Paginated company master results")
public record CompanyPageDTO(
        List<CompanyDTO> content,
        int page,
        int size,
        long totalElements,
        int totalPages,
        String sortBy,
        SortDirection direction) {

    public CompanyPageDTO {
        content = List.copyOf(content);
    }
}
