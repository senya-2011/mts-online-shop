package com.mts.online_shop.controller;

import com.mts.online_shop.model.MessageResponse;
import com.mts.online_shop.model.ProductListResponse;
import com.mts.online_shop.model.Product;
import com.mts.online_shop.model.ProductEntity;
import com.mts.online_shop.model.CreateProductRequest;
import com.mts.online_shop.model.UpdateProductRequest;
import com.mts.online_shop.camunda.BpmAdminService;
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

@RestController
@RequestMapping("/api/admin/products")
@PreAuthorize("hasRole('ADMIN')")
@io.swagger.v3.oas.annotations.tags.Tag(name = "Admin-Products", description = "Управление товарами")
public class AdminProductsController {

    private static final Logger log = LoggerFactory.getLogger(AdminProductsController.class);
    private final GoodsService goodsService;
    private final ProductMapper productMapper;
    private final BpmAdminService bpmAdminService;

    public AdminProductsController(GoodsService goodsService, ProductMapper productMapper,
                                   BpmAdminService bpmAdminService) {
        this.goodsService = goodsService;
        this.productMapper = productMapper;
        this.bpmAdminService = bpmAdminService;
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
    @io.swagger.v3.oas.annotations.Operation(
            summary = "Создать товар",
            description = "По умолчанию запускает BPM и оставляет форму в Tasklist. "
                    + "Синхронное создание: ?sync=true и тело {name, price}.")
    public ResponseEntity<?> createProduct(@RequestBody(required = false) CreateProductRequest request,
                                           @RequestParam(defaultValue = "false") boolean sync) {
        if (sync && request != null && request.getName() != null) {
            log.debug("POST admin product sync name={}", request.getName());
            Long productId = bpmAdminService.createProductSync(request.getName(), request.getPrice());
            ProductEntity entity = goodsService.getProductById(productId);
            return ResponseEntity.status(HttpStatus.CREATED).body(productMapper.toDto(entity));
        }
        return ResponseEntity.accepted().body(bpmAdminService.startCreateProduct());
    }

    @PutMapping("/{productId}")
    @io.swagger.v3.oas.annotations.Operation(
            summary = "Обновить товар",
            description = "По умолчанию запускает BPM-форму в Tasklist. Синхронно: ?sync=true и тело с полями.")
    public ResponseEntity<?> updateProduct(@PathVariable Long productId,
                                           @RequestBody(required = false) UpdateProductRequest request,
                                           @RequestParam(defaultValue = "false") boolean sync) {
        log.debug("PUT admin product id={} sync={}", productId, sync);
        if (sync && request != null) {
            bpmAdminService.updateProductSync(productId, request.getName(), request.getPrice());
            ProductEntity entity = goodsService.getProductById(productId);
            return ResponseEntity.ok(productMapper.toDto(entity));
        }
        return ResponseEntity.accepted().body(bpmAdminService.startUpdateProduct(productId));
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
