package com.liftrr.backend.api.controller

import com.liftrr.backend.api.dto.RegisterRequest
import com.liftrr.backend.api.dto.UserResponse
import com.liftrr.backend.domain.entity.Role
import com.liftrr.backend.domain.entity.User
import com.liftrr.backend.domain.repository.RoleRepository
import com.liftrr.backend.domain.repository.UserRepository
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/auth")
class AuthController(
    private val userRepository: UserRepository,
    private val roleRepository: RoleRepository,
    private val passwordEncoder: PasswordEncoder
) {

    @PostMapping("/register")
    fun register(@Valid @RequestBody request: RegisterRequest): ResponseEntity<Any> {
        // Check if username already exists
        if (userRepository.existsByUsername(request.username)) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(mapOf("error" to "Username already exists"))
        }

        // Check if email already exists
        if (userRepository.existsByEmail(request.email)) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(mapOf("error" to "Email already exists"))
        }

        // Get or create default user role
        val userRole = roleRepository.findByName("ROLE_USER")
            .orElseGet {
                roleRepository.save(
                    Role(name = "ROLE_USER", description = "Regular user role")
                )
            }

        // Create new user
        val user = User(
            username = request.username,
            password = passwordEncoder.encode(request.password),
            email = request.email,
            enabled = true,
            roles = mutableSetOf(userRole)
        )

        val savedUser = userRepository.save(user)

        val userResponse = UserResponse(
            id = savedUser.id!!,
            username = savedUser.username,
            email = savedUser.email,
            roles = savedUser.roles.map { it.name }.toSet()
        )

        return ResponseEntity.status(HttpStatus.CREATED).body(userResponse)
    }
}
