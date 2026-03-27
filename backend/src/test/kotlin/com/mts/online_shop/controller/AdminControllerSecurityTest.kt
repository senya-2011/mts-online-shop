package com.mts.online_shop.controller

import com.fasterxml.jackson.databind.ObjectMapper
import com.mts.online_shop.security.JwtService
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureWebMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.MediaType
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.get
import org.springframework.test.web.servlet.post

@SpringBootTest
@AutoConfigureWebMvc
@ActiveProfiles("test")
class AdminControllerSecurityTest {

    @Autowired
    private lateinit var mockMvc: MockMvc

    @Autowired
    private lateinit var jwtService: JwtService

    @Autowired
    private lateinit var objectMapper: ObjectMapper

    @Test
    fun `should allow admin to access all orders`() {
        val adminToken = jwtService.generateToken(
            null, 
            "admin", 
            setOf("ROLE_ADMIN", "ROLE_MANAGER")
        )

        mockMvc.get("/api/admin/orders") {
            header("Authorization", "Bearer $adminToken")
        }.andExpect {
            status { isOk() }
        }
    }

    @Test
    fun `should deny customer access to admin orders`() {
        val customerToken = jwtService.generateToken(
            null, 
            "customer1", 
            setOf("ROLE_CUSTOMER")
        )

        mockMvc.get("/api/admin/orders") {
            header("Authorization", "Bearer $customerToken")
        }.andExpect {
            status { isForbidden() }
        }
    }

    @Test
    fun `should allow manager to process orders`() {
        val managerToken = jwtService.generateToken(
            null, 
            "manager1", 
            setOf("ROLE_MANAGER")
        )

        mockMvc.put("/api/admin/orders/123/process") {
            header("Authorization", "Bearer $managerToken")
            contentType = MediaType.APPLICATION_JSON
        }.andExpect {
            status { isOk() }
        }
    }

    @Test
    fun `should deny customer to process orders`() {
        val customerToken = jwtService.generateToken(
            null, 
            "customer1", 
            setOf("ROLE_CUSTOMER")
        )

        mockMvc.put("/api/admin/orders/123/process") {
            header("Authorization", "Bearer $customerToken")
            contentType = MediaType.APPLICATION_JSON
        }.andExpect {
            status { isForbidden() }
        }
    }

    @Test
    fun `should allow admin to access system config`() {
        val adminToken = jwtService.generateToken(
            null, 
            "admin", 
            setOf("ROLE_ADMIN")
        )

        mockMvc.get("/api/admin/config") {
            header("Authorization", "Bearer $adminToken")
        }.andExpect {
            status { isOk() }
        }
    }

    @Test
    fun `should deny manager to access system config`() {
        val managerToken = jwtService.generateToken(
            null, 
            "manager1", 
            setOf("ROLE_MANAGER")
        )

        mockMvc.get("/api/admin/config") {
            header("Authorization", "Bearer $managerToken")
        }.andExpect {
            status { isForbidden() }
        }
    }

    @Test
    fun `should deny access without authentication`() {
        mockMvc.get("/api/admin/orders") {
        }.andExpect {
            status { isUnauthorized() }
        }

        mockMvc.put("/api/admin/orders/123/process") {
            contentType = MediaType.APPLICATION_JSON
        }.andExpect {
            status { isUnauthorized() }
        }

        mockMvc.get("/api/admin/config") {
        }.andExpect {
            status { isUnauthorized() }
        }
    }

    @Test
    fun `should allow admin to clear user cart`() {
        val adminToken = jwtService.generateToken(
            null, 
            "admin", 
            setOf("ROLE_ADMIN")
        )

        mockMvc.delete("/api/admin/cart/user/456") {
            header("Authorization", "Bearer $adminToken")
        }.andExpect {
            status { isOk() }
        }
    }

    @Test
    fun `should deny customer to clear other user cart`() {
        val customerToken = jwtService.generateToken(
            null, 
            "customer1", 
            setOf("ROLE_CUSTOMER")
        )

        mockMvc.delete("/api/admin/cart/user/456") {
            header("Authorization", "Bearer $customerToken")
        }.andExpect {
            status { isForbidden() }
        }
    }
}
