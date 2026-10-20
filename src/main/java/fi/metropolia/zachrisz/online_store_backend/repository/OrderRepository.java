package fi.metropolia.zachrisz.online_store_backend.repository;

import fi.metropolia.zachrisz.online_store_backend.entity.Order;
import fi.metropolia.zachrisz.online_store_backend.entity.OrderStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.query.Procedure;

import java.util.Optional;

public interface OrderRepository extends JpaRepository<Order, Integer> {

    @EntityGraph(attributePaths = "shippingAddress")
    @Query("""
            SELECT o FROM Order o
            WHERE o.customer.id = :customerId
              AND (:status IS NULL OR o.status = :status)
            """)
    Page<Order> findByCustomerId(Integer customerId, OrderStatus status, Pageable pageable);

    @Query("""
            SELECT o FROM Order o
            LEFT JOIN FETCH o.shippingAddress
            LEFT JOIN FETCH o.items i
            LEFT JOIN FETCH i.product
            WHERE o.id = :id AND o.customer.id = :customerId
            """)
    Optional<Order> findWithItemsByIdAndCustomerId(Integer id, Integer customerId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<Order> findForUpdateByIdAndCustomerId(Integer id, Integer customerId);

    @Procedure("cancel_order")
    boolean cancelOrder(Integer customerId, Integer orderId);

    boolean existsByCustomerId(Integer customerId);

    boolean existsByShippingAddressId(Integer addressId);
}
