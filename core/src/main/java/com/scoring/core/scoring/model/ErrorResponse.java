package com.scoring.core.scoring.model;

public record ErrorResponse(
        String message,
        String details
) {
}
