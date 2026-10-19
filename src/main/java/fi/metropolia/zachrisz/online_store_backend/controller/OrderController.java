package fi.metropolia.zachrisz.online_store_backend.controller;

import fi.metropolia.zachrisz.online_store_backend.dto.ItemQuantityRequest;
import fi.metropolia.zachrisz.online_store_backend.dto.OrderRequest;
import fi.metropolia.zachrisz.online_store_backend.dto.OrderResponse;
import fi.metropolia.zachrisz.online_store_backend.entity.OrderStatus;
import fi.metropolia.zachrisz.online_store_backend.service.OrderService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.List;

// There is no login, so the customer is identified by the id in the path.
// Every endpoint checks that the order belongs to that customer (404 otherwise).
@RestController
@RequestMapping("/api/orders/customer/{customerId}")
public class OrderController {

    private final OrderService orderService;

    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    @GetMapping
    public ResponseEntity<Page<OrderResponse>> getOrdersByCustomer(
            @PathVariable Integer customerId,
            @RequestParam(required = false) OrderStatus status,
            Pageable pageable) {
        return orderService.findByCustomerId(customerId, status, pageable)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/{orderId}")
    public ResponseEntity<OrderResponse> getOrderById(@PathVariable Integer customerId,
                                                      @PathVariable Integer orderId) {
        return orderService.findById(customerId, orderId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<OrderResponse> placeOrder(@PathVariable Integer customerId,
                                                    @Valid @RequestBody OrderRequest request) {
        OrderResponse order = orderService.placeOrder(customerId, request);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{orderId}")
                .buildAndExpand(order.id())
                .toUri();
        return ResponseEntity.created(location).body(order);
    }

    @PostMapping("/{orderId}/cancel")
    public OrderResponse cancelOrder(@PathVariable Integer customerId, @PathVariable Integer orderId) {
        return orderService.cancel(customerId, orderId);
    }

    @GetMapping("/{orderId}/items")
    public ResponseEntity<List<OrderResponse.Item>> getOrderItems(@PathVariable Integer customerId,
                                                                 @PathVariable Integer orderId) {
        return orderService.findItems(customerId, orderId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping("/{orderId}/items")
    public ResponseEntity<OrderResponse.Item> addOrderItem(@PathVariable Integer customerId,
                                                           @PathVariable Integer orderId,
                                                           @Valid @RequestBody OrderRequest.Item request) {
        OrderResponse.Item item = orderService.addItem(customerId, orderId, request);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{productId}")
                .buildAndExpand(item.productId())
                .toUri();
        return ResponseEntity.created(location).body(item);
    }

    @PutMapping("/{orderId}/items/{productId}")
    public OrderResponse.Item changeOrderItemQuantity(@PathVariable Integer customerId,
                                                      @PathVariable Integer orderId,
                                                      @PathVariable Integer productId,
                                                      @Valid @RequestBody ItemQuantityRequest request) {
        return orderService.changeItemQuantity(customerId, orderId, productId, request.quantity());
    }

    @DeleteMapping("/{orderId}/items/{productId}")
    public ResponseEntity<Void> removeOrderItem(@PathVariable Integer customerId,
                                                @PathVariable Integer orderId,
                                                @PathVariable Integer productId) {
        orderService.removeItem(customerId, orderId, productId);
        return ResponseEntity.noContent().build();
    }
}
