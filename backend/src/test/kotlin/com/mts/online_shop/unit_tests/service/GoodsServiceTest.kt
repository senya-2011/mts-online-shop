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
import io.mockk.every
import io.mockk.mockk
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.PageRequest
import java.util.Optional

class GoodsServiceTest : DescribeSpec({

    val goodsRepository = mockk<GoodsRepository>()
    val userItemRepository = mockk<UserItemRepository>()
    val userRepository = mockk<UserRepository>()
    val orderItemRepository = mockk<OrderItemRepository>()
    val service = GoodsService(goodsRepository, userItemRepository, userRepository, orderItemRepository)

    val product = ProductEntity().apply {
        id = 1L
        name = "Product"
        price = java.math.BigDecimal("10.00")
    }

    describe("findProducts") {
        
        context("when search is null") {
            val pageable = PageRequest.of(0, 20)
            every { goodsRepository.findAll(pageable) } returns PageImpl(listOf(product), pageable, 1L)

            it("should return page of products") {
                val result = service.findProducts(pageable, null)
                result.content.size shouldBe 1
                result.totalElements shouldBe 1L
            }
        }
        
        context("when search is provided") {
            val pageable = PageRequest.of(0, 20)
            every { goodsRepository.findByNameContainingIgnoreCase("prod", pageable) } returns PageImpl(listOf(product), pageable, 1L)

            it("should return matching products") {
                val result = service.findProducts(pageable, "prod")
                result.content.size shouldBe 1
            }
        }
    }

    describe("getProductById") {
        
        context("when product exists") {
            every { goodsRepository.findById(product.id) } returns Optional.of(product)

            it("should return product") {
                val result = service.getProductById(product.id)
                result shouldBe product
            }
        }
        
        context("when product does not exist") {
            every { goodsRepository.findById(999L) } returns Optional.empty()

            it("should throw ProductNotFoundException") {
                shouldThrow<ProductNotFoundException> {
                    service.getProductById(999L)
                }
            }
        }
    }
})
