package com.liftrr.backend.security.service

import com.liftrr.backend.domain.repository.UserRepository
import org.springframework.security.core.userdetails.UserDetails
import org.springframework.security.core.userdetails.UserDetailsService
import org.springframework.security.core.userdetails.UsernameNotFoundException
import org.springframework.stereotype.Service

@Service
class CustomUserDetailsService(
    private val userRepository: UserRepository
) : UserDetailsService {

    override fun loadUserByUsername(usernameOrEmail: String): UserDetails {
        // Try to find by username first, then by email
        return userRepository.findByUsername(usernameOrEmail)
            .orElseGet {
                userRepository.findByEmail(usernameOrEmail)
                    .orElseThrow { UsernameNotFoundException("User not found with username or email: $usernameOrEmail") }
            }
    }
}
