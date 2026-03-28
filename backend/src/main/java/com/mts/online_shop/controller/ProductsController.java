package com.mts.online_shop.controller;

import com.mts.online_shop.api.ProductsApi;
import com.mts.online_shop.mapper.ProductMapper;
import com.mts.online_shop.model.ProductListResponse;
import com.mts.online_shop.model.Product;
import com.mts.online_shop.service.GoodsService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
<<<<<<< HEAD
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
=======
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
>>>>>>> 8a4a50cbb9f97fde39a6ea063563c546e8320327
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
    public ResponseEntity<ProductListResponse> getProducts(Integer page, Integer size, String search) {
        log.debug("GET products page={} size={} search={}", page, size, search);
        Pageable pageable = PageRequest.of(page != null ? page : 0, size != null ? size : 20);
        var productsPage = goodsService.findProducts(pageable, search);
        ProductListResponse response = new ProductListResponse();
        response.setItems(productsPage.getContent().stream()
                .map(productMapper::toDto)
                .collect(Collectors.toList()));
        response.setTotal(productsPage.getTotalElements());
        response.setPage(productsPage.getNumber());
        response.setSize(productsPage.getSize());
        return ResponseEntity.ok(response);
    }

    @Override
    public ResponseEntity<Product> getProductById(Long productId) {
        log.debug("GET product id={}", productId);
        return ResponseEntity.ok(productMapper.toDto(goodsService.getProductById(productId)));
    }
}
