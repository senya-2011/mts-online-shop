package com.mts.online_shop.unit_tests.controller

import com.mts.online_shop.security.JwtService
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.IsolationMode
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

class AdminControllerTest : BehaviorSpec({

    isolationMode = IsolationMode.InstancePerLeaf

    val validSecret = "12345678901234567890123456789012"
    val jwtService = JwtService(validSecret, 60_000L)

    given("Admin user authentication") {
        `when`("admin token is generated") {
            val token = jwtService.generateToken(1L, "admin", setOf("ROLE_ADMIN"))
            
            then("token is not empty") {
                token shouldNotBe null
                token.isNotBlank() shouldBe true
            }
        }

        `when`("admin token is validated") {
            val token = jwtService.generateToken(1L, "admin", setOf("ROLE_ADMIN"))
            val isValid: Boolean = jwtService.isTokenValid(token)
            
            then("token is valid") {
                isValid shouldBe true
            }
        }

        `when`("admin role is checked") {
            val token = jwtService.generateToken(1L, "admin", setOf("ROLE_ADMIN"))
            val hasRole = jwtService.hasRole(token, "ADMIN")
            
            then("admin has ADMIN role") {
                hasRole shouldBe true
            }
        }
    }

    given("Regular user authentication") {
        `when`("regular user token is generated") {
            val token = jwtService.generateToken(2L, "user1", setOf("ROLE_USER"))
            
            then("token is not empty") {
                token shouldNotBe null
                token.isNotBlank() shouldBe true
            }
        }

        `when`("regular user role is checked for ADMIN") {
            val token = jwtService.generateToken(2L, "user1", setOf("ROLE_USER"))
            val hasRole = jwtService.hasRole(token, "ADMIN")
            
            then("regular user does not have ADMIN role") {
                hasRole shouldBe false
            }
        }

        `when`("regular user role is checked for USER") {
            val token = jwtService.generateToken(2L, "user1", setOf("ROLE_USER"))
            val hasRole = jwtService.hasRole(token, "USER")
            
            then("regular user has USER role") {
                hasRole shouldBe true
            }
        }
    }

    given("Access control validation") {
        `when`("admin tries to access admin resources") {
            val token = jwtService.generateToken(1L, "admin", setOf("ROLE_ADMIN"))
            val hasRole = jwtService.hasRole(token, "ADMIN")
            
            then("access is granted") {
                hasRole shouldBe true
            }
        }

        `when`("regular user tries to access admin resources") {
            val token = jwtService.generateToken(2L, "user1", setOf("ROLE_USER"))
            val hasRole = jwtService.hasRole(token, "ADMIN")
            
            then("access is denied") {
                hasRole shouldBe false
            }
        }
    }

    given("Token generation with invalid data") {
        `when`("user id is null") {
            then("IllegalArgumentException is thrown") {
                shouldThrow<IllegalArgumentException> {
                    jwtService.generateToken(null, "admin", setOf("ADMIN"))
                }
            }
        }

        `when`("username is null") {
            then("IllegalArgumentException is thrown") {
                shouldThrow<IllegalArgumentException> {
                    jwtService.generateToken(1L, null, setOf("ADMIN"))
                }
            }
        }

        `when`("roles are empty") {
            val token = jwtService.generateToken(1L, "admin", emptySet())
            val roles = jwtService.extractRoles(token)
            
            then("empty roles are extracted") {
                roles.isPresent shouldBe true
                roles.get().isEmpty() shouldBe true
            }
        }
    }

    given("Multi-role authentication") {
        `when`("user has multiple roles") {
            val token = jwtService.generateToken(1L, "admin", setOf("ROLE_ADMIN", "ROLE_USER"))
            val roles = jwtService.extractRoles(token)
            
            then("all roles are present") {
                roles.isPresent shouldBe true
                roles.get().size shouldBe 2
                roles.get().contains("ROLE_ADMIN") shouldBe true
                roles.get().contains("ROLE_USER") shouldBe true
            }
        }

        `when`("checking any of multiple roles") {
            val token = jwtService.generateToken(1L, "admin", setOf("ROLE_ADMIN", "ROLE_USER"))
            val hasAnyRole = jwtService.hasAnyRole(token, "ADMIN", "MANAGER")
            
            then("returns true if user has at least one role") {
                hasAnyRole shouldBe true
            }
        }

        `when`("checking all required roles") {
            val token = jwtService.generateToken(1L, "admin", setOf("ROLE_ADMIN", "ROLE_USER"))
            val roles = jwtService.extractRoles(token)
            
            then("returns true if user has all required roles") {
                roles.isPresent shouldBe true
                roles.get().contains("ROLE_ADMIN") shouldBe true
                roles.get().contains("ROLE_USER") shouldBe true
            }
        }
    }
})
