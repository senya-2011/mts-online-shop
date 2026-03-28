package com.mts.online_shop.controller;

import com.mts.online_shop.api.ProductsApi;
import com.mts.online_shop.mapper.ProductMapper;
import com.mts.online_shop.model.ProductListResponse;
import com.mts.online_shop.model.Product;
import com.mts.online_shop.service.GoodsService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@SecurityRequirement(name = "basicAuth")
public class ProductsController implements ProductsApi {

    private static final Logger log = LoggerFactory.getLogger(ProductsController.class);
    private final GoodsService goodsService;
    private final ProductMapper productMapper;

    public ProductsController(GoodsService goodsService, ProductMapper productMapper) {
        this.goodsService = goodsService;
        this.productMapper = productMapper;
    }

    @Override
    public ResponseEntity<ProductListResponse> getProducts() {
        log.debug("GET products");
        ProductListResponse response = new ProductListResponse();
        response.setItems(goodsService.findAllGoods().stream()
                .map(productMapper::toDto)
                .collect(Collectors.toList()));
        return ResponseEntity.ok(response);
    }

    @Override
    public ResponseEntity<Product> getProductById(Long productId) {
        log.debug("GET product id={}", productId);
        return ResponseEntity.ok(productMapper.toDto(goodsService.getProductById(productId)));
    }
}
