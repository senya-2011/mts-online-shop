package com.mts.online_shop.unit_tests.service

import com.mts.online_shop.exception.EmptyCartException
import com.mts.online_shop.exception.InvalidPaymentDataException
import com.mts.online_shop.exception.OrderAccessDeniedException
import com.mts.online_shop.exception.OrderNotFoundException
import com.mts.online_shop.exception.UserNotFoundException
import com.mts.online_shop.mapper.OrderMapper
import com.mts.online_shop.mapper.ProductMapper
import com.mts.online_shop.model.Order
import com.mts.online_shop.model.OrderResponse
import com.mts.online_shop.model.OrderStatus
import com.mts.online_shop.model.PaymentRequest
import com.mts.online_shop.model.ProductEntity
import com.mts.online_shop.model.User
import com.mts.online_shop.repository.OrderRepository
import com.mts.online_shop.repository.UserRepository
import com.mts.online_shop.service.GoodsService
import com.mts.online_shop.service.OrderService
import com.mts.online_shop.client.bank.BankClient
import com.mts.online_shop.simulator.mail.MailSimulator
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.IsolationMode
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.verify
import java.math.BigDecimal
import java.util.Optional

class OrderServiceTest : BehaviorSpec({

    isolationMode = IsolationMode.InstancePerLeaf

    val orderRepository = mockk<OrderRepository>()
    val orderMapper = mockk<OrderMapper>()
    val productMapper = mockk<ProductMapper>()
    val goodsService = mockk<GoodsService>()
    val userRepository = mockk<UserRepository>()
    val bankClient = mockk<BankClient>()
    val mailSimulator = mockk<MailSimulator>()

    val service = OrderService(
        orderRepository,
        orderMapper,
        productMapper,
        goodsService,
        userRepository,
        bankClient,
        mailSimulator
    )

    val user = User().apply {
        id = 1L
        login = "user_1"
        name = "User"
        email = "user@mail.ru"
        passwordHash = "hash"
    }
    val otherUser = User().apply {
        id = 2L
        login = "user_2"
        name = "Other"
        email = "other@mail.ru"
        passwordHash = "hash"
    }

    val order = Order().apply {
        id = 100L
        this.user = user
        status = OrderStatus.CREATED
        totalPrice = BigDecimal("150.00")
        items = emptyList()
    }

    val orderResponse = OrderResponse().apply {
        id = 100L
        userId = 1L
        status = OrderResponse.StatusEnum.CREATED
        totalPrice = 150.0
        items = emptyList()
    }

    val paymentRequest = PaymentRequest(
        cardNumber = "1234123412341234",
        expiresAt = "12/30",
        cvv = "111"
    )

    given("order exists and belongs to current user") {
        every { orderRepository.getOrderById(order.id) } returns Optional.of(order)
        every { orderMapper.toOrderResponse(order, productMapper) } returns orderResponse

        `when`("getOrderByOrderId is called") {
            val result = service.getOrderByOrderId(order.id, user.id)

            then("mapped response is returned") {
                result shouldBe orderResponse
            }
        }
    }

    given("order exists but belongs to another user") {
        order.user = otherUser
        every { orderRepository.getOrderById(order.id) } returns Optional.of(order)

        `when`("getOrderByOrderId is called") {
            then("OrderAccessDeniedException is thrown") {
                shouldThrow<OrderAccessDeniedException> {
                    service.getOrderByOrderId(order.id, user.id)
                }
            }
        }
    }

    given("order does not exist") {
        every { orderRepository.getOrderById(999L) } returns Optional.empty()

        `when`("getOrderByOrderId is called") {
            then("OrderNotFoundException is thrown") {
                shouldThrow<OrderNotFoundException> {
                    service.getOrderByOrderId(999L, user.id)
                }
            }
        }
    }

    given("user exists but cart is empty") {
        every { userRepository.findById(user.id) } returns Optional.of(user)
        every { goodsService.findUserGoods(user.id) } returns emptyList()

        `when`("createOrder is called") {
            then("EmptyCartException is thrown") {
                shouldThrow<EmptyCartException> {
                    service.createOrder(user.id)
                }
            }
        }
    }

    given("user and cart items exist") {
        val products = listOf(
            ProductEntity("A", BigDecimal("10.00")).apply { id = 1L },
            ProductEntity("B", BigDecimal("20.00")).apply { id = 2L }
        )
        every { userRepository.findById(user.id) } returns Optional.of(user)
        every { goodsService.findUserGoods(user.id) } returns products
        every { orderMapper.toOrderItems(any(), products) } returns emptyList()
        every { orderRepository.save(any()) } answers { firstArg() }
        every { goodsService.clearCart(user.id) } just Runs

        `when`("createOrder is called") {
            service.createOrder(user.id)

            then("order is saved and cart is cleared") {
                verify(exactly = 1) { orderRepository.save(any()) }
                verify(exactly = 1) { goodsService.clearCart(user.id) }
            }
        }
    }

    given("user has orders") {
        every { userRepository.findById(user.id) } returns Optional.of(user)
        every { orderRepository.getOrdersByUserId(user.id) } returns listOf(order)
        every { orderMapper.toOrderResponseList(listOf(order), productMapper) } returns listOf(orderResponse)

        `when`("getOrdersByUserId is called") {
            val result = service.getOrdersByUserId(user.id)

            then("list of mapped responses is returned") {
                result shouldBe listOf(orderResponse)
            }
        }
    }

    given("payment fails in bank client") {
        order.user = user
        every { orderRepository.getOrderById(order.id) } returns Optional.of(order)
        every { userRepository.findById(user.id) } returns Optional.of(user)
        every { bankClient.doPayment(paymentRequest, order.totalPrice) } returns false

        `when`("payOrder is called") {
            then("InvalidPaymentDataException is thrown") {
                shouldThrow<InvalidPaymentDataException> {
                    service.payOrder(order.id, paymentRequest, user.id)
                }
                verify(exactly = 0) { mailSimulator.sendOrderPaidEmail(any(), any(), any()) }
            }
        }
    }

    given("payment succeeds") {
        order.user = user
        order.status = OrderStatus.CREATED
        every { orderRepository.getOrderById(order.id) } returns Optional.of(order)
        every { userRepository.findById(user.id) } returns Optional.of(user)
        every { bankClient.doPayment(paymentRequest, order.totalPrice) } returns true
        every { orderRepository.save(order) } returns order
        every { mailSimulator.sendOrderPaidEmail(user.email, order.id, order.totalPrice) } just Runs

        `when`("payOrder is called") {
            service.payOrder(order.id, paymentRequest, user.id)

            then("status becomes paid and notification is sent") {
                order.status shouldBe OrderStatus.PAID
                verify(exactly = 1) { orderRepository.save(order) }
                verify(exactly = 1) { mailSimulator.sendOrderPaidEmail(user.email, order.id, order.totalPrice) }
            }
        }
    }

    given("order references missing user") {
        every { orderRepository.getOrderById(order.id) } returns Optional.of(order)
        every { userRepository.findById(user.id) } returns Optional.empty()

        `when`("payOrder is called") {
            then("UserNotFoundException is thrown") {
                shouldThrow<UserNotFoundException> {
                    service.payOrder(order.id, paymentRequest, user.id)
                }
            }
        }
    }
})
