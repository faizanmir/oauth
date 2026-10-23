package com.liftrr.backend.security.config

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.security.core.Authentication
import org.springframework.security.oauth2.server.authorization.OAuth2TokenType
import org.springframework.security.oauth2.server.authorization.token.JwtEncodingContext
import org.springframework.security.oauth2.server.authorization.token.OAuth2TokenCustomizer

@Configuration
class TokenCustomizerConfig {

    @Bean
    fun accessTokenRolesCustomizer(): OAuth2TokenCustomizer<JwtEncodingContext> =
        OAuth2TokenCustomizer { context ->
            if (context.tokenType == OAuth2TokenType.ACCESS_TOKEN) {
                val roles = context.getPrincipal<Authentication>()
                    .authorities
                    .map { it.authority }
                    .filter { it.startsWith("ROLE_") }
                context.claims.claim("roles", roles)
            }
        }
}
