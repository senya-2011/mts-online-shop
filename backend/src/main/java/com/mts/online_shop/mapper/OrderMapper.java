package com.mts.online_shop.mapper;

import com.mts.online_shop.model.Order;
import com.mts.online_shop.model.OrderItem;
import com.mts.online_shop.model.OrderStatus;
import com.mts.online_shop.model.Product;
import com.mts.online_shop.model.ProductEntity;
import com.mts.online_shop.model.OrderResponse;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;

@Component
public class OrderMapper {

    public OrderResponse toOrderResponse(Order order, ProductMapper productMapper) {
        if (order == null) {
            return null;
        }

        OrderResponse response = new OrderResponse();
        response.setId(order.getId());
        response.setUserId(order.getUser() != null ? order.getUser().getId() : null);
        response.setStatus(mapStatus(order.getStatus()));
        response.setTotalPrice(order.getTotalPrice().doubleValue()); 
        response.setItems(mapItems(order.getItems(), productMapper));
        return response;
    }

    private OrderResponse.StatusEnum mapStatus(OrderStatus status) {
        if (status == null) {
            return null;
        }
        return switch (status) {
            case CREATED -> OrderResponse.StatusEnum.CREATED;
            case PAID, DELIVERED -> OrderResponse.StatusEnum.PAID;
            case CANCELLED -> OrderResponse.StatusEnum.CANCELLED;
        };
    }

    private List<com.mts.online_shop.model.CartItem> mapItems(List<OrderItem> items, ProductMapper productMapper) {
        if (items == null || items.isEmpty()) {
            return Collections.emptyList();
        }
        return items.stream()
                .map(oi -> {
                    com.mts.online_shop.model.CartItem cartItem = new com.mts.online_shop.model.CartItem();
                    cartItem.setId(oi.getId());
                    cartItem.setProduct(productMapper.toDto(oi.getProduct()));
                    return cartItem;
                })
                .toList();
    }

    public List<OrderResponse> toOrderResponseList(
            List<Order> orders,
            ProductMapper productMapper) {
        if (orders == null || orders.isEmpty()) {
            return Collections.emptyList();
        }
        return orders.stream()
                .map(o -> toOrderResponse(o, productMapper))
                .toList();
    }

    private OrderItem toOrderItem(Order order, ProductEntity product) {
        OrderItem orderItem = new OrderItem();
        orderItem.setOrder(order);
        orderItem.setProduct(product);
        return orderItem;
    }

    public List<OrderItem> toOrderItems(Order order, List<ProductEntity> products) {
        if (products == null || products.isEmpty()) {
            return Collections.emptyList();
        }
        return products.stream()
                .map(product -> toOrderItem(order, product))
                .toList();
    }
}
