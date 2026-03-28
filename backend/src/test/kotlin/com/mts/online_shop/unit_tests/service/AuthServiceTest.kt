package com.mts.online_shop.unit_tests.service

import com.mts.online_shop.exception.BadRequestException
import com.mts.online_shop.exception.InvalidCredentialsException
import com.mts.online_shop.exception.UserAlreadyExistsException
import com.mts.online_shop.model.User
import com.mts.online_shop.repository.UserRepository
import com.mts.online_shop.service.AuthService
import com.mts.online_shop.security.JwtService
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
    val authenticationManager = mockk<AuthenticationManager>()
    val jwtService = mockk<JwtService>()
    val service = AuthService(passwordEncoder, authenticationManager, jwtService)

    given("credentials are correct") {
        val authorities: MutableList<GrantedAuthority> = mutableListOf(SimpleGrantedAuthority("ROLE_USER"))
        val authentication = mockk<Authentication>()
        every { authentication.name } returns "user_1"
        every { authentication.authorities } returns authorities
        every { authenticationManager.authenticate(any()) } returns authentication
        every { jwtService.generateToken(null, "user_1", setOf("ROLE_USER"), any()) } returns "test-token"

        `when`("authenticate is called") {
            val result = service.authenticate("User_1", "StrongPass123")

            then("token is returned") {
                result shouldBe "test-token"
            }
        }
    }

    given("authentication fails") {
        every { authenticationManager.authenticate(any()) } throws InvalidCredentialsException("Invalid credentials")

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
})
