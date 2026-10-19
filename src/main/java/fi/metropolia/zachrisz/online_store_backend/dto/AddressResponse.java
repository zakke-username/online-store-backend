package fi.metropolia.zachrisz.online_store_backend.dto;

import fi.metropolia.zachrisz.online_store_backend.entity.CustomerAddress;

public record AddressResponse(
        Integer id,
        String streetAddress,
        String postalCode,
        String city,
        String country,
        int version
) {

    public static AddressResponse from(CustomerAddress address) {
        return new AddressResponse(
                address.getId(),
                address.getStreetAddress(),
                address.getPostalCode(),
                address.getCity(),
                address.getCountry(),
                address.getVersion()
        );
    }
}
