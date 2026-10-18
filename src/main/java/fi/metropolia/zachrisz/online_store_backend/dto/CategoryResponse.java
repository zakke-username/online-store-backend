package fi.metropolia.zachrisz.online_store_backend.dto;

import fi.metropolia.zachrisz.online_store_backend.entity.ProductCategory;

public record CategoryResponse(
        Integer id,
        String name,
        String description
) {

    public static CategoryResponse from(ProductCategory category) {
        return new CategoryResponse(
                category.getId(),
                category.getName(),
                category.getDescription()
        );
    }
}
