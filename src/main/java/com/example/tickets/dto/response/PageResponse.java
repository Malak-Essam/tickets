package com.example.tickets.dto.response;

import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Paginated response wrapper")
public record PageResponse<T>(
    @Schema(description = "List of items in this page")
    List<T> content,

    @Schema(description = "Current page number (0-indexed)", example = "0")
    int page,

    @Schema(description = "Page size", example = "20")
    int size,

    @Schema(description = "Total number of matching elements", example = "150")
    long totalElements,

    @Schema(description = "Total number of pages", example = "8")
    int totalPages) {
}
