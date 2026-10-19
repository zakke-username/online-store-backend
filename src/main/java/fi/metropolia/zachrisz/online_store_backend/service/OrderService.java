package fi.metropolia.zachrisz.online_store_backend.service;

import fi.metropolia.zachrisz.online_store_backend.dto.OrderRequest;
import fi.metropolia.zachrisz.online_store_backend.dto.OrderResponse;
import fi.metropolia.zachrisz.online_store_backend.dto.OrderSummaryResponse;
import fi.metropolia.zachrisz.online_store_backend.entity.Customer;
import fi.metropolia.zachrisz.online_store_backend.entity.CustomerAddress;
import fi.metropolia.zachrisz.online_store_backend.entity.Order;
import fi.metropolia.zachrisz.online_store_backend.entity.OrderItem;
import fi.metropolia.zachrisz.online_store_backend.entity.OrderStatus;
import fi.metropolia.zachrisz.online_store_backend.entity.Product;
import fi.metropolia.zachrisz.online_store_backend.repository.CustomerAddressRepository;
import fi.metropolia.zachrisz.online_store_backend.repository.CustomerRepository;
import fi.metropolia.zachrisz.online_store_backend.repository.OrderRepository;
import fi.metropolia.zachrisz.online_store_backend.repository.ProductRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class OrderService {

    private final OrderRepository orderRepository;
    private final CustomerRepository customerRepository;
    private final CustomerAddressRepository customerAddressRepository;
    private final ProductRepository productRepository;

    public OrderService(OrderRepository orderRepository, CustomerRepository customerRepository,
                        CustomerAddressRepository customerAddressRepository, ProductRepository productRepository) {
        this.orderRepository = orderRepository;
        this.customerRepository = customerRepository;
        this.customerAddressRepository = customerAddressRepository;
        this.productRepository = productRepository;
    }

    @Transactional(readOnly = true)
    public Optional<OrderResponse> findById(Integer id) {
        return orderRepository.findWithItemsById(id).map(OrderResponse::from);
    }

    @Transactional(readOnly = true)
    public Optional<Page<OrderSummaryResponse>> findByCustomerId(Integer customerId, OrderStatus status,
                                                                 Pageable pageable) {
        if (!customerRepository.existsById(customerId)) {
            return Optional.empty();
        }

        // Newest first
        Pageable newestFirst = PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(),
                Sort.by(Sort.Direction.DESC, "orderDate", "id"));

        return Optional.of(orderRepository.findSummariesByCustomerId(customerId, status, newestFirst));
    }

    // The write methods lock rows (SELECT ... FOR UPDATE). READ COMMITTED lets a transaction that waited for
    // a lock read the row's latest committed state. Under MariaDB's default REPEATABLE READ, the waiting
    // transaction would fail with "Record has changed since last read" instead.
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public OrderResponse placeOrder(Integer customerId, OrderRequest request) {
        Customer customer = customerRepository.findById(customerId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Customer " + customerId + " not found"));

        CustomerAddress address = customerAddressRepository
                .findByIdAndCustomerId(request.shippingAddressId(), customerId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Address " + request.shippingAddressId() + " is not an address of customer " + customerId));

        // OrderItems has the primary key (order_id, product_id), so each product can only be listed once
        Set<Integer> productIds = new HashSet<>();
        for (OrderRequest.Item item : request.items()) {
            if (!productIds.add(item.productId())) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Product " + item.productId() + " is listed more than once");
            }
        }
        Map<Integer, Product> products = lockProducts(productIds);

        Order order = new Order();
        order.setCustomer(customer);
        order.setShippingAddress(address);
        // DATETIME stores whole seconds
        order.setOrderDate(LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS));
        order.setStatus(OrderStatus.NEW);

        for (OrderRequest.Item item : request.items()) {
            Product product = products.get(item.productId());
            if (product == null) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Product " + item.productId() + " not found");
            }
            if (product.getStockQuantity() < item.quantity()) {
                throw new ResponseStatusException(HttpStatus.CONFLICT,
                        "Not enough stock for product " + product.getId() + " (" + product.getStockQuantity() + " left)");
            }

            product.setStockQuantity(product.getStockQuantity() - item.quantity());
            // The current price is copied, so later price changes don't affect this order
            order.addItem(product, item.quantity(), product.getPrice());
        }

        return OrderResponse.from(orderRepository.save(order));
    }

    @Transactional(isolation = Isolation.READ_COMMITTED)
    public OrderResponse cancel(Integer orderId) {
        // Locked so two simultaneous cancel requests can't both put the items back into stock
        Order order = orderRepository.findForUpdateById(orderId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Order " + orderId + " not found"));

        if (order.getStatus() != OrderStatus.NEW && order.getStatus() != OrderStatus.PROCESSING) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Order " + orderId + " is " + order.getStatus() + " and can't be cancelled");
        }

        Set<Integer> productIds = order.getItems().stream()
                .map(item -> item.getProduct().getId())
                .collect(Collectors.toSet());
        Map<Integer, Product> products = lockProducts(productIds);

        for (OrderItem item : order.getItems()) {
            Product product = products.get(item.getProduct().getId());
            product.setStockQuantity(product.getStockQuantity() + item.getQuantity());
        }
        order.setStatus(OrderStatus.CANCELLED);

        return OrderResponse.from(order);
    }

    // Locking the product rows means two orders can't sell the same stock at the same time
    private Map<Integer, Product> lockProducts(Collection<Integer> ids) {
        return productRepository.findForUpdateByIdIn(ids).stream()
                .collect(Collectors.toMap(Product::getId, Function.identity()));
    }
}
