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
import com.mts.online_shop.service.GoodsService
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.IsolationMode
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.verify
import io.mockk.slot
import java.math.BigDecimal
import java.util.Optional

class GoodsServiceTest : BehaviorSpec({

    isolationMode = IsolationMode.InstancePerLeaf

    val goodsRepository = mockk<GoodsRepository>()
    val userItemRepository = mockk<UserItemRepository>()
    val userRepository = mockk<UserRepository>()
    val service = GoodsService(goodsRepository, userItemRepository, userRepository)

    val user = User().apply {
        id = 1L
        login = "user_1"
        name = "User"
        email = "user@mail.ru"
        passwordHash = "hash"
    }

    val product = ProductEntity().apply {
        id = 10L
        name = "Product"
        price = BigDecimal("99.99")
    }

    given("goods exist") {
        every { goodsRepository.findAll() } returns listOf(product)

        `when`("findAllGoods is called") {
            val result = service.findAllGoods()

            then("all goods are returned") {
                result shouldContainExactly listOf(product)
                verify(exactly = 1) { goodsRepository.findAll() }
            }
        }
    }

    given("user exists and product exists") {
        every { userRepository.findById(user.id) } returns Optional.of(user)
        every { goodsRepository.findById(product.id) } returns Optional.of(product)
        
        val savedItem = UserItem(user, product).apply { id = 1L }
        every { userItemRepository.save(match { it.user.id == user.id && it.product.id == product.id }) } returns savedItem

        `when`("addProductInUserCart is called") {
            val item = service.addProductInUserCart(user.id, product.id)

            then("user item is saved") {
                item.user shouldBe user
                item.product shouldBe product
                verify(exactly = 1) { userItemRepository.save(match { it.user.id == user.id && it.product.id == product.id }) }
            }
        }
    }

    given("user does not exist") {
        every { userRepository.findById(99L) } returns Optional.empty()

        `when`("findUserGoods is called") {
            then("UserNotFoundException is thrown") {
                shouldThrow<UserNotFoundException> {
                    service.findUserGoods(99L)
                }
            }
        }
    }

    given("product does not exist") {
        every { goodsRepository.findById(999L) } returns Optional.empty()

        `when`("getProductById is called") {
            then("ProductNotFoundException is thrown") {
                shouldThrow<ProductNotFoundException> {
                    service.getProductById(999L)
                }
            }
        }
    }

    given("product is absent in cart") {
        every { userRepository.findById(user.id) } returns Optional.of(user)
        every { goodsRepository.findById(product.id) } returns Optional.of(product)
        every { userItemRepository.existsByUser_IdAndProduct_Id(user.id, product.id) } returns false

        `when`("deleteProductFromCart is called") {
            then("ProductNotInCartException is thrown") {
                shouldThrow<ProductNotInCartException> {
                    service.deleteProductFromCart(user.id, product.id)
                }
                verify(exactly = 0) { userItemRepository.deleteByUser_IdAndProduct_Id(user.id, product.id) }
            }
        }
    }

    given("cart item exists") {
        val item = UserItem(user, product).apply { id = 15L }
        every { userItemRepository.findByIdAndUser_Id(item.id, user.id) } returns Optional.of(item)
        every { userItemRepository.delete(item) } just Runs

        `when`("removeCartItem is called") {
            service.removeCartItem(user.id, item.id)

            then("item is deleted") {
                verify(exactly = 1) { userItemRepository.delete(item) }
            }
        }
    }

    given("user exists with cart") {
        every { userRepository.findById(user.id) } returns Optional.of(user)
        every { userItemRepository.deleteAllByUser(user) } just Runs

        `when`("clearCart is called") {
            service.clearCart(user.id)

            then("all user items are deleted") {
                verify(exactly = 1) { userItemRepository.deleteAllByUser(user) }
            }
        }
    }
})
