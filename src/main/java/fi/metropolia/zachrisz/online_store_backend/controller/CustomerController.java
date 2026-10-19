package fi.metropolia.zachrisz.online_store_backend.controller;

import fi.metropolia.zachrisz.online_store_backend.dto.OrderRequest;
import fi.metropolia.zachrisz.online_store_backend.dto.OrderResponse;
import fi.metropolia.zachrisz.online_store_backend.dto.OrderSummaryResponse;
import fi.metropolia.zachrisz.online_store_backend.entity.OrderStatus;
import fi.metropolia.zachrisz.online_store_backend.service.OrderService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;

@RestController
@RequestMapping("/api/customers")
public class CustomerController {

    private final OrderService orderService;

    public CustomerController(OrderService orderService) {
        this.orderService = orderService;
    }

    @GetMapping("/{customerId}/orders")
    public ResponseEntity<Page<OrderSummaryResponse>> getOrdersByCustomer(
            @PathVariable Integer customerId,
            @RequestParam(required = false) OrderStatus status,
            Pageable pageable) {
        return orderService.findByCustomerId(customerId, status, pageable)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping("/{customerId}/orders")
    public ResponseEntity<OrderResponse> placeOrder(@PathVariable Integer customerId,
                                                    @Valid @RequestBody OrderRequest request) {
        OrderResponse order = orderService.placeOrder(customerId, request);
        URI location = ServletUriComponentsBuilder.fromCurrentContextPath()
                .path("/api/orders/{id}")
                .buildAndExpand(order.id())
                .toUri();
        return ResponseEntity.created(location).body(order);
    }
}
