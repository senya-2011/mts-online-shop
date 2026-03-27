package com.mts.online_shop.mapper;

import com.mts.online_shop.model.Product;
import com.mts.online_shop.model.ProductEntity;
import org.springframework.stereotype.Component;

@Component
public class ProductMapper {

    public Product toDto(ProductEntity entity) {
        if (entity == null) {
            return null;
        }
        Product product = new Product();
        product.setId(entity.getId());
        product.setName(entity.getName());
        product.setPrice(entity.getPrice());
        return product;
    }
}
