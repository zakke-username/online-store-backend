package fi.metropolia.zachrisz.online_store_backend.dto;

import fi.metropolia.zachrisz.online_store_backend.entity.Product;
import fi.metropolia.zachrisz.online_store_backend.entity.ProductCategory;

import java.math.BigDecimal;

public record ProductResponse(
        Integer id,
        String name,
        String description,
        BigDecimal price,
        int stockQuantity,
        Integer categoryId,
        String categoryName
) {

    public static ProductResponse from(Product product) {
        ProductCategory category = product.getCategory();
        return new ProductResponse(
                product.getId(),
                product.getName(),
                product.getDescription(),
                product.getPrice(),
                product.getStockQuantity(),
                category != null ? category.getId() : null,
                category != null ? category.getName() : null
        );
    }
}
