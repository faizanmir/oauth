package com.liftrr.backend.api.controller

import com.liftrr.backend.domain.entity.Role
import com.liftrr.backend.domain.entity.User
import com.liftrr.backend.domain.repository.RoleRepository
import com.liftrr.backend.domain.repository.UserRepository
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import jakarta.servlet.http.HttpSession
import org.springframework.security.authentication.AuthenticationManager
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.security.web.context.HttpSessionSecurityContextRepository
import org.springframework.security.web.savedrequest.HttpSessionRequestCache
import org.springframework.stereotype.Controller
import org.springframework.ui.Model
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam

/**
 * Web pages for the authorization server login flow.
 * POST /auth/login is handled by Spring Security formLogin (see SecurityConfig).
 */
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
        // Handle error parameter (from failed login attempts)
        if (error != null) {
            model.addAttribute("error", "Invalid username or password. Please try again.")
        }

        message?.let { model.addAttribute("message", it) }

        return "login"
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
        request: HttpServletRequest,
        response: HttpServletResponse,
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

            // Resume the saved /oauth2/authorize request, if any
            "redirect:" + (HttpSessionRequestCache().getRequest(request, response)?.redirectUrl ?: "/")
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
