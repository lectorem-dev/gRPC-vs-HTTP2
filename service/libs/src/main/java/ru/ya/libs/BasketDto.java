package ru.ya.libs;

import lombok.Builder;
import lombok.Data;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Data
@Builder
public class BasketDto {
    private UUID id;
    private UUID customerId;
    private List<ProductDto> products;
    private Instant createdAt;
}