package com.mts.online_shop.mapper;

import com.mts.online_shop.model.Order;
import com.mts.online_shop.model.OrderItem;
import com.mts.online_shop.model.OrderRequest;
import com.mts.online_shop.model.OrderResponse;
import com.mts.online_shop.model.Product;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Mapper(componentModel = "spring")
public interface OrderMapper {

    @Mapping(target = "orderId", source = "id")
    @Mapping(target = "userId", source = "user.id")
    @Mapping(target = "status", source = "status")
    @Mapping(target = "totalPrice", source = "totalPrice")
    @Mapping(target = "goods", expression = "java(mapItems(order.getItems()))")
    OrderResponse toOrderResponse(Order order);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "status", constant = "CREATED")
    @Mapping(target = "totalPrice", ignore = true)
    @Mapping(target = "items", ignore = true)
    @Mapping(target = "user.id", source = "userId")
    Order toOrder(OrderRequest orderRequest);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "order", source = "order")
    @Mapping(target = "product", source = "product")
    OrderItem toOrderItem(Order order, Product product);

    default List<OrderItem> toOrderItems(Order order, List<Product> products) {
        if (products == null || products.isEmpty()) {
            return Collections.emptyList();
        }
        return products.stream()
                .map(product -> toOrderItem(order, product))
                .toList();
    }

    default List<Product> mapItems(List<OrderItem> items) {
        if (items == null || items.isEmpty()) {
            return Collections.emptyList();
        }
        return items.stream()
                .map(OrderItem::getProduct)
                .collect(Collectors.toList());
    }
}

