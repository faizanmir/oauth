package com.liftrr.backend.api.controller

import com.liftrr.backend.api.dto.UserResponse
import com.liftrr.backend.domain.repository.UserRepository
import org.springframework.http.ResponseEntity
import org.springframework.security.core.Authentication
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/user")
class UserController(
    private val userRepository: UserRepository
) {

    @GetMapping("/me")
    fun getCurrentUser(authentication: Authentication): ResponseEntity<UserResponse> {
        val username = authentication.name

        val user = userRepository.findByUsername(username)
            .orElseThrow { IllegalStateException("User not found") }

        val userResponse = UserResponse(
            id = user.id!!,
            username = user.username,
            email = user.email,
            roles = user.roles.map { it.name }.toSet()
        )

        return ResponseEntity.ok(userResponse)
    }

    @GetMapping("/profile")
    fun getUserProfile(authentication: Authentication): ResponseEntity<Map<String, Any>> {
        val username = authentication.name

        val user = userRepository.findByUsername(username)
            .orElseThrow { IllegalStateException("User not found") }

        val profile: Map<String, Any> = mapOf(
            "id" to (user.id ?: 0L),
            "username" to user.username,
            "email" to user.email,
            "roles" to user.roles.map { it.name },
            "enabled" to user.isEnabled,
            "createdAt" to user.createdAt.toString()
        )

        return ResponseEntity.ok(profile)
    }
}
