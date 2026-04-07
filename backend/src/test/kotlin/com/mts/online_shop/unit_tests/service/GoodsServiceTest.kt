package com.mts.online_shop.unit_tests.service

import com.mts.online_shop.exception.ProductNotFoundException
import com.mts.online_shop.exception.UserNotFoundException
import com.mts.online_shop.model.ProductEntity
import com.mts.online_shop.model.User
import com.mts.online_shop.repository.GoodsRepository
import com.mts.online_shop.repository.UserItemRepository
import com.mts.online_shop.repository.UserRepository
import com.mts.online_shop.repository.OrderItemRepository
import com.mts.online_shop.service.GoodsService
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.PageRequest
import java.util.Optional

class GoodsServiceTest : DescribeSpec({

    val goodsRepository = mock<GoodsRepository>()
    val userItemRepository = mock<UserItemRepository>()
    val userRepository = mock<UserRepository>()
    val orderItemRepository = mock<OrderItemRepository>()
    val service = GoodsService(goodsRepository, userItemRepository, userRepository, orderItemRepository)

    val product = ProductEntity().apply {
        id = 1L
        name = "Product"
        price = java.math.BigDecimal("10.00")
    }

    describe("findProducts") {
        
        context("when search is null") {
            val pageable = PageRequest.of(0, 20)
            whenever(goodsRepository.findAll(pageable)).thenReturn(PageImpl(listOf(product), pageable, 1L))

            it("should return page of products") {
                val result = service.findProducts(pageable, null)
                result shouldNotBe null
                result.content.size shouldBe 1
                result.totalElements shouldBe 1L
            }
        }
        
        context("when search is provided") {
            val pageable = PageRequest.of(0, 20)
            whenever(goodsRepository.findByNameContainingIgnoreCase("prod", pageable)).thenReturn(PageImpl(listOf(product), pageable, 1L))

            it("should return matching products") {
                val result = service.findProducts(pageable, "prod")
                result shouldNotBe null
                result.content.size shouldBe 1
            }
        }
    }

    describe("getProductById") {
        
        context("when product exists") {
            whenever(goodsRepository.findById(product.id)).thenReturn(Optional.of(product))

            it("should return product") {
                val result = service.getProductById(product.id)
                result shouldBe product
            }
        }
        
        context("when product does not exist") {
            whenever(goodsRepository.findById(999L)).thenReturn(Optional.empty())

            it("should throw ProductNotFoundException") {
                shouldThrow<ProductNotFoundException> {
                    service.getProductById(999L)
                }
            }
        }
    }
})
