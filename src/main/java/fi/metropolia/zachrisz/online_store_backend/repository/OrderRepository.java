package fi.metropolia.zachrisz.online_store_backend.repository;

import fi.metropolia.zachrisz.online_store_backend.dto.OrderSummaryResponse;
import fi.metropolia.zachrisz.online_store_backend.entity.Order;
import fi.metropolia.zachrisz.online_store_backend.entity.OrderStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;

public interface OrderRepository extends JpaRepository<Order, Integer> {

    // Totals are calculated by the database, so a page of orders is a single query
    @Query(value = """
            SELECT new fi.metropolia.zachrisz.online_store_backend.dto.OrderSummaryResponse(
                o.id, o.orderDate, o.status, SUM(i.unitPrice * i.quantity))
            FROM Order o JOIN o.items i
            WHERE o.customer.id = :customerId
              AND (:status IS NULL OR o.status = :status)
            GROUP BY o.id, o.orderDate, o.status
            """,
            countQuery = """
            SELECT COUNT(o) FROM Order o
            WHERE o.customer.id = :customerId
              AND (:status IS NULL OR o.status = :status)
            """)
    Page<OrderSummaryResponse> findSummariesByCustomerId(Integer customerId, OrderStatus status, Pageable pageable);

    @Query("""
            SELECT o FROM Order o
            LEFT JOIN FETCH o.shippingAddress
            LEFT JOIN FETCH o.items i
            LEFT JOIN FETCH i.product
            WHERE o.id = :id
            """)
    Optional<Order> findWithItemsById(Integer id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<Order> findForUpdateById(Integer id);
}
