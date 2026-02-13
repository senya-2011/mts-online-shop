package com.mts.online_shop.unit_tests.service

import com.mts.online_shop.exception.ProductNotFoundException
import com.mts.online_shop.exception.ProductNotInCartException
import com.mts.online_shop.exception.UserNotFoundException
import com.mts.online_shop.model.Product
import com.mts.online_shop.model.User
import com.mts.online_shop.model.UserItem
import com.mts.online_shop.repository.GoodsRepository
import com.mts.online_shop.repository.UserItemRepository
import com.mts.online_shop.repository.UserRepository
import com.mts.online_shop.service.GoodsService
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.IsolationMode
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.shouldBe
import io.mockk.Runs
import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.verify
import java.util.Optional

class GoodsServiceTest(): BehaviorSpec({

    isolationMode = IsolationMode.InstancePerTest
    
    val goodsRepository = mockk<GoodsRepository>()
    val userItemRepository = mockk<UserItemRepository>()
    val userRepository = mockk<UserRepository>()
    val goodsService = GoodsService(
        goodsRepository,
        userItemRepository,
        userRepository
    )

    val product1 = Product("test1", 1f)
    product1.id = 1L
    val product2 = Product("test2", 2f)
    product2.id = 2L
    val products = listOf(product1, product2)

    val user = User()
    user.id = 1L
    user.name = "test_name"
    user.email = "test_email"

    val userItem1 = UserItem()
    userItem1.id = 1L
    userItem1.user = user
    userItem1.product = product1
    val userItem2 = UserItem()
    userItem2.id = 2L
    userItem2.user = user
    userItem2.product = product2
    val userItems = listOf(userItem1, userItem2)

    every { userRepository.findById(user.id) } returns Optional.of(user)
    every { goodsRepository.findById(product1.id) } returns Optional.of(product1)
    every { userItemRepository.save(any()) } returns mockk()
    every { goodsRepository.findAll() } returns products
    every { userItemRepository.findByUser_Id(user.id) } returns userItems
    every { userItemRepository.deleteAllByUser(any()) } just Runs
    every { userItemRepository.deleteByUser_IdAndProduct_Id(user.id, product1.id) } just Runs


    given("goods exist in database") {
        `when`("findAllGoods is called") {
            val result = goodsService.findAllGoods()

            then("all products should be returned") {
                result shouldBe products
                verify(exactly = 1) { goodsRepository.findAll() }
            }
        }
    }

    given("user exist and has products in cart"){

        `when`("findUserGoods is called") {
            val result = goodsService.findUserGoods(user.id)
            then("the userGoods should be returned") {
                result shouldContainExactlyInAnyOrder products
            }
        }
        `when`("clearCart is called"){
            goodsService.clearCart(user.id)
            then("cart should be clear"){
                verify {userRepository.findById(user.id)}
                verify {userItemRepository.deleteAllByUser(any())}
            }
        }
    }

    given("user does not exist") {
        val userId = 99L
        every { userRepository.findById(userId) } returns Optional.empty()
        `when`("findUserGoods is called") {
            then("UserNotFoundException should be thrown") {
                shouldThrow<UserNotFoundException> {
                    goodsService.findUserGoods(userId)
                }
            }
        }
        `when`("addProductInUserCard is called") {
            then("UserNotFoundException should be thrown") {
                shouldThrow<UserNotFoundException> {
                    goodsService.addProductInUserCart(userId, product1.id)
                }
            }
        }
        `when`("deleteProductInUserCard is called") {
            then("UserNotFoundException should be thrown"){
                shouldThrow<UserNotFoundException> {
                    goodsService.deleteProductFromCart(userId, product1.id)
                }
            }
        }
        `when`("clearCart is called"){
            then("UserNotFountException should be thrown"){
                shouldThrow<UserNotFoundException> {
                    goodsService.clearCart(userId)
                }
            }
        }
    }

    given("user and product exist") {
        `when`("addProductInUserCard is called"){
            val result = goodsService.addProductInUserCart(user.id, product1.id)
            then("product should be added to cart") {
                result shouldBe product1
                verify { userItemRepository.save(any()) }
            }
        }
    }

    given("user, product, userItem exist"){
        every { userItemRepository.existsByUser_IdAndProduct_Id(user.id, product1.id) } returns true

        `when`("deleteProductFromCart is called"){
            goodsService.deleteProductFromCart(user.id, product1.id)
            then("product should be deleted from cart") {
                verify { userItemRepository.deleteByUser_IdAndProduct_Id(user.id, product1.id) }
            }
        }
    }

    given("product does not exist"){
        val productId = 99L
        every { goodsRepository.findById(productId) } returns Optional.empty()
        `when`("addProductInUserCard is called"){
            then("ProductNotFoundException should be thrown") {
                shouldThrow<ProductNotFoundException> {
                    goodsService.addProductInUserCart(user.id, productId)
                }
            }
        }
        `when`("deleteProductFromCart is called"){
            then("ProductNotFoundException should be thrown"){
                shouldThrow<ProductNotFoundException> {
                    goodsService.deleteProductFromCart(user.id, productId)
                }
            }
        }
    }

    given("UserItem does not exist"){
        every { userItemRepository.existsByUser_IdAndProduct_Id(user.id, product1.id) } returns false
        `when` ("deleteProductFromCart is called"){
            then("ProductNotInCartException should be thrown"){
                shouldThrow<ProductNotInCartException>{
                    goodsService.deleteProductFromCart(user.id, product1.id)
                }
                verify(exactly = 0){userItemRepository.deleteByUser_IdAndProduct_Id(user.id, product1.id) }
            }
        }
    }
})