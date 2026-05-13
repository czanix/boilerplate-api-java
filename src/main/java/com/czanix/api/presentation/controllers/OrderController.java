package com.czanix.api.presentation.controllers;

import com.czanix.api.application.dtos.CreateOrderInput;
import com.czanix.api.application.usecases.CreateOrderUseCase;
import com.czanix.api.domain.Result;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/orders")
public class OrderController {
    private final CreateOrderUseCase createOrderUseCase;

    public OrderController(CreateOrderUseCase createOrderUseCase) {
        this.createOrderUseCase = createOrderUseCase;
    }

    @PostMapping
    public ResponseEntity<?> create(@Valid @RequestBody CreateOrderInput input) {
        var result = createOrderUseCase.execute(input);

        return switch (result) {
            case Result.Success<?> s -> ResponseEntity.status(201).body(s.value());
            case Result.Failure<?> f -> ResponseEntity.status(422).body(Map.of("error", f.error()));
        };
    }
}
