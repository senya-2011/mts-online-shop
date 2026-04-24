package com.mts.online_shop.controller;

import com.mts.online_shop.model.MessageResponse;
import com.mts.online_shop.model.ProductListResponse;
import com.mts.online_shop.model.Product;
import com.mts.online_shop.model.ProductEntity;
import com.mts.online_shop.model.CreateProductRequest;
import com.mts.online_shop.model.UpdateProductRequest;
import com.mts.online_shop.service.GoodsService;
import com.mts.online_shop.mapper.ProductMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;
import java.math.BigDecimal;

@RestController
@RequestMapping("/api/admin/products")
@PreAuthorize("hasRole('ADMIN')")
@io.swagger.v3.oas.annotations.tags.Tag(name = "Admin-Products", description = "Управление товарами")
public class AdminProductsController {

    private static final Logger log = LoggerFactory.getLogger(AdminProductsController.class);
    private final GoodsService goodsService;
    private final ProductMapper productMapper;

    public AdminProductsController(GoodsService goodsService, ProductMapper productMapper) {
        this.goodsService = goodsService;
        this.productMapper = productMapper;
    }

    @GetMapping
    @io.swagger.v3.oas.annotations.Operation(summary = "Получить товары (админ)", description = "Возвращает список всех товаров для администратора")
    public ResponseEntity<ProductListResponse> getProducts(@RequestParam(required = false) Integer page,
                                                          @RequestParam(required = false) Integer size) {
        log.debug("GET admin products page={} size={}", page, size);
        List<ProductEntity> products = goodsService.findAllGoodsIncludingDeleted();
        
        ProductListResponse response = new ProductListResponse();
        response.setItems(products.stream()
            .map(productMapper::toDto)
            .collect(Collectors.toList()));
        response.setTotal((long) products.size());
        response.setPage(page != null ? page : 0);
        response.setSize(size != null ? size : 20);
        return ResponseEntity.ok(response);
    }

    @PostMapping
    @io.swagger.v3.oas.annotations.Operation(summary = "Создать товар", description = "Создает новый товар в каталоге")
    public ResponseEntity<Product> createProduct(@RequestBody CreateProductRequest request) {
        log.debug("POST admin product name={}", request.getName());
        ProductEntity entity = goodsService.createProduct(
            request.getName(),
            BigDecimal.valueOf(request.getPrice())
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(productMapper.toDto(entity));
    }

    @PutMapping("/{productId}")
    @io.swagger.v3.oas.annotations.Operation(summary = "Обновить товар", description = "Обновляет информацию о существующем товаре")
    public ResponseEntity<Product> updateProduct(@PathVariable Long productId,
                                               @RequestBody UpdateProductRequest request) {
        log.debug("PUT admin product id={}", productId);
        ProductEntity entity = goodsService.updateProduct(
            productId,
            request.getName(),
            request.getPrice() != null ? BigDecimal.valueOf(request.getPrice()) : null
        );
        return ResponseEntity.ok(productMapper.toDto(entity));
    }

    @DeleteMapping("/{productId}")
    @io.swagger.v3.oas.annotations.Operation(summary = "Удалить товар", description = "Удаляет товар из каталога")
    public ResponseEntity<MessageResponse> deleteProduct(@PathVariable Long productId) {
        log.debug("DELETE admin product id={}", productId);
        goodsService.deleteProduct(productId);
        MessageResponse msg = new MessageResponse();
        msg.setMessage("Товар #" + productId + " удален");
        return ResponseEntity.ok(msg);
    }
}
