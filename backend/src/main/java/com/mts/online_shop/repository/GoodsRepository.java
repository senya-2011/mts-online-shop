package com.mts.online_shop.repository;

import com.mts.online_shop.model.ProductEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface GoodsRepository extends JpaRepository<ProductEntity, Long> {

    Page<ProductEntity> findByNameContainingIgnoreCase(String name, Pageable pageable);
}
