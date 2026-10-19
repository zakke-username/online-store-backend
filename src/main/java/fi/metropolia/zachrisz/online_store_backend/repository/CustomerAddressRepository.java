package fi.metropolia.zachrisz.online_store_backend.repository;

import fi.metropolia.zachrisz.online_store_backend.entity.CustomerAddress;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface CustomerAddressRepository extends JpaRepository<CustomerAddress, Integer> {

    List<CustomerAddress> findByCustomerId(Integer customerId, Sort sort);

    Optional<CustomerAddress> findByIdAndCustomerId(Integer id, Integer customerId);

    // One DELETE statement instead of loading and deleting the addresses one by one
    @Modifying
    @Query("DELETE FROM CustomerAddress a WHERE a.customer.id = :customerId")
    void deleteByCustomerId(Integer customerId);
}
