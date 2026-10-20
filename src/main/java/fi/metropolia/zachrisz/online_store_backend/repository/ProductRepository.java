package fi.metropolia.zachrisz.online_store_backend.repository;

import fi.metropolia.zachrisz.online_store_backend.dto.PopularProductResponse;
import fi.metropolia.zachrisz.online_store_backend.entity.Product;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface ProductRepository extends JpaRepository<Product, Integer> {

    List<Product> findByCategoryId(Integer categoryId, Sort sort);

    @EntityGraph(attributePaths = "category")
    @Query("""
            SELECT p FROM Product p
            WHERE (:categoryId IS NULL OR p.category.id = :categoryId)
              AND (:q IS NULL OR LOWER(p.name) LIKE LOWER(CONCAT('%', :q, '%'))
                              OR LOWER(p.description) LIKE LOWER(CONCAT('%', :q, '%')))
              AND (:minPrice IS NULL OR p.price >= :minPrice)
              AND (:maxPrice IS NULL OR p.price <= :maxPrice)
              AND (:inStock = false OR p.stockQuantity > 0)
            """)
    Page<Product> search(Integer categoryId, String q, BigDecimal minPrice, BigDecimal maxPrice,
                         boolean inStock, Pageable pageable);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    List<Product> findForUpdateByIdIn(Collection<Integer> ids);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<Product> findForUpdateById(Integer id);

    @Query(nativeQuery = true, value = """
            SELECT pc.id, pc.name, pc.description, pc.price,
                   pc.stock_quantity AS stockQuantity, pc.category_id AS categoryId,
                   pc.category_name AS categoryName,
                   CAST(SUM(i.quantity) AS SIGNED) AS unitsSold
            FROM ProductCatalog pc
            JOIN OrderItems i ON i.product_id = pc.id
            JOIN Orders o ON o.id = i.order_id
            WHERE o.order_date >= NOW() - INTERVAL :days DAY
              AND o.status <> 'CANCELLED'
            GROUP BY pc.id, pc.name, pc.description, pc.price, pc.stock_quantity, pc.category_id, pc.category_name
            ORDER BY unitsSold DESC, pc.id
            LIMIT :limit
            """)
    List<PopularProductResponse> findPopular(int days, int limit);
}
