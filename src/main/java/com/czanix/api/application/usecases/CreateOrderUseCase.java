package com.czanix.api.application.usecases;

import com.czanix.api.application.dtos.CreateOrderInput;
import com.czanix.api.application.dtos.OrderOutput;
import com.czanix.api.domain.Result;
import com.czanix.api.domain.entities.Order;
import com.czanix.api.domain.entities.OrderItem;
import com.czanix.api.domain.repositories.OrderRepository;
import org.springframework.stereotype.Service;

@Service
public class CreateOrderUseCase {
    private final OrderRepository repository;

    public CreateOrderUseCase(OrderRepository repository) {
        this.repository = repository;
    }

    public Result<OrderOutput> execute(CreateOrderInput input) {
        if (input.items().isEmpty()) {
            return Result.fail("Pedido deve ter pelo menos um item");
        }

        var items = input.items().stream()
                .map(i -> new OrderItem(i.productId(), i.quantity(), i.unitPrice()))
                .toList();

        var order = Order.create(input.customerId(), items);
        repository.save(order);

        return Result.ok(OrderOutput.from(order));
    }
}
