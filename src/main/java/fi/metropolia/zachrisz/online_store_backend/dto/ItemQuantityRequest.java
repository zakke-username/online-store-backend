package fi.metropolia.zachrisz.online_store_backend.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record ItemQuantityRequest(
        @NotNull @Positive Integer quantity
) {
}
