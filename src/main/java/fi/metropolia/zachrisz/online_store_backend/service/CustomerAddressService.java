package fi.metropolia.zachrisz.online_store_backend.service;

import fi.metropolia.zachrisz.online_store_backend.dto.AddressRequest;
import fi.metropolia.zachrisz.online_store_backend.dto.AddressResponse;
import fi.metropolia.zachrisz.online_store_backend.entity.Customer;
import fi.metropolia.zachrisz.online_store_backend.entity.CustomerAddress;
import fi.metropolia.zachrisz.online_store_backend.repository.CustomerAddressRepository;
import fi.metropolia.zachrisz.online_store_backend.repository.CustomerRepository;
import fi.metropolia.zachrisz.online_store_backend.repository.OrderRepository;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Optional;

@Service
public class CustomerAddressService {

    private final CustomerAddressRepository customerAddressRepository;
    private final CustomerRepository customerRepository;
    private final OrderRepository orderRepository;

    public CustomerAddressService(CustomerAddressRepository customerAddressRepository,
                                  CustomerRepository customerRepository, OrderRepository orderRepository) {
        this.customerAddressRepository = customerAddressRepository;
        this.customerRepository = customerRepository;
        this.orderRepository = orderRepository;
    }

    @Transactional(readOnly = true)
    public Optional<List<AddressResponse>> findByCustomerId(Integer customerId) {
        if (!customerRepository.existsById(customerId)) {
            return Optional.empty();
        }

        return Optional.of(customerAddressRepository.findByCustomerId(customerId, Sort.by("id")).stream()
                .map(AddressResponse::from)
                .toList());
    }

    @Transactional(readOnly = true)
    public Optional<AddressResponse> findById(Integer customerId, Integer addressId) {
        return customerAddressRepository.findByIdAndCustomerId(addressId, customerId).map(AddressResponse::from);
    }

    @Transactional
    public AddressResponse add(Integer customerId, AddressRequest request) {
        Customer customer = customerRepository.findById(customerId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Customer " + customerId + " not found"));

        CustomerAddress address = new CustomerAddress();
        address.setCustomer(customer);
        copyFields(request, address);
        return AddressResponse.from(customerAddressRepository.save(address));
    }

    @Transactional
    public AddressResponse update(Integer customerId, Integer addressId, AddressRequest request) {
        CustomerAddress address = findUnusedAddress(customerId, addressId);
        // Optimistic locking
        if (request.version() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "version is required");
        }
        if (request.version() != address.getVersion()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Address " + addressId
                    + " has changed since version " + request.version() + "; get it again and retry");
        }
        copyFields(request, address);
        return AddressResponse.from(customerAddressRepository.saveAndFlush(address));
    }

    @Transactional
    public void delete(Integer customerId, Integer addressId) {
        customerAddressRepository.delete(findUnusedAddress(customerId, addressId));
    }

    private CustomerAddress findUnusedAddress(Integer customerId, Integer addressId) {
        CustomerAddress address = customerAddressRepository.findByIdAndCustomerId(addressId, customerId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Customer " + customerId + " has no address " + addressId));
        if (orderRepository.existsByShippingAddressId(addressId)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Address " + addressId + " is used by an order; add a new address instead");
        }
        return address;
    }

    private static void copyFields(AddressRequest request, CustomerAddress address) {
        address.setStreetAddress(request.streetAddress());
        address.setPostalCode(request.postalCode());
        address.setCity(request.city());
        address.setCountry(request.country());
    }
}
