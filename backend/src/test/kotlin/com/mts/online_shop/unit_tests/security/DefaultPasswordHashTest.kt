package com.mts.online_shop.unit_tests.security

import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder

class DefaultPasswordHashTest {
    private val encoder = BCryptPasswordEncoder()

    @Test
    fun `template hashes match default passwords`() {
        val adminHash = "\$2b\$10\$obg6MpawLg3be8haz3aPZ.U8IEK2Pj5F8hlhyuQ/BMfdzSoLlVm5a"
        val userHash = "\$2b\$10\$yp.KEmY35HLhrSoK0uX2bOHKj6lcLOOtaYwF17MG0T33uNdT0.qZG"
        assertTrue(encoder.matches("admin", adminHash))
        assertTrue(encoder.matches("user", userHash))
    }
}
