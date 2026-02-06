package com.liftrr.backend.security.service

import org.springframework.security.core.Authentication
import org.springframework.security.oauth2.jwt.JwtClaimsSet
import org.springframework.security.oauth2.jwt.JwtEncoder
import org.springframework.security.oauth2.jwt.JwtEncoderParameters
import org.springframework.stereotype.Service
import java.time.Instant
import java.time.temporal.ChronoUnit

/**
 * Service for generating JWT tokens for web-authenticated users
 * Used when mobile app needs tokens after web login
 */
@Service
class TokenService(
    private val jwtEncoder: JwtEncoder
) {

    /**
     * Generate access and refresh tokens for an authenticated user
     */
    fun generateTokens(authentication: Authentication, username: String): TokenPair {
        val now = Instant.now()

        // Access token (1 hour)
        val accessTokenClaims = JwtClaimsSet.builder()
            .issuer("https://web-production-2b788.up.railway.app")
            .issuedAt(now)
            .expiresAt(now.plus(1, ChronoUnit.HOURS))
            .subject(username)
            .claim("scope", authentication.authorities.joinToString(" ") { it.authority })
            .build()

        val accessToken = jwtEncoder.encode(JwtEncoderParameters.from(accessTokenClaims)).tokenValue

        // Refresh token (24 hours)
        val refreshTokenClaims = JwtClaimsSet.builder()
            .issuer("https://web-production-2b788.up.railway.app")
            .issuedAt(now)
            .expiresAt(now.plus(24, ChronoUnit.HOURS))
            .subject(username)
            .claim("scope", "refresh_token")
            .build()

        val refreshToken = jwtEncoder.encode(JwtEncoderParameters.from(refreshTokenClaims)).tokenValue

        return TokenPair(
            accessToken = accessToken,
            refreshToken = refreshToken,
            expiresIn = 3600, // 1 hour in seconds
            tokenType = "Bearer"
        )
    }
}

/**
 * Token pair returned after authentication
 */
data class TokenPair(
    val accessToken: String,
    val refreshToken: String,
    val expiresIn: Long,
    val tokenType: String
)
