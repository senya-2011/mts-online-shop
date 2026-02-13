package com.mts.online_shop.unit_tests.service

import com.mts.online_shop.exception.EmptyCartException
import com.mts.online_shop.exception.InvalidPaymentDataException
import com.mts.online_shop.exception.OrderNotFoundException
import com.mts.online_shop.exception.UserNotFoundException
import com.mts.online_shop.mapper.OrderMapper
import com.mts.online_shop.model.Order
import com.mts.online_shop.model.OrderRequest
import com.mts.online_shop.model.OrderResponse
import com.mts.online_shop.model.OrderStatus
import com.mts.online_shop.model.PaymentRequest
import com.mts.online_shop.model.Product
import com.mts.online_shop.model.User
import io.mockk.just
import com.mts.online_shop.repository.OrderRepository
import com.mts.online_shop.repository.UserRepository
import com.mts.online_shop.service.GoodsService
import com.mts.online_shop.service.OrderService
import com.mts.online_shop.simulator.bank.BankSimulator
import com.mts.online_shop.simulator.mail.MailSimulator
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.IsolationMode
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.mockk.Runs
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import java.util.Optional


class OrderServiceTest(): BehaviorSpec({

    isolationMode = IsolationMode.InstancePerLeaf

    val orderRepository = mockk<OrderRepository>()
    val orderMapper = mockk<OrderMapper>()
    val goodsService = mockk<GoodsService>()
    val userRepository = mockk<UserRepository>()
    val bankSimulator = mockk<BankSimulator>()
    val mailSimulator = mockk<MailSimulator>()
    val orderService = OrderService(
        orderRepository,
        orderMapper,
        goodsService,
        userRepository,
        bankSimulator,
        mailSimulator
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

    val order1 = Order()
    order1.id = 1L
    order1.user = user
    val orders = listOf(order1)

    val savedOrder = Order()
    savedOrder.id = 1L
    savedOrder.user = user
    savedOrder.status = OrderStatus.CREATED

    val orderResponse = OrderResponse()
    val orderResponseList = listOf(orderResponse)
    val orderRequest = OrderRequest()
    orderRequest.userId = 1L

    val paymentRequest = PaymentRequest()


    every { orderRepository.getOrderById(order1.id) } returns Optional.of(order1)
    every { orderMapper.toOrderResponse(any()) } returns orderResponse
    every { userRepository.findById(user.id) } returns Optional.of(user)
    every { goodsService.findUserGoods(user.id) } returns products
    every { orderMapper.toOrder(any()) } returns order1
    every { orderMapper.toOrderItems(order1, products) } returns listOf()
    every { orderRepository.save(order1) } returns savedOrder
    every { goodsService.clearCart(any()) } just Runs
    every { orderRepository.getOrdersByUserId(user.id) } returns orders
    every { orderMapper.toOrderResponseList(orders) } returns orderResponseList
    every { bankSimulator.doPayment(paymentRequest, any()) } returns true
    every { mailSimulator.sendOrderPaidEmail(any(), any(), any()) } just Runs


    given("order exists"){
        `when`("getOrderByOrderId is called"){
            val result = orderService.getOrderByOrderId(order1.id)
            then("OrderResponse should be returned") {
                result shouldBe orderResponse
            }
        }
    }

    given("user exists"){
        `when`("createOrder is called"){
            val result = orderService.createOrder(orderRequest)
            then("created OrderResponse should be returned") {
                result shouldBe orderResponse
                verify(exactly=1) { orderRepository.save(order1) }
            }
        }
        `when`("getOrdersByUserId is called"){
            val result = orderService.getOrdersByUserId(user.id)
            then("List<OrderResponse should be returned") {
                result shouldBe orderResponseList
            }
        }
    }

    given("order does not exists"){
        val orderId = 99L
        every { orderRepository.getOrderById(orderId) } returns Optional.empty()
        `when`("getOrderByOrderId is called"){
            then("OrderNotFoundException should be thrown") {
                shouldThrow<OrderNotFoundException> {
                    orderService.getOrderByOrderId(orderId)
                }
            }

        }
        `when`("payOrder is called"){
            then("OrderNotFoundException should be thrown"){
                shouldThrow<OrderNotFoundException> {
                    orderService.payOrder(orderId, paymentRequest)
                }
            }
        }
    }

    given("user does not exists"){
        val userId = 99L
        val exceptionOrderRequest = OrderRequest()
        exceptionOrderRequest.userId = userId
        every { userRepository.findById(exceptionOrderRequest.userId) } returns Optional.empty()

        `when`("createOrder is called"){
            then("UserNotFoundException should be thrown") {
                shouldThrow<UserNotFoundException> {
                    orderService.createOrder(exceptionOrderRequest)
                }
            }
        }
        `when`("getOrderByUserId is called"){
            then("UserNotFoundException should be thrown"){
                shouldThrow<UserNotFoundException> {
                    orderService.getOrdersByUserId(userId)
                }
            }
        }
        `when`("payOrder is called"){
            then("UserNotFoundException should be thrown"){
                shouldThrow<UserNotFoundException> {
                    orderService.getOrdersByUserId(userId)
                }
            }
        }
    }

    given("User Product Cart is Empty"){
        every { goodsService.findUserGoods(any()) } returns listOf()
        `when`("createOrder is called"){
            then("EmptyCartException should be thrown"){
                shouldThrow<EmptyCartException> {
                    orderService.createOrder(orderRequest)
                }
            }
        }
    }

    given("order exist and paymentRequest is valid"){
        `when`("payOrder is called"){
            val result = orderService.payOrder(order1.id, paymentRequest)
            then("OrderResponse should be returned") {
                result shouldBe orderResponse
                verify(exactly=1) { orderRepository.save(any()) }
                verify(exactly=1) { mailSimulator.sendOrderPaidEmail(any(), any(), any())}
                verify(exactly=1) { bankSimulator.doPayment(any(), any()) }
            }
        }
    }

    given("invalid payment data"){
        every { bankSimulator.doPayment(any(), any()) } returns false
        `when`("payOrder is called"){
            then("InvalidPaymentDataException should be thrown"){
                shouldThrow<InvalidPaymentDataException> {
                    orderService.payOrder(order1.id, paymentRequest)
                }
                verify(exactly=1) { bankSimulator.doPayment(any(), any()) }
                verify(exactly=0) { orderRepository.save(any()) }
            }
        }
    }
})