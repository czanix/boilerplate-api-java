package com.czanix.api.domain.repositories;

import com.czanix.api.domain.entities.Order;
import java.util.Optional;

public interface OrderRepository {
    void save(Order order);
    Optional<Order> findByPublicId(String publicId);
    void update(Order order);
}
