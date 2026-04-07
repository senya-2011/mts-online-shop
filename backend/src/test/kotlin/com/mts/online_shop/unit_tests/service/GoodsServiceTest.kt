package com.mts.online_shop.unit_tests.service

import com.mts.online_shop.exception.ProductNotInCartException
import com.mts.online_shop.exception.ProductNotFoundException
import com.mts.online_shop.exception.UserNotFoundException
import com.mts.online_shop.model.ProductEntity
import com.mts.online_shop.model.User
import com.mts.online_shop.model.UserItem
import com.mts.online_shop.repository.GoodsRepository
import com.mts.online_shop.repository.UserItemRepository
import com.mts.online_shop.repository.UserRepository
import com.mts.online_shop.repository.OrderItemRepository
import com.mts.online_shop.service.GoodsService
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.collections.shouldContainExactly
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

    val user = User().apply {
        id = 1L
        login = "user_1"
        name = "User"
        email = "user@mail.ru"
        passwordHash = "hash"
    }

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
                result.content shouldContainExactly listOf(product)
                result.totalElements shouldBe 1L
            }
        }
        
        context("when search is provided") {
            val pageable = PageRequest.of(0, 20)
            every { goodsRepository.findByNameContainingIgnoreCase("prod", pageable) } returns PageImpl(listOf(product), pageable, 1L)

            it("should return matching products") {
                val result = service.findProducts(pageable, "prod")
                result.content shouldContainExactly listOf(product)
            }
        }
    }

    describe("addProductInUserCart") {
        
        context("when user and product exist") {
            every { userRepository.findById(user.id) } returns Optional.of(user)
            every { goodsRepository.findById(product.id) } returns Optional.of(product)
            every { userItemRepository.existsByUser_IdAndProduct_Id(user.id, product.id) } returns false
            
            val savedItem = UserItem(user, product).apply { id = 1L }
            every { userItemRepository.save(any()) } returns savedItem

            it("should save user item") {
                val item = service.addProductInUserCart(user.id, product.id)
                item.user shouldBe user
                item.product shouldBe product
            }
        }
        
        context("when user does not exist") {
            every { userRepository.findById(99L) } returns Optional.empty()

            it("should throw UserNotFoundException") {
                shouldThrow<UserNotFoundException> {
                    service.addProductInUserCart(99L, product.id)
                }
            }
        }
    }

    describe("findUserGoods") {
        
        context("when user exists") {
            every { userRepository.findById(user.id) } returns Optional.of(user)
            every { userItemRepository.findByUser_Id(user.id) } returns listOf(UserItem(user, product))

            it("should return user goods") {
                val result = service.findUserGoods(user.id)
                result shouldContainExactly listOf(product)
            }
        }
        
        context("when user does not exist") {
            every { userRepository.findById(99L) } returns Optional.empty()

            it("should throw UserNotFoundException") {
                shouldThrow<UserNotFoundException> {
                    service.findUserGoods(99L)
                }
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

    describe("deleteProductFromCart") {
        context("when product is absent in cart") {
            every { userRepository.findById(user.id) } returns Optional.of(user)
            every { goodsRepository.findById(product.id) } returns Optional.of(product)
            every { userItemRepository.existsByUser_IdAndProduct_Id(user.id, product.id) } returns false

            it("should throw ProductNotInCartException") {
                shouldThrow<ProductNotInCartException> {
                    service.deleteProductFromCart(user.id, product.id)
                }
            }
        }
    }

    describe("removeCartItem") {
        context("when cart item exists") {
            val item = UserItem(user, product).apply { id = 15L }
            every { userItemRepository.findByIdAndUser_Id(item.id, user.id) } returns Optional.of(item)

            it("should delete item") {
                service.removeCartItem(user.id, item.id)
            }
        }
    }

    describe("clearCart") {
        context("when user exists") {
            every { userRepository.findById(user.id) } returns Optional.of(user)

            it("should clear cart") {
                service.clearCart(user.id)
            }
        }
    }
})
