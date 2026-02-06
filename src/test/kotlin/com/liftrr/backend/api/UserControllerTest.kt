package com.liftrr.backend.api

import com.liftrr.backend.domain.repository.UserRepository
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.security.test.context.support.WithMockUser
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

@SpringBootTest
@AutoConfigureMockMvc
class UserControllerTest {

    @Autowired
    private lateinit var mockMvc: MockMvc

    @Autowired
    private lateinit var userRepository: UserRepository

    @Test
    fun `should return 401 when accessing protected endpoint without authentication`() {
        mockMvc.perform(get("/api/user/me"))
            .andExpect(status().isUnauthorized)
    }

    @Test
    @WithMockUser(username = "user", roles = ["USER"])
    fun `should return current user info when authenticated`() {
        mockMvc.perform(get("/api/user/me"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.username").value("user"))
            .andExpect(jsonPath("$.email").exists())
            .andExpect(jsonPath("$.roles").isArray)
    }

    @Test
    @WithMockUser(username = "user", roles = ["USER"])
    fun `should return user profile when authenticated`() {
        mockMvc.perform(get("/api/user/profile"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.username").value("user"))
            .andExpect(jsonPath("$.email").exists())
            .andExpect(jsonPath("$.enabled").value(true))
            .andExpect(jsonPath("$.createdAt").exists())
    }

    @Test
    @WithMockUser(username = "admin", roles = ["ADMIN", "USER"])
    fun `should return admin user info when authenticated as admin`() {
        mockMvc.perform(get("/api/user/me"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.username").value("admin"))
            .andExpect(jsonPath("$.roles").isArray)
    }
}
