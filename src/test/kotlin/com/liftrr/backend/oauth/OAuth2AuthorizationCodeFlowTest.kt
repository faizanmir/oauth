package com.liftrr.backend.oauth

import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

@SpringBootTest
@AutoConfigureMockMvc
class OAuth2AuthorizationCodeFlowTest {

    @Autowired
    private lateinit var mockMvc: MockMvc

    @Test
    fun `should return 401 when accessing authorization endpoint without authentication`() {
        mockMvc.perform(
            get("/oauth2/authorize")
                .param("response_type", "code")
                .param("client_id", "liftrr-web-client")
                .param("redirect_uri", "http://localhost:3000/authorized")
                .param("scope", "read write")
        )
            .andExpect(status().isUnauthorized)
    }

    @Test
    fun `should successfully obtain token using client credentials grant`() {
        mockMvc.perform(
            post("/oauth2/token")
                .with(httpBasic("liftrr-service-client", "service-secret"))
                .param("grant_type", "client_credentials")
                .param("scope", "service.read")
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.access_token").exists())
            .andExpect(jsonPath("$.token_type").value("Bearer"))
            .andExpect(jsonPath("$.expires_in").exists())
    }

    @Test
    fun `should fail token request with invalid client credentials`() {
        mockMvc.perform(
            post("/oauth2/token")
                .with(httpBasic("liftrr-service-client", "wrong-secret"))
                .param("grant_type", "client_credentials")
                .param("scope", "service.read")
        )
            .andExpect(status().isUnauthorized)
    }

    @Test
    fun `should successfully obtain token using password grant`() {
        mockMvc.perform(
            post("/oauth2/token")
                .with(httpBasic("liftrr-mobile-client", "mobile-secret"))
                .param("grant_type", "password")
                .param("username", "user")
                .param("password", "user123")
                .param("scope", "read write")
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.access_token").exists())
            .andExpect(jsonPath("$.refresh_token").exists())
            .andExpect(jsonPath("$.token_type").value("Bearer"))
    }

    @Test
    fun `should fail password grant with invalid user credentials`() {
        mockMvc.perform(
            post("/oauth2/token")
                .with(httpBasic("liftrr-mobile-client", "mobile-secret"))
                .param("grant_type", "password")
                .param("username", "user")
                .param("password", "wrongpassword")
                .param("scope", "read write")
        )
            .andExpect(status().isBadRequest)
    }
}
