package com.czanix.api.domain.entities;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public class Order {
    private Long id;
    private final String publicId;
    private final String customerId;
    private final List<OrderItem> items;
    private String status;
    private final Instant createdAt;
    private Instant updatedAt;

    private Order(Long id, String publicId, String customerId, List<OrderItem> items,
                  String status, Instant createdAt, Instant updatedAt) {
        this.id = id;
        this.publicId = publicId;
        this.customerId = customerId;
        this.items = items;
        this.status = status;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public static Order create(String customerId, List<OrderItem> items) {
        if (items == null || items.isEmpty()) {
            throw new IllegalArgumentException("Pedido deve ter pelo menos um item");
        }
        return new Order(null, UUID.randomUUID().toString(), customerId, items,
                "pending", Instant.now(), Instant.now());
    }

    public static Order fromPersistence(Long id, String publicId, String customerId,
                                         List<OrderItem> items, String status,
                                         Instant createdAt, Instant updatedAt) {
        return new Order(id, publicId, customerId, items, status, createdAt, updatedAt);
    }

    public BigDecimal getTotal() {
        return items.stream()
                .map(i -> i.unitPrice().multiply(BigDecimal.valueOf(i.quantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public void cancel() {
        if ("delivered".equals(status)) throw new IllegalStateException("Cannot cancel delivered order");
        if ("cancelled".equals(status)) throw new IllegalStateException("Already cancelled");
        this.status = "cancelled";
        this.updatedAt = Instant.now();
    }

    public Long getId() { return id; }
    public String getPublicId() { return publicId; }
    public String getCustomerId() { return customerId; }
    public List<OrderItem> getItems() { return items; }
    public String getStatus() { return status; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}
