package com.latteandletters.dto;

public record RegistrationAvailabilityResult(
        boolean valid,
        boolean available,
        String normalizedValue,
        String message
) {
}
