package com.czanix.api.application.dtos;

import com.czanix.api.domain.entities.Order;
import java.math.BigDecimal;

public record OrderOutput(String publicId, String customerId, String status,
                           BigDecimal total, String createdAt) {
    public static OrderOutput from(Order order) {
        return new OrderOutput(order.getPublicId(), order.getCustomerId(),
                order.getStatus(), order.getTotal(), order.getCreatedAt().toString());
    }
}
