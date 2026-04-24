package com.mts.online_shop.controller;

import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import com.mts.online_shop.mapper.ProductMapper;
import com.mts.online_shop.model.ProductListResponse;
import com.mts.online_shop.model.Product;
import com.mts.online_shop.service.GoodsService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/products")
@io.swagger.v3.oas.annotations.tags.Tag(name = "Products", description = "Просмотр товаров")
public class ProductsController {

    private static final Logger log = LoggerFactory.getLogger(ProductsController.class);
    private final GoodsService goodsService;
    private final ProductMapper productMapper;

    public ProductsController(GoodsService goodsService, ProductMapper productMapper) {
        this.goodsService = goodsService;
        this.productMapper = productMapper;
    }

    @GetMapping
    @io.swagger.v3.oas.annotations.Operation(summary = "Получить список товаров", description = "Возвращает список всех доступных товаров с пагинацией")
    public ResponseEntity<ProductListResponse> getProducts(@RequestParam(required = false) Integer page, 
                                                         @RequestParam(required = false) Integer size, 
                                                         @RequestParam(required = false) String search) {
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

    @GetMapping("/{productId}")
    @io.swagger.v3.oas.annotations.Operation(summary = "Получить товар по ID", description = "Возвращает детальную информацию о товаре по его уникальному идентификатору")
    public ResponseEntity<Product> getProductById(@PathVariable Long productId) {
        log.debug("GET product id={}", productId);
        return ResponseEntity.ok(productMapper.toDto(goodsService.getProductById(productId)));
    }
}
