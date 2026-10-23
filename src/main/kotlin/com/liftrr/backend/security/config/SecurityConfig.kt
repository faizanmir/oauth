package com.liftrr.backend.security.config

import com.liftrr.backend.security.service.CustomUserDetailsService
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.annotation.Order
import org.springframework.security.authentication.AuthenticationManager
import org.springframework.security.authentication.AuthenticationProvider
import org.springframework.security.authentication.dao.DaoAuthenticationProvider
import org.springframework.security.config.Customizer
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.security.web.SecurityFilterChain

@Configuration
@EnableWebSecurity
class SecurityConfig(
    private val userDetailsService: CustomUserDetailsService
) {

    @Bean
    @Order(3)
    fun defaultSecurityFilterChain(http: HttpSecurity): SecurityFilterChain {
        http
            .authorizeHttpRequests { authorize ->
                authorize
                    .requestMatchers("/h2-console/**").permitAll()
                    .requestMatchers("/api/auth/register", "/api/auth/login").permitAll()
                    .requestMatchers("/auth/**").permitAll()  // Allow web login/register pages
                    .requestMatchers("/oauth2/callback").permitAll()  // App Links fallback page
                    .requestMatchers("/css/**", "/js/**", "/images/**", "/favicon.ico").permitAll()  // Static resources
                    .requestMatchers("/actuator/health").permitAll()
                    .requestMatchers("/.well-known/**").permitAll()  // Allow assetlinks.json and apple-app-site-association
                    .requestMatchers("/error").permitAll()
                    .anyRequest().authenticated()
            }
            // Login page for the authorization server; the default success handler
            // resumes the saved /oauth2/authorize request.
            .formLogin { formLogin ->
                formLogin
                    .loginPage("/auth/login")
                    .loginProcessingUrl("/auth/login")
                    .failureUrl("/auth/login?error")
                    .permitAll()
            }
            .httpBasic(Customizer.withDefaults())
            .csrf { csrf ->
                csrf.ignoringRequestMatchers("/h2-console/**", "/api/auth/**")
            }
            .headers { headers ->
                headers.frameOptions { frameOptions ->
                    frameOptions.sameOrigin()
                }
            }

        return http.build()
    }

    @Bean
    fun passwordEncoder(): PasswordEncoder {
        return BCryptPasswordEncoder()
    }

    @Bean
    fun authenticationProvider(passwordEncoder: PasswordEncoder): AuthenticationProvider {
        val provider = DaoAuthenticationProvider()
        provider.setUserDetailsService(userDetailsService)
        provider.setPasswordEncoder(passwordEncoder)
        return provider
    }

    @Bean
    fun authenticationManager(config: AuthenticationConfiguration): AuthenticationManager {
        return config.authenticationManager
    }
}
