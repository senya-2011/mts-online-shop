package com.mts.online_shop.mapper;

import com.mts.online_shop.model.Order;
import com.mts.online_shop.model.OrderItem;
import com.mts.online_shop.model.OrderStatus;
import com.mts.online_shop.model.Product;
import com.mts.online_shop.model.ProductEntity;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;

@Component
public class OrderMapper {

    public com.mts.online_shop.model.OrderResponse toOrderResponse(Order order, ProductMapper productMapper) {
        if (order == null) {
            return null;
        }

        com.mts.online_shop.model.OrderResponse response = new com.mts.online_shop.model.OrderResponse();
        response.setOrderId(order.getId());
        response.setUserId(order.getUser() != null ? order.getUser().getId() : null);
        response.setStatus(mapStatus(order.getStatus()));
        response.setTotalPrice(order.getTotalPrice());
        response.setItems(mapItems(order.getItems(), productMapper));
        return response;
    }

    private com.mts.online_shop.model.OrderResponse.StatusEnum mapStatus(OrderStatus status) {
        if (status == null) {
            return null;
        }
        return switch (status) {
            case CREATED -> com.mts.online_shop.model.OrderResponse.StatusEnum.PENDING;
            case PAID, DELIVERED -> com.mts.online_shop.model.OrderResponse.StatusEnum.PAID;
            case CANCELLED -> com.mts.online_shop.model.OrderResponse.StatusEnum.CANCELLED;
        };
    }

    private List<Product> mapItems(List<OrderItem> items, ProductMapper productMapper) {
        if (items == null || items.isEmpty()) {
            return Collections.emptyList();
        }
        return items.stream()
                .map(oi -> productMapper.toDto(oi.getProduct()))
                .toList();
    }

    public List<com.mts.online_shop.model.OrderResponse> toOrderResponseList(
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
