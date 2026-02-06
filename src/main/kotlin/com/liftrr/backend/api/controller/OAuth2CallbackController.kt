package com.liftrr.backend.api.controller

import jakarta.servlet.http.HttpServletRequest
import org.springframework.stereotype.Controller
import org.springframework.ui.Model
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestParam

/**
 * Controller to handle OAuth2 callbacks for mobile apps
 * These endpoints are triggered after successful authentication
 * and will be intercepted by Android App Links to open the app
 */
@Controller
class OAuth2CallbackController {

    /**
     * Main OAuth2 callback endpoint for mobile apps
     * Android App Links will intercept this HTTPS URL and open the app
     */
    @GetMapping("/oauth2/callback")
    fun oauth2Callback(
        @RequestParam(required = false) code: String?,
        @RequestParam(required = false) state: String?,
        @RequestParam(required = false) error: String?,
        @RequestParam(required = false) error_description: String?,
        request: HttpServletRequest,
        model: Model
    ): String {
        // If there's an error, show error page
        if (error != null) {
            model.addAttribute("statusCode", 400)
            model.addAttribute("statusMessage", "OAuth Error")
            model.addAttribute("errorMessage", error_description ?: error)
            return "error"
        }

        // Prepare data for the success page
        model.addAttribute("title", "Authentication Successful!")
        model.addAttribute("message", "You will be redirected to the app...")
        model.addAttribute("code", code)
        model.addAttribute("state", state)

        // Build the query parameters to pass to the app
        val queryParams = mutableListOf<String>()
        code?.let { queryParams.add("code=$it") }
        state?.let { queryParams.add("state=$it") }

        if (queryParams.isNotEmpty()) {
            model.addAttribute("queryParams", queryParams.joinToString("&"))
        }

        // Return success page that will attempt to open the app
        return "mobile-callback"
    }

    /**
     * Alternative mobile callback endpoint
     */
    @GetMapping("/mobile/callback")
    fun mobileCallback(
        @RequestParam(required = false) code: String?,
        @RequestParam(required = false) state: String?,
        @RequestParam(required = false) error: String?,
        @RequestParam(required = false) error_description: String?,
        @RequestParam(required = false) token: String?,
        @RequestParam(required = false) refresh_token: String?,
        request: HttpServletRequest,
        model: Model
    ): String {
        // If there's an error, show error page
        if (error != null) {
            model.addAttribute("statusCode", 400)
            model.addAttribute("statusMessage", "OAuth Error")
            model.addAttribute("errorMessage", error_description ?: error)
            return "error"
        }

        // Prepare data for the success page
        model.addAttribute("title", "Login Successful!")
        model.addAttribute("message", "Opening app...")
        model.addAttribute("code", code)
        model.addAttribute("state", state)
        model.addAttribute("token", token)
        model.addAttribute("refreshToken", refresh_token)

        // Build the query parameters
        val queryParams = mutableListOf<String>()
        code?.let { queryParams.add("code=$it") }
        state?.let { queryParams.add("state=$it") }
        token?.let { queryParams.add("token=$it") }
        refresh_token?.let { queryParams.add("refresh_token=$it") }

        if (queryParams.isNotEmpty()) {
            model.addAttribute("queryParams", queryParams.joinToString("&"))
        }

        return "mobile-callback"
    }
}
