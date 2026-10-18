package fi.metropolia.zachrisz.online_store_backend.service;

import fi.metropolia.zachrisz.online_store_backend.dto.CategoryResponse;
import fi.metropolia.zachrisz.online_store_backend.repository.CategoryRepository;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
public class CategoryService {

    private final CategoryRepository categoryRepository;

    public CategoryService(CategoryRepository categoryRepository) {
        this.categoryRepository = categoryRepository;
    }

    @Transactional(readOnly = true)
    public List<CategoryResponse> findAll() {
        return categoryRepository.findAll(Sort.by("id")).stream()
                .map(CategoryResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public Optional<CategoryResponse> findById(Integer id) {
        return categoryRepository.findById(id).map(CategoryResponse::from);
    }
}
