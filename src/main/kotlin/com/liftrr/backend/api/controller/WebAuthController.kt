package com.liftrr.backend.api.controller

import com.liftrr.backend.domain.entity.Role
import com.liftrr.backend.domain.entity.User
import com.liftrr.backend.domain.repository.RoleRepository
import com.liftrr.backend.domain.repository.UserRepository
import jakarta.servlet.http.HttpSession
import org.springframework.security.authentication.AuthenticationManager
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.security.web.context.HttpSessionSecurityContextRepository
import org.springframework.stereotype.Controller
import org.springframework.ui.Model
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam

@Controller
@RequestMapping("/auth")
class WebAuthController(
    private val userRepository: UserRepository,
    private val roleRepository: RoleRepository,
    private val passwordEncoder: PasswordEncoder,
    private val authenticationManager: AuthenticationManager
) {

    @GetMapping("/login")
    fun loginPage(
        @RequestParam(required = false) error: String?,
        @RequestParam(required = false) message: String?,
        model: Model
    ): String {
        error?.let { model.addAttribute("error", it) }
        message?.let { model.addAttribute("message", it) }
        return "login"
    }

    @PostMapping("/login")
    fun login(
        @RequestParam username: String,
        @RequestParam password: String,
        session: HttpSession,
        model: Model
    ): String {
        return try {
            // Authenticate user
            val authentication = authenticationManager.authenticate(
                UsernamePasswordAuthenticationToken(username, password)
            )

            // Set authentication in security context
            SecurityContextHolder.getContext().authentication = authentication
            session.setAttribute(
                HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY,
                SecurityContextHolder.getContext()
            )

            // Get user details
            val user = userRepository.findByUsername(username)
                .orElseThrow { IllegalStateException("User not found") }

            // Prepare user data for success page
            model.addAttribute("title", "Welcome Back!")
            model.addAttribute("message", "You have been successfully logged in as ${user.username}")
            model.addAttribute("userData", mapOf(
                "username" to user.username,
                "email" to user.email
            ))

            "success"
        } catch (e: Exception) {
            model.addAttribute("error", "Invalid username or password")
            "login"
        }
    }

    @GetMapping("/register")
    fun registerPage(
        @RequestParam(required = false) error: String?,
        model: Model
    ): String {
        error?.let { model.addAttribute("error", it) }
        return "register"
    }

    @PostMapping("/register")
    fun register(
        @RequestParam username: String,
        @RequestParam email: String,
        @RequestParam password: String,
        @RequestParam confirmPassword: String,
        session: HttpSession,
        model: Model
    ): String {
        // Validation
        if (username.length < 3) {
            model.addAttribute("error", "Username must be at least 3 characters")
            return "register"
        }

        if (password != confirmPassword) {
            model.addAttribute("error", "Passwords do not match")
            return "register"
        }

        if (password.length < 6) {
            model.addAttribute("error", "Password must be at least 6 characters")
            return "register"
        }

        // Check if username exists
        if (userRepository.existsByUsername(username)) {
            model.addAttribute("error", "Username already exists")
            return "register"
        }

        // Check if email exists
        if (userRepository.existsByEmail(email)) {
            model.addAttribute("error", "Email already registered")
            return "register"
        }

        return try {
            // Get or create user role
            val userRole = roleRepository.findByName("ROLE_USER")
                .orElseGet {
                    roleRepository.save(
                        Role(name = "ROLE_USER", description = "Regular user role")
                    )
                }

            // Create new user
            val newUser = User(
                username = username,
                password = passwordEncoder.encode(password),
                email = email,
                enabled = true,
                roles = mutableSetOf(userRole)
            )

            userRepository.save(newUser)

            // Auto-login the user
            val authentication = authenticationManager.authenticate(
                UsernamePasswordAuthenticationToken(username, password)
            )
            SecurityContextHolder.getContext().authentication = authentication
            session.setAttribute(
                HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY,
                SecurityContextHolder.getContext()
            )

            // Show success page
            model.addAttribute("title", "Account Created!")
            model.addAttribute("message", "Welcome to our platform, $username!")
            model.addAttribute("userData", mapOf(
                "username" to newUser.username,
                "email" to newUser.email
            ))

            "success"
        } catch (e: Exception) {
            model.addAttribute("error", "Registration failed: ${e.message}")
            "register"
        }
    }

    @GetMapping("/logout")
    fun logout(session: HttpSession, model: Model): String {
        session.invalidate()
        SecurityContextHolder.clearContext()

        model.addAttribute("title", "Logged Out")
        model.addAttribute("message", "You have been successfully logged out")

        return "success"
    }
}
