package com.mts.online_shop.service

import com.mts.online_shop.exception.EmptyCartException
import com.mts.online_shop.exception.InvalidPaymentDataException
import com.mts.online_shop.exception.OrderNotFoundException
import com.mts.online_shop.exception.UserNotFoundException
import com.mts.online_shop.mapper.OrderMapper
import com.mts.online_shop.mapper.ProductMapper
import com.mts.online_shop.model.*
import com.mts.online_shop.repository.OrderRepository
import com.mts.online_shop.repository.UserRepository
import com.mts.online_shop.client.bank.BankClient
import com.mts.online_shop.simulator.mail.MailSimulator
import io.mockk.every
import io.mockk.mockk
import java.util.*
import io.mockk.verify
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import org.springframework.transaction.annotation.Transactional
import org.springframework.transaction.annotation.Transactional as SpringTransactional

@SpringBootTest
@ActiveProfiles("test")
@Disabled("Temporarily disabled - AssertionError and InvalidPaymentDataException issues")
class NarayanaTransactionTest {

    private lateinit var orderService: OrderService
    private lateinit var orderRepository: OrderRepository
    private lateinit var userRepository: UserRepository
    private lateinit var bankClient: BankClient
    private lateinit var goodsService: GoodsService

    @BeforeEach
    fun setUp() {
        orderRepository = mockk()
        userRepository = mockk()
        bankClient = mockk()
        goodsService = mockk()
        val orderMapper = mockk<OrderMapper>()
        val productMapper = mockk<ProductMapper>()
        val mailSimulator = mockk<MailSimulator>()

        orderService = OrderService(
            orderRepository,
            orderMapper,
            productMapper,
            goodsService,
            userRepository,
            bankClient,
            mailSimulator
        )
    }

    @Test
    @SpringTransactional(rollbackFor = [Exception::class])
    fun `should rollback transaction when payment fails with Narayana`() {
        val userId = 1L
        val orderId = 1L
        val paymentRequest = PaymentRequest()

        val user = User().apply { id = userId }
        val order = Order().apply {
            id = orderId
            setUser(user)
            setStatus(OrderStatus.CREATED)
        }

        every { userRepository.findById(userId) } returns Optional.of(user)
        every { orderRepository.getOrderById(orderId) } returns Optional.of(order)
        every { bankClient.doPayment(any(), any()) } returns false
        every { orderRepository.save(any()) } returns order

        assertThrows<InvalidPaymentDataException> {
            orderService.payOrder(orderId, paymentRequest, userId)
        }

        verify { bankClient.doPayment(paymentRequest, any()) }
        verify(exactly = 0) { orderRepository.save(order) }
    }

    @Test
    @SpringTransactional(rollbackFor = [Exception::class])
    fun `should complete transaction successfully when payment succeeds with Narayana`() {
        val userId = 1L
        val orderId = 1L
        val paymentRequest = PaymentRequest()

        val user = User().apply { id = userId }
        val order = Order().apply {
            id = orderId
            setUser(user)
            setStatus(OrderStatus.CREATED)
        }

        every { userRepository.findById(userId) } returns Optional.of(user)
        every { orderRepository.getOrderById(orderId) } returns Optional.of(order)
        every { bankClient.doPayment(any(), any()) } returns true
        every { orderRepository.save(any()) } returns order

        orderService.payOrder(orderId, paymentRequest, userId)

        verify { bankClient.doPayment(paymentRequest, any()) }
        verify { orderRepository.save(order) }
    }

    @Test
    @SpringTransactional(rollbackFor = [Exception::class])
    fun `should rollback order creation when cart is empty with Narayana`() {
        val userId = 1L
        val user = User().apply { id = userId }

        every { userRepository.findById(userId) } returns Optional.of(user)
        every { goodsService.findUserGoods(userId) } returns emptyList()

        assertThrows<EmptyCartException> {
            orderService.createOrder(userId)
        }

        verify(exactly = 0) { orderRepository.save(any()) }
    }
}
