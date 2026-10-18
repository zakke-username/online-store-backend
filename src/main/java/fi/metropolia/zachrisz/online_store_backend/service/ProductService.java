package fi.metropolia.zachrisz.online_store_backend.service;

import fi.metropolia.zachrisz.online_store_backend.dto.ProductResponse;
import fi.metropolia.zachrisz.online_store_backend.repository.CategoryRepository;
import fi.metropolia.zachrisz.online_store_backend.repository.ProductRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Service
public class ProductService {

    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;

    public ProductService(ProductRepository productRepository, CategoryRepository categoryRepository) {
        this.productRepository = productRepository;
        this.categoryRepository = categoryRepository;
    }

    @Transactional(readOnly = true)
    public Optional<ProductResponse> findById(Integer id) {
        return productRepository.findById(id).map(ProductResponse::from);
    }

    @Transactional(readOnly = true)
    public Optional<List<ProductResponse>> findByCategoryId(Integer categoryId) {
        return categoryRepository.findById(categoryId)
                .map(category -> productRepository.findByCategoryId(categoryId, Sort.by("id")).stream()
                        .map(ProductResponse::from)
                        .toList());
    }

    @Transactional(readOnly = true)
    public Page<ProductResponse> search(Integer categoryId, String q, BigDecimal minPrice, BigDecimal maxPrice,
                                        boolean inStock, Pageable pageable) {
        String searchText = (q == null || q.isBlank()) ? null : q.strip();

        // Order by ID
        Pageable byId = PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), Sort.by("id"));

        return productRepository.search(categoryId, searchText, minPrice, maxPrice, inStock, byId)
                .map(ProductResponse::from);
    }
}
