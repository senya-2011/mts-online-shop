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
import io.kotest.core.spec.IsolationMode
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.security.authentication.AuthenticationManager
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.Authentication
import org.springframework.security.core.GrantedAuthority
import org.springframework.security.core.authority.SimpleGrantedAuthority
import java.util.Optional

class AuthServiceTest : BehaviorSpec({

    isolationMode = IsolationMode.InstancePerLeaf

    val passwordEncoder = mockk<PasswordEncoder>()
    val xmlUserDetailsService = mockk<XmlUserDetailsService>()
    val jwtService = mockk<JwtService>()
    val userRepository = mockk<UserRepository>()
    val service = AuthService(passwordEncoder, xmlUserDetailsService, jwtService, userRepository)

    given("credentials are correct") {
        val authorities: MutableList<GrantedAuthority> = mutableListOf(SimpleGrantedAuthority("ROLE_USER"))
        val userDetails = mockk<org.springframework.security.core.userdetails.UserDetails>()
        every { userDetails.username } returns "user_1"
        every { userDetails.password } returns "encodedPassword"
        every { userDetails.authorities } returns authorities
        every { xmlUserDetailsService.loadUserByUsername("user_1") } returns userDetails
        every { xmlUserDetailsService.getUserIdByUsername("user_1") } returns 1L
        every { passwordEncoder.matches("password", "encodedPassword") } returns true
        every { jwtService.generateToken(1L, "user_1", setOf("ROLE_USER"), any()) } returns "test-token"

        `when`("authenticate is called") {
            val result = service.authenticate("User_1", "StrongPass123")

            then("token is returned") {
                result shouldBe "test-token"
            }
        }
    }

    given("authentication fails") {
        every { xmlUserDetailsService.loadUserByUsername("user_1") } returns mockk<org.springframework.security.core.userdetails.UserDetails>()
        every { passwordEncoder.matches("WrongPass123", any()) } returns false

        `when`("authenticate is called") {
            then("InvalidCredentialsException is thrown") {
                shouldThrow<InvalidCredentialsException> {
                    service.authenticate("user_1", "WrongPass123")
                }
            }
        }
    }

    given("registration is called") {
        `when`("register is called") {
            then("BadRequestException is thrown") {
                shouldThrow<BadRequestException> {
                    service.register("new_user", "new@mail.ru", "StrongPass123", "Name")
                }
            }
        }
    }

    given("email has no at-sign") {
        `when`("register is called") {
            then("BadRequestException is thrown") {
                shouldThrow<BadRequestException> {
                    service.register("new_user", "invalidemail", "StrongPass123", "Name")
                }
            }
        }
    }

    given("email has no dot in domain") {
        `when`("register is called") {
            then("BadRequestException is thrown") {
                shouldThrow<BadRequestException> {
                    service.register("new_user", "user@nodot", "StrongPass123", "Name")
                }
            }
        }
    }
})
