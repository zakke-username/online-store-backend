package fi.metropolia.zachrisz.online_store_backend.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.util.List;

public record OrderRequest(
        @NotNull Integer shippingAddressId,
        @NotEmpty List<@NotNull @Valid Item> items
) {

    public record Item(
            @NotNull Integer productId,
            @NotNull @Positive Integer quantity
    ) {
    }
}
