package fi.metropolia.zachrisz.online_store_backend.repository;

import fi.metropolia.zachrisz.online_store_backend.entity.Customer;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CustomerRepository extends JpaRepository<Customer, Integer> {
}
