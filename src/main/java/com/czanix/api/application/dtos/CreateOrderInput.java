package com.czanix.api.application.dtos;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.util.List;

public record CreateOrderInput(
    @NotNull String customerId,
    @NotEmpty List<ItemInput> items
) {
    public record ItemInput(String productId, int quantity, BigDecimal unitPrice) {}
}
