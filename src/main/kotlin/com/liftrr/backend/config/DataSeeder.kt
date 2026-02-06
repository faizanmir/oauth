package com.liftrr.backend.config

import com.liftrr.backend.domain.entity.OAuth2Client
import com.liftrr.backend.domain.entity.Role
import com.liftrr.backend.domain.entity.User
import com.liftrr.backend.domain.repository.OAuth2ClientRepository
import com.liftrr.backend.domain.repository.RoleRepository
import com.liftrr.backend.domain.repository.UserRepository
import org.slf4j.LoggerFactory
import org.springframework.boot.CommandLineRunner
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Component

@Component
class DataSeeder(
    private val roleRepository: RoleRepository,
    private val userRepository: UserRepository,
    private val oAuth2ClientRepository: OAuth2ClientRepository,
    private val passwordEncoder: PasswordEncoder
) : CommandLineRunner {

    private val logger = LoggerFactory.getLogger(DataSeeder::class.java)

    override fun run(vararg args: String) {
        seedRoles()
        seedUsers()
        seedOAuth2Clients()
    }

    private fun seedRoles() {
        if (roleRepository.count() == 0L) {
            val adminRole = Role(
                name = "ROLE_ADMIN",
                description = "Administrator role with full access"
            )
            val userRole = Role(
                name = "ROLE_USER",
                description = "Regular user role"
            )

            roleRepository.saveAll(listOf(adminRole, userRole))
            logger.info("Roles seeded successfully")
        } else {
            logger.info("Roles already exist, skipping seeding")
        }
    }

    private fun seedUsers() {
        if (userRepository.count() == 0L) {
            val adminRole = roleRepository.findByName("ROLE_ADMIN").orElseThrow()
            val userRole = roleRepository.findByName("ROLE_USER").orElseThrow()

            val adminUser = User(
                username = "admin",
                password = passwordEncoder.encode("admin123")!!,
                email = "admin@liftrr.com",
                enabled = true,
                roles = mutableSetOf(adminRole, userRole)
            )

            val regularUser = User(
                username = "user",
                password = passwordEncoder.encode("user123")!!,
                email = "user@liftrr.com",
                enabled = true,
                roles = mutableSetOf(userRole)
            )

            userRepository.saveAll(listOf(adminUser, regularUser))
            logger.info("Users seeded successfully")
        } else {
            logger.info("Users already exist, skipping seeding")
        }
    }

    private fun seedOAuth2Clients() {
        if (oAuth2ClientRepository.count() == 0L) {
            // Web client with Authorization Code + PKCE
            val webClient = OAuth2Client(
                clientId = "liftrr-web-client",
                clientSecret = passwordEncoder.encode("secret")!!,
                clientName = "Liftrr Web Application",
                grantTypes = mutableSetOf("authorization_code", "refresh_token"),
                redirectUris = mutableSetOf(
                    "https://web-production-2b788.up.railway.app/oauth2/callback",
                    "https://web-production-2b788.up.railway.app/authorized",
                    "http://localhost:3000/authorized",  // Keep for local development
                    "http://localhost:3000/callback"
                ),
                scopes = mutableSetOf("read", "write", "openid", "profile", "email"),
                accessTokenTimeToLive = 3600,
                refreshTokenTimeToLive = 86400,
                requireProofKey = true
            )

            // Service client with Client Credentials
            val serviceClient = OAuth2Client(
                clientId = "liftrr-service-client",
                clientSecret = passwordEncoder.encode("service-secret")!!,
                clientName = "Liftrr Service Application",
                grantTypes = mutableSetOf("client_credentials"),
                redirectUris = mutableSetOf(),
                scopes = mutableSetOf("service.read", "service.write"),
                accessTokenTimeToLive = 3600,
                refreshTokenTimeToLive = 0,
                requireProofKey = false
            )

            // Mobile client with Password Grant
            val mobileClient = OAuth2Client(
                clientId = "liftrr-mobile-client",
                clientSecret = passwordEncoder.encode("mobile-secret")!!,
                clientName = "Liftrr Mobile Application",
                grantTypes = mutableSetOf("password", "refresh_token"),
                redirectUris = mutableSetOf(),
                scopes = mutableSetOf("read", "write", "openid"),
                accessTokenTimeToLive = 3600,
                refreshTokenTimeToLive = 86400,
                requireProofKey = false
            )

            oAuth2ClientRepository.saveAll(listOf(webClient, serviceClient, mobileClient))
            logger.info("OAuth2 clients seeded successfully")
        } else {
            logger.info("OAuth2 clients already exist, skipping seeding")
        }
    }
}
