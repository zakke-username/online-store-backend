package fi.metropolia.zachrisz.online_store_backend.controller;

import fi.metropolia.zachrisz.online_store_backend.dto.PopularProductResponse;
import fi.metropolia.zachrisz.online_store_backend.dto.ProductResponse;
import fi.metropolia.zachrisz.online_store_backend.service.ProductService;
import jakarta.validation.constraints.Positive;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/api/products")
public class ProductController {

    private final ProductService productService;

    public ProductController(ProductService productService) {
        this.productService = productService;
    }

    @GetMapping
    public Page<ProductResponse> getProducts(
            @RequestParam(required = false) Integer categoryId,
            @RequestParam(required = false) String q,
            @RequestParam(required = false) BigDecimal minPrice,
            @RequestParam(required = false) BigDecimal maxPrice,
            @RequestParam(defaultValue = "false") boolean inStock,
            Pageable pageable) {
        return productService.search(categoryId, q, minPrice, maxPrice, inStock, pageable);
    }

    // Bestsellers. "popular" is matched before {productId}, because a fixed path wins over a variable.
    @GetMapping("/popular")
    public List<PopularProductResponse> getPopularProducts(@RequestParam(defaultValue = "30") @Positive int days,
                                                           @RequestParam(defaultValue = "10") @Positive int limit) {
        return productService.findPopular(days, limit);
    }

    @GetMapping("/{productId}")
    public ResponseEntity<ProductResponse> getProductById(@PathVariable Integer productId) {
        return productService.findById(productId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
}
