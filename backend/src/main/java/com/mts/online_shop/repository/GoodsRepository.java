package com.mts.online_shop.repository;

import com.mts.online_shop.model.ProductEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface GoodsRepository extends JpaRepository<ProductEntity, Long> {
}
