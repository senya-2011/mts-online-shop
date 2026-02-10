package com.mts.online_shop.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@Entity
@Table(name = "orders")
public class Order {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private OrderStatus status;

    @Column(name = "total_price", nullable = false)
    private float totalPrice;

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<OrderItem> items;

    @PrePersist
    private void calculateTotalPriceOnCreate() {
        if (items == null || items.isEmpty()) {
            totalPrice = 0f;
            return;
        }
        totalPrice = (float) items.stream()
                .map(OrderItem::getProduct)
                .mapToDouble(Product::getPrice)
                .sum();
    }
}

