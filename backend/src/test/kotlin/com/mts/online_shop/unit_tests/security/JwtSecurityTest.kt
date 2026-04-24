package com.mts.online_shop.unit_tests.security

import com.mts.online_shop.security.JwtService
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.IsolationMode
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe

class JwtSecurityTest : BehaviorSpec({

    isolationMode = IsolationMode.InstancePerLeaf

    val validSecret = "12345678901234567890123456789012"
    val jwtService = JwtService(validSecret, 60_000L)

    given("JWT token generation") {
        `when`("token is generated for admin user") {
            val token = jwtService.generateToken(1L, "admin", setOf("ROLE_ADMIN"))
            val userId = jwtService.extractUserId(token)

            then("admin user id is extracted from token") {
                userId.get() shouldBe 1L
            }
        }

        `when`("token is generated for regular user") {
            val token = jwtService.generateToken(2L, "user1", setOf("ROLE_USER"))
            val userId = jwtService.extractUserId(token)

            then("regular user id is extracted from token") {
                userId.get() shouldBe 2L
            }
        }

        `when`("token is generated with multiple roles") {
            val token = jwtService.generateToken(1L, "admin", setOf("ROLE_ADMIN", "ROLE_USER"))
            val roles = jwtService.extractRoles(token)

            then("all roles are extracted from token") {
                roles.isPresent shouldBe true
                roles.get() shouldBe setOf("ROLE_ADMIN", "ROLE_USER")
            }
        }
    }

    given("JWT token validation") {
        `when`("token is valid") {
            val token = jwtService.generateToken(1L, "admin", setOf("ROLE_ADMIN"))
            val isValid: Boolean = jwtService.isTokenValid(token)

            then("token is valid") {
                isValid shouldBe true
            }
        }

        `when`("token is invalid") {
            val isValid: Boolean = jwtService.isTokenValid("invalid-token")

            then("token is invalid") {
                isValid shouldBe false
            }
        }

        `when`("token is empty") {
            val isValid: Boolean = jwtService.isTokenValid("")

            then("token is invalid") {
                isValid shouldBe false
            }
        }
    }

    given("JWT role checking") {
        `when`("user has required role") {
            val token = jwtService.generateToken(1L, "admin", setOf("ROLE_ADMIN"))
            val hasRole = jwtService.hasRole(token, "ADMIN")

            then("role check returns true") {
                hasRole shouldBe true
            }
        }

        `when`("user does not have required role") {
            val token = jwtService.generateToken(2L, "user1", setOf("ROLE_USER"))
            val hasRole = jwtService.hasRole(token, "ADMIN")

            then("role check returns false") {
                hasRole shouldBe false
            }
        }

        `when`("checking any of multiple roles") {
            val token = jwtService.generateToken(1L, "admin", setOf("ROLE_ADMIN", "ROLE_USER"))
            val hasAnyRole = jwtService.hasAnyRole(token, "ADMIN", "MANAGER")

            then("returns true if user has at least one role") {
                hasAnyRole shouldBe true
            }
        }
    }

    given("JWT token expiration") {
        `when`("token is generated with expiration") {
            val token = jwtService.generateToken(1L, "admin", setOf("ADMIN"))
            val userId = jwtService.extractUserId(token)

            then("user id is extracted before expiration") {
                userId.get() shouldBe 1L
            }
        }
    }
})
