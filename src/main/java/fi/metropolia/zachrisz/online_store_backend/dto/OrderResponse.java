package fi.metropolia.zachrisz.online_store_backend.dto;

import fi.metropolia.zachrisz.online_store_backend.entity.CustomerAddress;
import fi.metropolia.zachrisz.online_store_backend.entity.Order;
import fi.metropolia.zachrisz.online_store_backend.entity.OrderItem;
import fi.metropolia.zachrisz.online_store_backend.entity.OrderStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;

public record OrderResponse(
        Integer id,
        Integer customerId,
        LocalDateTime orderDate,
        LocalDateTime deliveryDate,
        OrderStatus status,
        AddressResponse shippingAddress,
        List<Item> items,
        BigDecimal total
) {

    public record Item(
            Integer productId,
            String productName,
            int quantity,
            BigDecimal unitPrice,
            BigDecimal lineTotal
    ) {

        public static Item from(OrderItem item) {
            return new Item(
                    item.getProduct().getId(),
                    item.getProduct().getName(),
                    item.getQuantity(),
                    item.getUnitPrice(),
                    item.getLineTotal()
            );
        }
    }

    public static OrderResponse from(Order order) {
        CustomerAddress address = order.getShippingAddress();
        return new OrderResponse(
                order.getId(),
                order.getCustomer().getId(),
                order.getOrderDate(),
                order.getDeliveryDate(),
                order.getStatus(),
                address != null ? AddressResponse.from(address) : null,
                order.getItems().stream()
                        .map(Item::from)
                        .sorted(Comparator.comparing(Item::productId))
                        .toList(),
                order.getTotal()
        );
    }
}
