package ru.ya.receiver.service;

import org.springframework.stereotype.Service;
import ru.ya.libs.BasketDto;

import java.math.BigDecimal;

@Service
public class BasketService {

    public BigDecimal calculateTotal(BasketDto basket) {
        return basket.getProducts().stream()
                .map(p -> p.getPrice().multiply(BigDecimal.valueOf(p.getQuantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}