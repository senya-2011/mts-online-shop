package com.mts.online_shop.unit_tests.security

import com.mts.online_shop.security.JwtService
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.IsolationMode
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe

class JwtServiceTest : BehaviorSpec({

    isolationMode = IsolationMode.InstancePerLeaf

    val validSecret = "12345678901234567890123456789012"

    given("valid jwt service") {
        val jwtService = JwtService(validSecret, 60_000L)

        `when`("token is generated and parsed") {
            val token = jwtService.generateToken(42L)
            val userId = jwtService.extractUserId(token)

            then("user id is extracted from token") {
                userId.get() shouldBe 42L
            }
        }

        `when`("token is invalid") {
            then("empty optional is returned") {
                jwtService.extractUserId("not-a-jwt").isEmpty shouldBe true
            }
        }

        `when`("user id is null") {
            then("IllegalArgumentException is thrown") {
                shouldThrow<IllegalArgumentException> {
                    jwtService.generateToken(null)
                }
            }
        }
    }

    given("secret is too short") {
        `when`("service is created") {
            then("IllegalStateException is thrown") {
                shouldThrow<IllegalStateException> {
                    JwtService("short-secret", 60_000L)
                }
            }
        }
    }
})
