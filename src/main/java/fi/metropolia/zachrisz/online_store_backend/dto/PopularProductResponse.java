package fi.metropolia.zachrisz.online_store_backend.dto;

import java.math.BigDecimal;

// The fields of ProductResponse plus unitsSold. Created directly by ProductRepository's native query,
// which maps the query's column aliases to these fields, so there's no from() method.
public record PopularProductResponse(
        Integer id,
        String name,
        String description,
        BigDecimal price,
        int stockQuantity,
        Integer categoryId,
        String categoryName,
        long unitsSold
) {
}
