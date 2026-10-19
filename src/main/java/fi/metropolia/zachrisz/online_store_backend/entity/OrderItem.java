package fi.metropolia.zachrisz.online_store_backend.entity;

import jakarta.persistence.*;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Objects;

// A row in OrderItems, identified by (order_id, product_id). Items only exist as part of an order,
// so they are saved and deleted through Order.items.
@Entity
@Table(name = "OrderItems")
public class OrderItem {

    @EmbeddedId
    private Key id = new Key();

    // @MapsId: the key's columns are the same as the foreign keys, so the key is filled in from these
    @MapsId("orderId")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id")
    private Order order;

    @MapsId("productId")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id")
    private Product product;

    @Column(name = "quantity", nullable = false)
    private int quantity;

    @Column(name = "unit_price", nullable = false, precision = 10, scale = 2)
    private BigDecimal unitPrice;

    protected OrderItem() {
    }

    public OrderItem(Order order, Product product, int quantity, BigDecimal unitPrice) {
        this.order = order;
        this.product = product;
        this.quantity = quantity;
        this.unitPrice = unitPrice;
    }

    public BigDecimal getLineTotal() {
        return unitPrice.multiply(BigDecimal.valueOf(quantity));
    }

    public Product getProduct() {
        return product;
    }

    public int getQuantity() {
        return quantity;
    }

    public void setQuantity(int quantity) {
        this.quantity = quantity;
    }

    public BigDecimal getUnitPrice() {
        return unitPrice;
    }

    @Embeddable
    public static class Key implements Serializable {

        @Column(name = "order_id")
        private Integer orderId;

        @Column(name = "product_id")
        private Integer productId;

        @Override
        public boolean equals(Object o) {
            return o instanceof Key key
                    && Objects.equals(orderId, key.orderId)
                    && Objects.equals(productId, key.productId);
        }

        @Override
        public int hashCode() {
            return Objects.hash(orderId, productId);
        }
    }
}
