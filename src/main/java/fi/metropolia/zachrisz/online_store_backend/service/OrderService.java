package fi.metropolia.zachrisz.online_store_backend.service;

import fi.metropolia.zachrisz.online_store_backend.dto.OrderRequest;
import fi.metropolia.zachrisz.online_store_backend.dto.OrderResponse;
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

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
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

    // An order of another customer is treated as not found
    @Transactional(readOnly = true)
    public Optional<OrderResponse> findById(Integer customerId, Integer orderId) {
        return orderRepository.findWithItemsByIdAndCustomerId(orderId, customerId).map(OrderResponse::from);
    }

    @Transactional(readOnly = true)
    public Optional<Page<OrderResponse>> findByCustomerId(Integer customerId, OrderStatus status,
                                                          Pageable pageable) {
        if (!customerRepository.existsById(customerId)) {
            return Optional.empty();
        }

        // Newest first
        Pageable newestFirst = PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(),
                Sort.by(Sort.Direction.DESC, "orderDate", "id"));

        return Optional.of(orderRepository.findByCustomerId(customerId, status, newestFirst)
                .map(OrderResponse::from));
    }

    // Read-lukko
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
        order.setStatus(OrderStatus.NEW);

        for (OrderRequest.Item item : request.items()) {
            Product product = products.get(item.productId());
            if (product == null) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Product " + item.productId() + " not found");
            }

            takeFromStock(product, item.quantity());
            // The current price is copied, so later price changes don't affect this order
            order.addItem(product, item.quantity(), product.getPrice());
        }

        return OrderResponse.from(orderRepository.save(order));
    }

    // The cancel_order procedure (schema.sql) only cancels NEW and PROCESSING orders, and returns their stock
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public OrderResponse cancel(Integer customerId, Integer orderId) {
        boolean cancelled = orderRepository.cancelOrder(customerId, orderId);

        OrderResponse order = findById(customerId, orderId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Customer " + customerId + " has no order " + orderId));
        if (!cancelled) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Order " + orderId + " is " + order.status() + " and can't be cancelled");
        }
        return order;
    }

    @Transactional(readOnly = true)
    public Optional<List<OrderResponse.Item>> findItems(Integer customerId, Integer orderId) {
        return findById(customerId, orderId).map(OrderResponse::items);
    }

    @Transactional(isolation = Isolation.READ_COMMITTED)
    public OrderResponse.Item addItem(Integer customerId, Integer orderId, OrderRequest.Item request) {
        Order order = lockNewOrder(customerId, orderId);
        if (order.findItem(request.productId()).isPresent()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Product " + request.productId() + " is already in order " + orderId);
        }

        Product product = productRepository.findForUpdateById(request.productId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Product " + request.productId() + " not found"));
        takeFromStock(product, request.quantity());
        // The current price is copied, so later price changes don't affect this order
        return OrderResponse.Item.from(order.addItem(product, request.quantity(), product.getPrice()));
    }

    @Transactional(isolation = Isolation.READ_COMMITTED)
    public OrderResponse.Item changeItemQuantity(Integer customerId, Integer orderId, Integer productId,
                                                 int quantity) {
        Order order = lockNewOrder(customerId, orderId);
        OrderItem item = findItem(order, productId);
        Product product = productRepository.findForUpdateById(productId).orElseThrow();

        // Only the difference is taken from stock; a smaller quantity returns items to stock.
        // The price stays the one copied when the product was added.
        takeFromStock(product, quantity - item.getQuantity());
        item.setQuantity(quantity);
        return OrderResponse.Item.from(item);
    }

    @Transactional(isolation = Isolation.READ_COMMITTED)
    public void removeItem(Integer customerId, Integer orderId, Integer productId) {
        Order order = lockNewOrder(customerId, orderId);
        OrderItem item = findItem(order, productId);
        // An order can't be left empty; the customer can cancel it instead
        if (order.getItems().size() == 1) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Product " + productId + " is the last item of order " + orderId + "; cancel the order instead");
        }

        Product product = productRepository.findForUpdateById(productId).orElseThrow();
        product.setStockQuantity(product.getStockQuantity() + item.getQuantity());
        order.removeItem(item);
    }

    // order is locked before its products: no deadlock
    private Order lockNewOrder(Integer customerId, Integer orderId) {
        Order order = orderRepository.findForUpdateByIdAndCustomerId(orderId, customerId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Customer " + customerId + " has no order " + orderId));
        if (order.getStatus() != OrderStatus.NEW) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Order " + orderId + " is " + order.getStatus() + " and its items can't be changed");
        }
        return order;
    }

    private OrderItem findItem(Order order, Integer productId) {
        return order.findItem(productId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Product " + productId + " is not in order " + order.getId()));
    }

    // lock the product rows
    private Map<Integer, Product> lockProducts(Collection<Integer> ids) {
        return productRepository.findForUpdateByIdIn(ids).stream()
                .collect(Collectors.toMap(Product::getId, Function.identity()));
    }

    private void takeFromStock(Product product, int quantity) {
        if (product.getStockQuantity() < quantity) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Not enough stock for product " + product.getId() + " (" + product.getStockQuantity() + " left)");
        }
        product.setStockQuantity(product.getStockQuantity() - quantity);
    }
}
