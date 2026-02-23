package com.mts.online_shop.unit_tests.service

import com.mts.online_shop.exception.BadRequestException
import com.mts.online_shop.exception.InvalidCredentialsException
import com.mts.online_shop.exception.UserAlreadyExistsException
import com.mts.online_shop.model.User
import com.mts.online_shop.repository.UserRepository
import com.mts.online_shop.service.AuthService
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.IsolationMode
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.springframework.security.crypto.password.PasswordEncoder
import java.util.Optional

class AuthServiceTest : BehaviorSpec({

    isolationMode = IsolationMode.InstancePerLeaf

    val userRepository = mockk<UserRepository>()
    val passwordEncoder = mockk<PasswordEncoder>()
    val service = AuthService(userRepository, passwordEncoder)

    val existingUser = User().apply {
        id = 1L
        login = "user_1"
        email = "user@mail.ru"
        name = "User"
        passwordHash = "encoded"
    }

    given("credentials are correct") {
        every { userRepository.findByLoginIgnoreCaseOrEmailIgnoreCase("user_1", "user_1") } returns Optional.of(existingUser)
        every { passwordEncoder.matches("StrongPass123", "encoded") } returns true

        `when`("authenticate is called") {
            val result = service.authenticate("User_1", "StrongPass123")

            then("user id is returned") {
                result shouldBe 1L
            }
        }
    }

    given("password does not match") {
        every { userRepository.findByLoginIgnoreCaseOrEmailIgnoreCase("user_1", "user_1") } returns Optional.of(existingUser)
        every { passwordEncoder.matches("WrongPass123", "encoded") } returns false

        `when`("authenticate is called") {
            then("InvalidCredentialsException is thrown") {
                shouldThrow<InvalidCredentialsException> {
                    service.authenticate("user_1", "WrongPass123")
                }
            }
        }
    }

    given("login is invalid") {
        `when`("authenticate is called") {
            then("BadRequestException is thrown") {
                shouldThrow<BadRequestException> {
                    service.authenticate("xx", "StrongPass123")
                }
            }
        }
    }

    given("new user data is valid and unique") {
        every { userRepository.existsByLoginIgnoreCase("new_user") } returns false
        every { userRepository.existsByEmailIgnoreCase("new@mail.ru") } returns false
        every { passwordEncoder.encode("StrongPass123") } returns "encodedHash"
        val capturedUser = slot<User>()
        every { userRepository.save(capture(capturedUser)) } answers { firstArg() }

        `when`("register is called") {
            service.register("New_User", "new@mail.ru", "StrongPass123", "")

            then("user is saved in normalized form") {
                capturedUser.captured.login shouldBe "new_user"
                capturedUser.captured.email shouldBe "new@mail.ru"
                capturedUser.captured.name shouldBe "new_user"
                capturedUser.captured.passwordHash shouldBe "encodedHash"
                verify(exactly = 1) { userRepository.save(any()) }
            }
        }
    }

    given("login already exists") {
        every { userRepository.existsByLoginIgnoreCase("new_user") } returns true

        `when`("register is called") {
            then("UserAlreadyExistsException is thrown") {
                shouldThrow<UserAlreadyExistsException> {
                    service.register("new_user", "new@mail.ru", "StrongPass123", "Name")
                }
            }
        }
    }
})
