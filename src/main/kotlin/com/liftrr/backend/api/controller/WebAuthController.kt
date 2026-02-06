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
        @RequestParam(name = "redirect_url", required = false) redirectUrl: String?,
        model: Model
    ): String {
        // Handle error parameter (from failed login attempts)
        if (error != null) {
            model.addAttribute("error", "Invalid username or password. Please try again.")
        }

        message?.let { model.addAttribute("message", it) }
        redirectUrl?.let { model.addAttribute("redirectUrl", it) }

        return "login"
    }

    @PostMapping("/login")
    fun login(
        @RequestParam username: String,
        @RequestParam password: String,
        @RequestParam(name = "redirect_url", required = false) redirectUrl: String?,
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
                .orElseGet {
                    userRepository.findByEmail(username)
                        .orElseThrow { IllegalStateException("User not found") }
                }

            // Prepare user data for success page
            model.addAttribute("title", "Welcome Back!")
            model.addAttribute("message", "You have been successfully logged in as ${user.username}")
            model.addAttribute("userData", mapOf(
                "username" to user.username,
                "email" to user.email
            ))

            // Pass redirect URL to success page if provided
            redirectUrl?.let { model.addAttribute("redirectUrl", it) }

            "success"
        } catch (e: Exception) {
            model.addAttribute("error", "Invalid username or password")
            redirectUrl?.let { model.addAttribute("redirectUrl", it) }
            "login"
        }
    }

    @GetMapping("/register")
    fun registerPage(
        @RequestParam(required = false) error: String?,
        @RequestParam(name = "redirect_url", required = false) redirectUrl: String?,
        model: Model
    ): String {
        error?.let { model.addAttribute("error", it) }
        redirectUrl?.let { model.addAttribute("redirectUrl", it) }
        return "register"
    }

    @PostMapping("/register")
    fun register(
        @RequestParam username: String,
        @RequestParam email: String,
        @RequestParam password: String,
        @RequestParam confirmPassword: String,
        @RequestParam(name = "redirect_url", required = false) redirectUrl: String?,
        session: HttpSession,
        model: Model
    ): String {
        // Validation
        if (username.length < 3) {
            model.addAttribute("error", "Username must be at least 3 characters")
            redirectUrl?.let { model.addAttribute("redirectUrl", it) }
            return "register"
        }

        if (password != confirmPassword) {
            model.addAttribute("error", "Passwords do not match")
            redirectUrl?.let { model.addAttribute("redirectUrl", it) }
            return "register"
        }

        if (password.length < 6) {
            model.addAttribute("error", "Password must be at least 6 characters")
            redirectUrl?.let { model.addAttribute("redirectUrl", it) }
            return "register"
        }

        // Check if username exists
        if (userRepository.existsByUsername(username)) {
            model.addAttribute("error", "Username already exists")
            redirectUrl?.let { model.addAttribute("redirectUrl", it) }
            return "register"
        }

        // Check if email exists
        if (userRepository.existsByEmail(email)) {
            model.addAttribute("error", "Email already registered")
            redirectUrl?.let { model.addAttribute("redirectUrl", it) }
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

            // Pass redirect URL to success page if provided
            redirectUrl?.let { model.addAttribute("redirectUrl", it) }

            "success"
        } catch (e: Exception) {
            model.addAttribute("error", "Registration failed: ${e.message}")
            redirectUrl?.let { model.addAttribute("redirectUrl", it) }
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
