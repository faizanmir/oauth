package com.liftrr.backend.api.controller

import jakarta.servlet.RequestDispatcher
import jakarta.servlet.http.HttpServletRequest
import org.springframework.boot.web.servlet.error.ErrorController
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Controller
import org.springframework.ui.Model
import org.springframework.web.bind.annotation.RequestMapping

@Controller
class CustomErrorController : ErrorController {

    @RequestMapping("/error")
    fun handleError(request: HttpServletRequest, model: Model): String {
        val status = request.getAttribute(RequestDispatcher.ERROR_STATUS_CODE)
        val errorMessage = request.getAttribute(RequestDispatcher.ERROR_MESSAGE)
        val exception = request.getAttribute(RequestDispatcher.ERROR_EXCEPTION)

        val statusCode = status?.toString()?.toIntOrNull() ?: 500
        val httpStatus = HttpStatus.valueOf(statusCode)

        model.addAttribute("statusCode", statusCode)
        model.addAttribute("statusMessage", httpStatus.reasonPhrase)
        model.addAttribute("errorMessage", errorMessage ?: "An unexpected error occurred")
        model.addAttribute("path", request.getAttribute(RequestDispatcher.ERROR_REQUEST_URI))

        return "error"
    }
}
