package com.bank_simulator.demo.model;

import jakarta.persistence.Column;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "cards")
public class Card {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "number", nullable = false, unique = true, length = 16)
    private String number;

    @Column(name = "cvv", nullable = false, length = 4)
    private String cvv;

    @Column(name = "expires_at", nullable = false, length = 5)
    private String expiresAt;

    @Column(name = "balance", nullable = false, precision = 19, scale = 2)
    private BigDecimal balance;
}

