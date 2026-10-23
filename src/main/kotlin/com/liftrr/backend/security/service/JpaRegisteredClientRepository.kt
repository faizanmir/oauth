package com.liftrr.backend.security.service

import com.liftrr.backend.domain.repository.OAuth2ClientRepository
import org.springframework.security.oauth2.core.AuthorizationGrantType
import org.springframework.security.oauth2.core.ClientAuthenticationMethod
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient
import org.springframework.security.oauth2.server.authorization.client.RegisteredClientRepository
import org.springframework.security.oauth2.server.authorization.settings.ClientSettings
import org.springframework.security.oauth2.server.authorization.settings.TokenSettings
import org.springframework.stereotype.Service
import java.time.Duration

@Service
class JpaRegisteredClientRepository(
    private val oAuth2ClientRepository: OAuth2ClientRepository
) : RegisteredClientRepository {

    override fun save(registeredClient: RegisteredClient) {
        // Not implementing save for now - we'll manage clients via JPA directly
        throw UnsupportedOperationException("Saving clients via RegisteredClientRepository not supported")
    }

    override fun findById(id: String): RegisteredClient? {
        return oAuth2ClientRepository.findById(id.toLongOrNull() ?: return null)
            .map { mapToRegisteredClient(it) }
            .orElse(null)
    }

    override fun findByClientId(clientId: String): RegisteredClient? {
        return oAuth2ClientRepository.findByClientId(clientId)
            .map { mapToRegisteredClient(it) }
            .orElse(null)
    }

    private fun mapToRegisteredClient(client: com.liftrr.backend.domain.entity.OAuth2Client): RegisteredClient {
        val builder = RegisteredClient.withId(client.id.toString())
            .clientId(client.clientId)
            .clientSecret(client.clientSecret)
            .clientName(client.clientName)

        // Add authentication methods
        builder.clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_BASIC)
        builder.clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_POST)

        // Add grant types
        client.grantTypes.forEach { grantType ->
            when (grantType) {
                "authorization_code" -> builder.authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
                "refresh_token" -> builder.authorizationGrantType(AuthorizationGrantType.REFRESH_TOKEN)
                "client_credentials" -> builder.authorizationGrantType(AuthorizationGrantType.CLIENT_CREDENTIALS)
            }
        }

        // Add redirect URIs
        client.redirectUris.forEach { uri ->
            builder.redirectUri(uri)
        }

        // Add scopes
        client.scopes.forEach { scope ->
            builder.scope(scope)
        }

        // Configure token settings
        val tokenSettingsBuilder = TokenSettings.builder()
            .accessTokenTimeToLive(Duration.ofSeconds(client.accessTokenTimeToLive))

        // Only set refresh token TTL if it's greater than zero (client credentials doesn't use refresh tokens)
        if (client.refreshTokenTimeToLive > 0) {
            tokenSettingsBuilder.refreshTokenTimeToLive(Duration.ofSeconds(client.refreshTokenTimeToLive))
        }

        val tokenSettings = tokenSettingsBuilder.build()

        // Configure client settings
        val clientSettings = ClientSettings.builder()
            .requireProofKey(client.requireProofKey)
            .requireAuthorizationConsent(true)
            .build()

        return builder
            .tokenSettings(tokenSettings)
            .clientSettings(clientSettings)
            .build()
    }
}
