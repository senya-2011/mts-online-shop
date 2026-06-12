package com.mts.online_shop.service;

import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class ProductReservationService {

    private final Map<Long, Integer> reservedByProduct = new ConcurrentHashMap<>();

    public void reserveProduct(Long productId, int quantity) {
        reservedByProduct.merge(productId, quantity, Integer::sum);
    }

    public void releaseProduct(Long productId, int quantity) {
        reservedByProduct.computeIfPresent(productId, (id, current) -> {
            int updated = current - quantity;
            return updated > 0 ? updated : null;
        });
    }
}
