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
import org.mockito.Mockito.*
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import org.mockito.kotlin.any
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.security.core.userdetails.UserDetails

class AuthServiceTest : DescribeSpec({

    val passwordEncoder = mock<PasswordEncoder>()
    val xmlUserDetailsService = mock<XmlUserDetailsService>()
    val jwtService = mock<JwtService>()
    val userRepository = mock<UserRepository>()
    val service = AuthService(passwordEncoder, xmlUserDetailsService, jwtService, userRepository)

    describe("authenticate") {
        
        context("when credentials are correct") {
            val userDetails = mock<UserDetails>()
            val authority = mock<org.springframework.security.core.GrantedAuthority>()
            
            whenever(userDetails.username).thenReturn("user_1")
            whenever(userDetails.password).thenReturn("encodedPassword")
            whenever(userDetails.authorities).thenReturn(listOf(authority))
            whenever(authority.authority).thenReturn("ROLE_USER")
            
            whenever(xmlUserDetailsService.loadUserByUsername("User_1")).thenReturn(userDetails)
            whenever(xmlUserDetailsService.getUserIdByUsername("User_1")).thenReturn(1L)
            whenever(passwordEncoder.matches("StrongPass123", "encodedPassword")).thenReturn(true)
            whenever(jwtService.generateToken(1L, "User_1", setOf("ROLE_USER"), any())).thenReturn("test-token")

            it("should return token") {
                val result = service.authenticate("User_1", "StrongPass123")
                result shouldBe "test-token"
            }
        }
        
        context("when credentials are incorrect") {
            val userDetails = mock<UserDetails>()
            whenever(userDetails.password).thenReturn("encodedPassword")
            
            whenever(xmlUserDetailsService.loadUserByUsername("user_1")).thenReturn(userDetails)
            whenever(passwordEncoder.matches("WrongPass123", "encodedPassword")).thenReturn(false)

            it("should throw InvalidCredentialsException") {
                shouldThrow<InvalidCredentialsException> {
                    service.authenticate("user_1", "WrongPass123")
                }
            }
        }
    }

    describe("register") {
        
        context("when user already exists") {
            whenever(xmlUserDetailsService.userExists("new_user")).thenReturn(true)

            it("should throw UserAlreadyExistsException") {
                shouldThrow<UserAlreadyExistsException> {
                    service.register("new_user", "new@mail.ru", "StrongPass123", "Name")
                }
            }
        }
        
        context("when email has no at-sign") {
            whenever(xmlUserDetailsService.userExists("new_user")).thenReturn(false)

            it("should throw BadRequestException") {
                shouldThrow<BadRequestException> {
                    service.register("new_user", "invalidemail", "StrongPass123", "Name")
                }
            }
        }
        
        context("when email has no dot in domain") {
            whenever(xmlUserDetailsService.userExists("new_user")).thenReturn(false)

            it("should throw BadRequestException") {
                shouldThrow<BadRequestException> {
                    service.register("new_user", "user@nodot", "StrongPass123", "Name")
                }
            }
        }
    }
})
