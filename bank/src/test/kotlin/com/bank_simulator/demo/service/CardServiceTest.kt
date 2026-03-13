package com.bank_simulator.demo.service

import com.bank_simulator.demo.exception.CardNotFoundException
import com.bank_simulator.demo.exception.InsufficientFundsException
import com.bank_simulator.demo.exception.InvalidAmountException
import com.bank_simulator.demo.exception.InvalidCardDataException
import com.bank_simulator.demo.mapper.CardMapper
import com.bank_simulator.demo.model.Card
import com.bank_simulator.demo.model.CardsResponse
import com.bank_simulator.demo.model.PaymentRequest
import com.bank_simulator.demo.repository.CardRepository
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.IsolationMode
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.mockk.*
import java.math.BigDecimal
import java.util.Optional

class CardServiceTest : BehaviorSpec({

    isolationMode = IsolationMode.InstancePerLeaf

    val cardRepository = mockk<CardRepository>()
    val cardMapper = mockk<CardMapper>()
    val service = CardService(cardRepository, cardMapper)

    val card = Card().apply {
        id = 1L
        number = "1111222233334444"
        cvv = "111"
        expiresAt = "12/30"
        balance = BigDecimal("1000.00")
    }

    given("cards exist") {
        every { cardRepository.findAll() } returns listOf(card)
        val response = CardsResponse().apply { items = emptyList() }
        every { cardMapper.toCardsResponse(listOf(card)) } returns response

        `when`("getAllCards is called") {
            val result = service.getAllCards()

            then("mapper result is returned") {
                result shouldBe response
                verify(exactly = 1) { cardRepository.findAll(); Unit }
                verify(exactly = 1) { cardMapper.toCardsResponse(listOf(card)); Unit }
            }
        }
    }

    given("randomizeBalances") {
        every { cardRepository.findAll() } returns listOf(card)
        every { cardRepository.saveAll(any<List<Card>>()) } returns listOf(card)
        val response = CardsResponse().apply { items = emptyList() }
        every { cardMapper.toCardsResponse(any<List<Card>>()) } returns response

        `when`("randomizeBalances is called") {
            val result = service.randomizeBalances()

            then("balances are updated and response returned") {
                result shouldBe response
                verify(exactly = 1) { cardRepository.saveAll(any<List<Card>>()); Unit }
                verify(exactly = 1) { cardMapper.toCardsResponse(any<List<Card>>()); Unit }
            }
        }
    }

    given("processPayment with null amount") {
        `when`("processPayment is called") {
            then("InvalidAmountException is thrown") {
                val request = PaymentRequest().apply {
                    cardNumber = "1111222233334444"
                    cvv = "111"
                    expiresAt = "12/30"
                    amount = null
                }
                shouldThrow<InvalidAmountException> {
                    service.processPayment(request)
                }
            }
        }
    }

    given("processPayment with zero amount") {
        `when`("processPayment is called") {
            then("InvalidAmountException is thrown") {
                val request = PaymentRequest().apply {
                    cardNumber = "1111222233334444"
                    cvv = "111"
                    expiresAt = "12/30"
                    amount = 0f
                }
                shouldThrow<InvalidAmountException> {
                    service.processPayment(request)
                }
            }
        }
    }

    given("processPayment with invalid card number") {
        `when`("processPayment is called with 15 digits") {
            then("InvalidCardDataException is thrown") {
                val request = PaymentRequest().apply {
                    cardNumber = "111122223333444"
                    cvv = "111"
                    expiresAt = "12/30"
                    amount = 50f
                }
                shouldThrow<InvalidCardDataException> {
                    service.processPayment(request)
                }
            }
        }
    }

    given("processPayment with invalid CVV") {
        `when`("processPayment is called with 2-digit CVV") {
            then("InvalidCardDataException is thrown") {
                val request = PaymentRequest().apply {
                    cardNumber = "1111222233334444"
                    cvv = "11"
                    expiresAt = "12/30"
                    amount = 50f
                }
                shouldThrow<InvalidCardDataException> {
                    service.processPayment(request)
                }
            }
        }
    }

    given("processPayment with invalid expiresAt format") {
        `when`("processPayment is called with wrong format") {
            then("InvalidCardDataException is thrown") {
                val request = PaymentRequest().apply {
                    cardNumber = "1111222233334444"
                    cvv = "111"
                    expiresAt = "13/30"
                    amount = 50f
                }
                shouldThrow<InvalidCardDataException> {
                    service.processPayment(request)
                }
            }
        }
    }

    given("card not found") {
        every {
            cardRepository.findByNumberAndCvvAndExpiresAt("9999999999999999", "111", "12/30")
        } returns Optional.empty()

        `when`("processPayment is called") {
            then("CardNotFoundException is thrown") {
                val request = PaymentRequest().apply {
                    cardNumber = "9999999999999999"
                    cvv = "111"
                    expiresAt = "12/30"
                    amount = 50f
                }
                shouldThrow<CardNotFoundException> {
                    service.processPayment(request)
                }
            }
        }
    }

    given("insufficient funds") {
        every {
            cardRepository.findByNumberAndCvvAndExpiresAt("1111222233334444", "111", "12/30")
        } returns Optional.of(card)
        card.balance = BigDecimal("10.00")

        `when`("processPayment is called with amount > balance") {
            then("InsufficientFundsException is thrown") {
                val request = PaymentRequest().apply {
                    cardNumber = "1111222233334444"
                    cvv = "111"
                    expiresAt = "12/30"
                    amount = 50f
                }
                shouldThrow<InsufficientFundsException> {
                    service.processPayment(request)
                }
            }
        }
    }

    given("card exists with sufficient balance") {
        every {
            cardRepository.findByNumberAndCvvAndExpiresAt("1111222233334444", "111", "12/30")
        } returns Optional.of(card)
        every { cardRepository.save(any()) } answers { firstArg() }

        `when`("processPayment is called") {
            val request = PaymentRequest().apply {
                cardNumber = "1111222233334444"
                cvv = "111"
                expiresAt = "12/30"
                amount = 100f
            }
            val result = service.processPayment(request)

            then("payment is approved and balance updated") {
                result.approved shouldBe true
                result.message shouldBe "Payment approved"
                result.remainingBalance shouldBe 900f
                verify(exactly = 1) { cardRepository.save(any<Card>()); Unit }
            }
        }
    }
})
