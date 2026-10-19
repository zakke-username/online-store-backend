package fi.metropolia.zachrisz.online_store_backend.service;

import fi.metropolia.zachrisz.online_store_backend.dto.CustomerRequest;
import fi.metropolia.zachrisz.online_store_backend.dto.CustomerResponse;
import fi.metropolia.zachrisz.online_store_backend.entity.Customer;
import fi.metropolia.zachrisz.online_store_backend.repository.CustomerAddressRepository;
import fi.metropolia.zachrisz.online_store_backend.repository.CustomerRepository;
import fi.metropolia.zachrisz.online_store_backend.repository.OrderRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;

@Service
public class CustomerService {

    private final CustomerRepository customerRepository;
    private final CustomerAddressRepository customerAddressRepository;
    private final OrderRepository orderRepository;

    public CustomerService(CustomerRepository customerRepository,
                           CustomerAddressRepository customerAddressRepository, OrderRepository orderRepository) {
        this.customerRepository = customerRepository;
        this.customerAddressRepository = customerAddressRepository;
        this.orderRepository = orderRepository;
    }

    @Transactional(readOnly = true)
    public Optional<CustomerResponse> findById(Integer customerId) {
        return customerRepository.findById(customerId).map(CustomerResponse::from);
    }

    @Transactional(readOnly = true)
    public Optional<CustomerResponse> findByEmail(String email) {
        return customerRepository.findByEmail(email).map(CustomerResponse::from);
    }

    @Transactional
    public CustomerResponse register(CustomerRequest request) {
        if (customerRepository.existsByEmail(request.email())) {
            throw emailInUse(request.email());
        }

        Customer customer = new Customer();
        copyFields(request, customer);
        return CustomerResponse.from(customerRepository.save(customer));
    }

    @Transactional
    public CustomerResponse update(Integer customerId, CustomerRequest request) {
        Customer customer = findCustomer(customerId);
        if (customerRepository.existsByEmailAndIdNot(request.email(), customerId)) {
            throw emailInUse(request.email());
        }

        copyFields(request, customer);
        return CustomerResponse.from(customer);
    }

    @Transactional
    public void delete(Integer customerId) {
        Customer customer = findCustomer(customerId);
        // Orders are kept as history, so a customer with orders can't be deleted
        if (orderRepository.existsByCustomerId(customerId)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Customer " + customerId + " has orders and can't be deleted");
        }

        // Without orders, none of the customer's addresses are used by an order
        customerAddressRepository.deleteByCustomerId(customerId);
        customerRepository.delete(customer);
    }

    private Customer findCustomer(Integer customerId) {
        return customerRepository.findById(customerId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Customer " + customerId + " not found"));
    }

    private static ResponseStatusException emailInUse(String email) {
        return new ResponseStatusException(HttpStatus.CONFLICT, "Email " + email + " is already in use");
    }

    private static void copyFields(CustomerRequest request, Customer customer) {
        customer.setFirstName(request.firstName());
        customer.setLastName(request.lastName());
        customer.setEmail(request.email());
        customer.setPhone(request.phone());
    }
}
