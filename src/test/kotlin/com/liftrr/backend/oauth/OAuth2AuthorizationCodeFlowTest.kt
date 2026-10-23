package com.liftrr.backend.oauth

import com.jayway.jsonpath.JsonPath
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.mock.web.MockHttpServletResponse
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.web.util.UriComponentsBuilder

@SpringBootTest
@AutoConfigureMockMvc
class OAuth2AuthorizationCodeFlowTest {

    companion object {
        private const val CLIENT_ID = "liftrr-mobile-client"
        private const val CLIENT_SECRET = "mobile-secret"
        private const val REDIRECT_URI = "https://web-production-2b788.up.railway.app/oauth2/callback"
        private const val CODE_VERIFIER = "dBjftJeZ4CVP-mB92K27uhbUJU1p1r_wW1gFWFOEjXk"
        private const val CODE_CHALLENGE = "E9Melhoa2OwvFrEMTJguCHaoeK1t8URWbuGJSstw-cM"
        private const val CLIENT_STATE = "xyz"
        private val CONSENT_STATE_REGEX = Regex("""name="state" value="([^"]+)"""")
    }

    @Autowired
    private lateinit var mockMvc: MockMvc

    private val principal = user("user").roles("USER")

    private fun authorize(approve: Boolean): MockHttpServletResponse {
        val consentPage = mockMvc.perform(
            get("/oauth2/authorize")
                .with(principal)
                .param("response_type", "code")
                .param("client_id", CLIENT_ID)
                .param("redirect_uri", REDIRECT_URI)
                .param("scope", "openid profile read write")
                .param("state", CLIENT_STATE)
                .param("code_challenge", CODE_CHALLENGE)
                .param("code_challenge_method", "S256")
        )
            .andExpect(status().isOk)
            .andReturn().response.contentAsString

        val consentState = CONSENT_STATE_REGEX.find(consentPage)?.groupValues?.get(1)
        assertNotNull(consentState, "consent page should contain a state field")

        val consent = post("/oauth2/authorize")
            .with(principal)
            .param("client_id", CLIENT_ID)
            .param("state", consentState)
        if (approve) {
            consent.param("scope", "profile", "read", "write")
        }

        return mockMvc.perform(consent)
            .andExpect(status().is3xxRedirection)
            .andReturn().response
    }

    private fun obtainCode(): String {
        val location = authorize(approve = true).redirectedUrl!!
        assertTrue(location.startsWith(REDIRECT_URI), "unexpected redirect: $location")
        val params = UriComponentsBuilder.fromUriString(location).build().queryParams
        assertTrue(params.getFirst("state") == CLIENT_STATE, "state not echoed: $location")
        return params.getFirst("code") ?: error("no code in redirect: $location")
    }

    private fun exchange(code: String, codeVerifier: String? = CODE_VERIFIER) =
        post("/oauth2/token")
            .with(httpBasic(CLIENT_ID, CLIENT_SECRET))
            .param("grant_type", "authorization_code")
            .param("code", code)
            .param("redirect_uri", REDIRECT_URI)
            .apply { if (codeVerifier != null) param("code_verifier", codeVerifier) }

    @Test
    fun `full authorization code flow with pkce reaches api and refreshes`() {
        val location = authorize(approve = true).redirectedUrl!!
        assertTrue(location.startsWith(REDIRECT_URI), "unexpected redirect: $location")
        assertTrue(location.contains("state=$CLIENT_STATE"), "state not echoed: $location")
        assertTrue(location.contains("code="), "no code in redirect: $location")
        val code = UriComponentsBuilder.fromUriString(location).build().queryParams.getFirst("code")!!

        val tokenJson = mockMvc.perform(exchange(code))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.access_token").exists())
            .andExpect(jsonPath("$.refresh_token").exists())
            .andReturn().response.contentAsString
        val accessToken: String = JsonPath.read(tokenJson, "$.access_token")
        val refreshToken: String = JsonPath.read(tokenJson, "$.refresh_token")

        mockMvc.perform(
            get("/api/user/me").header("Authorization", "Bearer $accessToken")
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.username").value("user"))

        mockMvc.perform(
            post("/oauth2/token")
                .with(httpBasic(CLIENT_ID, CLIENT_SECRET))
                .param("grant_type", "refresh_token")
                .param("refresh_token", refreshToken)
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.access_token").exists())
    }

    @Test
    fun `token exchange without code_verifier fails`() {
        mockMvc.perform(exchange(obtainCode(), codeVerifier = null))
            .andExpect(status().isBadRequest)
    }

    @Test
    fun `authorization code cannot be reused`() {
        val code = obtainCode()

        mockMvc.perform(exchange(code))
            .andExpect(status().isOk)

        mockMvc.perform(exchange(code))
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.error").value("invalid_grant"))
    }

    @Test
    fun `denied consent redirects with access_denied`() {
        val location = authorize(approve = false).redirectedUrl!!
        assertTrue(location.contains("error=access_denied"), "unexpected redirect: $location")
        assertTrue(location.contains("state=$CLIENT_STATE"), "state not echoed: $location")
    }

    @Test
    fun `password grant is rejected`() {
        mockMvc.perform(
            post("/oauth2/token")
                .with(httpBasic(CLIENT_ID, CLIENT_SECRET))
                .param("grant_type", "password")
                .param("username", "user")
                .param("password", "user123")
                .param("scope", "read write")
        )
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.error").value("unsupported_grant_type"))
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
}
