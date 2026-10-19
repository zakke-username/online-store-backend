package fi.metropolia.zachrisz.online_store_backend.dto;

import fi.metropolia.zachrisz.online_store_backend.entity.Customer;

public record CustomerResponse(
        Integer id,
        String firstName,
        String lastName,
        String email,
        String phone
) {

    public static CustomerResponse from(Customer customer) {
        return new CustomerResponse(
                customer.getId(),
                customer.getFirstName(),
                customer.getLastName(),
                customer.getEmail(),
                customer.getPhone()
        );
    }
}
