package fi.metropolia.zachrisz.online_store_backend.dto;

import fi.metropolia.zachrisz.online_store_backend.entity.OrderStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;

// Created directly by OrderRepository's query, so there's no from() method
public record OrderSummaryResponse(
        Integer id,
        LocalDateTime orderDate,
        OrderStatus status,
        BigDecimal total
) {
}
