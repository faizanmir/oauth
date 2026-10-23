package com.liftrr.backend.api.controller

import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.GetMapping

/**
 * Fallback page for the mobile OAuth2 redirect URI.
 * When Android App Links are verified, the app intercepts this URL and this page is never shown.
 * The page intentionally does not render the authorization code or state.
 */
@Controller
class OAuth2CallbackController {

    @GetMapping("/oauth2/callback")
    fun oauth2Callback(): String = "mobile-callback"
}
