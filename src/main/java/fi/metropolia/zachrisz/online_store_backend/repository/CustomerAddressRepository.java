package fi.metropolia.zachrisz.online_store_backend.repository;

import fi.metropolia.zachrisz.online_store_backend.entity.CustomerAddress;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CustomerAddressRepository extends JpaRepository<CustomerAddress, Integer> {

    Optional<CustomerAddress> findByIdAndCustomerId(Integer id, Integer customerId);
}
