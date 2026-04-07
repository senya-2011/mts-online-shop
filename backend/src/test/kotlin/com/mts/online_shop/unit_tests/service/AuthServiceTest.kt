package com.mts.online_shop.unit_tests.service

import com.mts.online_shop.exception.BadRequestException
import com.mts.online_shop.exception.InvalidCredentialsException
import com.mts.online_shop.exception.UserAlreadyExistsException
import com.mts.online_shop.model.User
import com.mts.online_shop.repository.UserRepository
import com.mts.online_shop.service.AuthService
import com.mts.online_shop.security.JwtService
import com.mts.online_shop.security.XmlUserDetailsService
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import org.springframework.security.crypto.password.PasswordEncoder

class AuthServiceTest : DescribeSpec({

    val passwordEncoder = mockk<PasswordEncoder>()
    val xmlUserDetailsService = mockk<XmlUserDetailsService>()
    val jwtService = mockk<JwtService>()
    val userRepository = mockk<UserRepository>()
    val service = AuthService(passwordEncoder, xmlUserDetailsService, jwtService, userRepository)

    describe("authenticate") {
        
        context("when credentials are correct") {
            every { xmlUserDetailsService.loadUserByUsername("User_1") } returns mockk {
                every { username } returns "user_1"
                every { password } returns "encodedPassword"
                every { authorities } returns listOf(mockk {
                    every { authority } returns "ROLE_USER"
                })
            }
            every { xmlUserDetailsService.getUserIdByUsername("User_1") } returns 1L
            every { passwordEncoder.matches("StrongPass123", "encodedPassword") } returns true
            every { jwtService.generateToken(1L, "User_1", setOf("ROLE_USER"), any()) } returns "test-token"

            it("should return token") {
                val result = service.authenticate("User_1", "StrongPass123")
                result shouldBe "test-token"
            }
        }
        
        context("when credentials are incorrect") {
            every { xmlUserDetailsService.loadUserByUsername("user_1") } returns mockk {
                every { password } returns "encodedPassword"
            }
            every { passwordEncoder.matches("WrongPass123", "encodedPassword") } returns false

            it("should throw InvalidCredentialsException") {
                shouldThrow<InvalidCredentialsException> {
                    service.authenticate("user_1", "WrongPass123")
                }
            }
        }
    }

    describe("register") {
        
        context("when user already exists") {
            every { xmlUserDetailsService.userExists("new_user") } returns true

            it("should throw UserAlreadyExistsException") {
                shouldThrow<UserAlreadyExistsException> {
                    service.register("new_user", "new@mail.ru", "StrongPass123", "Name")
                }
            }
        }
        
        context("when email has no at-sign") {
            every { xmlUserDetailsService.userExists("new_user") } returns false

            it("should throw BadRequestException") {
                shouldThrow<BadRequestException> {
                    service.register("new_user", "invalidemail", "StrongPass123", "Name")
                }
            }
        }
        
        context("when email has no dot in domain") {
            every { xmlUserDetailsService.userExists("new_user") } returns false

            it("should throw BadRequestException") {
                shouldThrow<BadRequestException> {
                    service.register("new_user", "user@nodot", "StrongPass123", "Name")
                }
            }
        }
    }
})
