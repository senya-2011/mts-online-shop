package com.mts.online_shop.unit_tests.security

import com.mts.online_shop.exception.UnauthorizedException
import com.mts.online_shop.security.CurrentUserService
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.IsolationMode
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.context.SecurityContextHolder

class CurrentUserServiceTest : BehaviorSpec({

    isolationMode = IsolationMode.InstancePerLeaf

    val service = CurrentUserService()

    afterEach {
        SecurityContextHolder.clearContext()
    }

    given("authentication principal is Long") {
        SecurityContextHolder.getContext().authentication =
            UsernamePasswordAuthenticationToken.authenticated(5L, null, emptyList())

        `when`("getCurrentUserId is called") {
            then("user id is returned") {
                service.getCurrentUserId().get() shouldBe 5L
            }
        }
    }

    given("authentication principal is numeric String") {
        SecurityContextHolder.getContext().authentication =
            UsernamePasswordAuthenticationToken.authenticated("7", null, emptyList())

        `when`("getCurrentUserId is called") {
            then("parsed user id is returned") {
                service.getCurrentUserId().get() shouldBe 7L
            }
        }
    }

    given("authentication principal is invalid String") {
        SecurityContextHolder.getContext().authentication =
            UsernamePasswordAuthenticationToken.authenticated("abc", null, emptyList())

        `when`("getCurrentUserId is called") {
            then("empty optional is returned") {
                service.getCurrentUserId().isEmpty shouldBe true
            }
        }
    }

    given("authentication is absent") {
        SecurityContextHolder.clearContext()

        `when`("getCurrentUserIdOrThrow is called") {
            then("UnauthorizedException is thrown") {
                shouldThrow<UnauthorizedException> {
                    service.getCurrentUserIdOrThrow()
                }
            }
        }
    }
})
