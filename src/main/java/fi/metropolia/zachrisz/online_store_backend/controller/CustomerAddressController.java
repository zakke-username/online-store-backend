package fi.metropolia.zachrisz.online_store_backend.controller;

import fi.metropolia.zachrisz.online_store_backend.dto.AddressRequest;
import fi.metropolia.zachrisz.online_store_backend.dto.AddressResponse;
import fi.metropolia.zachrisz.online_store_backend.service.CustomerAddressService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/api/customers/{customerId}/addresses")
public class CustomerAddressController {

    private final CustomerAddressService customerAddressService;

    public CustomerAddressController(CustomerAddressService customerAddressService) {
        this.customerAddressService = customerAddressService;
    }

    @GetMapping
    public ResponseEntity<List<AddressResponse>> getAddresses(@PathVariable Integer customerId) {
        return customerAddressService.findByCustomerId(customerId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/{addressId}")
    public ResponseEntity<AddressResponse> getAddressById(@PathVariable Integer customerId,
                                                          @PathVariable Integer addressId) {
        return customerAddressService.findById(customerId, addressId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<AddressResponse> addAddress(@PathVariable Integer customerId,
                                                      @Valid @RequestBody AddressRequest request) {
        AddressResponse address = customerAddressService.add(customerId, request);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{addressId}")
                .buildAndExpand(address.id())
                .toUri();
        return ResponseEntity.created(location).body(address);
    }

    @PutMapping("/{addressId}")
    public AddressResponse updateAddress(@PathVariable Integer customerId, @PathVariable Integer addressId,
                                         @Valid @RequestBody AddressRequest request) {
        return customerAddressService.update(customerId, addressId, request);
    }

    @DeleteMapping("/{addressId}")
    public ResponseEntity<Void> deleteAddress(@PathVariable Integer customerId, @PathVariable Integer addressId) {
        customerAddressService.delete(customerId, addressId);
        return ResponseEntity.noContent().build();
    }
}
