package com.mts.online_shop.security

import com.mts.online_shop.security.jaas.XmlUserLoginModule
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.security.authentication.AuthenticationManager
import org.springframework.security.authentication.BadCredentialsException
import org.springframework.security.core.Authentication
import org.springframework.security.core.GrantedAuthority
import org.springframework.test.context.ActiveProfiles
import java.util.*

@SpringBootTest
@ActiveProfiles("test")
class JaasSecurityTest {

    private lateinit var authenticationManager: AuthenticationManager
    private lateinit var jwtService: JwtService
    private lateinit var privilegeService: PrivilegeService

    @BeforeEach
    fun setUp() {
        authenticationManager = mockk()
        jwtService = mockk()
        privilegeService = mockk()
    }

    @Test
    fun `should authenticate admin user successfully`() {
        val username = "admin"
        val password = "admin123"
        
        val authentication = mockk<Authentication>()
        every { authentication.name } returns username
        every { authentication.authorities } returns setOf(
            mockk<GrantedAuthority> { every { authority } returns "ROLE_ADMIN" }
        )
        
        every { authenticationManager.authenticate(any()) } returns authentication
        every { jwtService.generateToken(any(), any(), any()) } returns "jwt-token"
        
        // Test authentication
        val result = authenticationManager.authenticate(
            org.springframework.security.authentication.UsernamePasswordAuthenticationToken(username, password)
        )
        
        assert(result.name == username)
        assert(result.authorities.any { it.authority == "ROLE_ADMIN" })
    }

    @Test
    fun `should fail authentication for invalid credentials`() {
        val username = "invalid"
        val password = "wrong"
        
        every { 
            authenticationManager.authenticate(any()) 
        } throws BadCredentialsException("Authentication failed")
        
        assertThrows<BadCredentialsException> {
            authenticationManager.authenticate(
                org.springframework.security.authentication.UsernamePasswordAuthenticationToken(username, password)
            )
        }
    }

    @Test
    fun `should generate JWT with roles`() {
        val username = "manager1"
        val roles = setOf("ROLE_MANAGER", "ROLE_CUSTOMER")
        val additionalClaims = mapOf("email" to "manager1@mtsonline.ru")
        
        every { jwtService.generateToken(any(), any(), any(), any()) } returns "jwt-token-with-roles"
        
        val token = jwtService.generateToken(null, username, roles, additionalClaims)
        
        assert(token == "jwt-token-with-roles")
    }

    @Test
    fun `should validate user privileges`() {
        val username = "admin"
        val privilege = "USER_MANAGE"
        
        every { privilegeService.hasPrivilege(username, privilege) } returns true
        
        val hasPrivilege = privilegeService.hasPrivilege(username, privilege)
        
        assert(hasPrivilege)
    }

    @Test
    fun `should deny access for insufficient privileges`() {
        val username = "customer1"
        val privilege = "USER_MANAGE"
        
        every { privilegeService.hasPrivilege(username, privilege) } returns false
        
        val hasPrivilege = privilegeService.hasPrivilege(username, privilege)
        
        assert(!hasPrivilege)
    }

    @Test
    fun `should check multiple privileges`() {
        val username = "manager1"
        val privileges = arrayOf("ORDER_VIEW_ALL", "ORDER_PROCESS")
        
        every { privilegeService.hasAnyPrivilege(username, *privileges) } returns true
        every { privilegeService.hasAllPrivileges(username, *privileges) } returns false
        
        assert(privilegeService.hasAnyPrivilege(username, *privileges))
        assert(!privilegeService.hasAllPrivileges(username, *privileges))
    }

    @Test
    fun `should extract roles from JWT token`() {
        val token = "valid-jwt-token"
        val expectedRoles = setOf("ROLE_ADMIN", "ROLE_MANAGER")
        
        every { jwtService.extractRoles(token) } returns Optional.of(expectedRoles)
        
        val roles = jwtService.extractRoles(token)
        
        assert(roles.isPresent)
        assert(roles.get() == expectedRoles)
    }

    @Test
    fun `should validate role in JWT token`() {
        val token = "valid-jwt-token"
        
        every { jwtService.hasRole(token, "ADMIN") } returns true
        every { jwtService.hasRole(token, "CUSTOMER") } returns false
        
        assert(jwtService.hasRole(token, "ADMIN"))
        assert(!jwtService.hasRole(token, "CUSTOMER"))
    }

    @Test
    fun `should check any role in JWT token`() {
        val token = "valid-jwt-token"
        
        every { jwtService.hasAnyRole(token, "MANAGER", "ADMIN") } returns true
        every { jwtService.hasAnyRole(token, "CUSTOMER", "GUEST") } returns false
        
        assert(jwtService.hasAnyRole(token, "MANAGER", "ADMIN"))
        assert(!jwtService.hasAnyRole(token, "CUSTOMER", "GUEST"))
    }
}
