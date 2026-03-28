package com.mts.online_shop.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Date;
import java.util.List;

@Getter
@Setter
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

    @Column(name = "total_price", nullable = false, precision = 19, scale = 2)
    private BigDecimal totalPrice;

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<OrderItem> items;

    @Column(name = "created_at")
    private Date createdAt;

    @Column(name = "updated_at")
    private Date updatedAt;

    @Column(name = "order_number", unique = true)
    private String orderNumber;

    @PrePersist
    private void onCreate() {
        createdAt = new Date();
        updatedAt = new Date();
        calculateTotalPriceOnCreate();
    }

    @PreUpdate
    private void onUpdate() {
        updatedAt = new Date();
    }

    private void calculateTotalPriceOnCreate() {
        if (items == null || items.isEmpty()) {
            totalPrice = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
            return;
        }
        totalPrice = items.stream()
                .map(OrderItem::getProduct)
                .map(ProductEntity::getPrice)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(2, RoundingMode.HALF_UP);
    }
}

