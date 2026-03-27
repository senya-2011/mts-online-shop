package com.bank_simulator.demo.repository;

import com.bank_simulator.demo.model.Card;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CardRepository extends JpaRepository<Card, Long> {

    Optional<Card> findByNumberAndCvvAndExpiresAt(String number, String cvv, String expiresAt);
}

