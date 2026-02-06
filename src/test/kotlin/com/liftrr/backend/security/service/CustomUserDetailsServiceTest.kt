package com.liftrr.backend.security.service

import com.liftrr.backend.domain.entity.Role
import com.liftrr.backend.domain.entity.User
import com.liftrr.backend.domain.repository.UserRepository
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.Mockito.*
import org.mockito.junit.jupiter.MockitoExtension
import org.springframework.security.core.userdetails.UsernameNotFoundException
import java.util.*

@ExtendWith(MockitoExtension::class)
class CustomUserDetailsServiceTest {

    @Mock
    private lateinit var userRepository: UserRepository

    @InjectMocks
    private lateinit var customUserDetailsService: CustomUserDetailsService

    private lateinit var testUser: User
    private lateinit var userRole: Role

    @BeforeEach
    fun setUp() {
        userRole = Role(
            id = 1L,
            name = "ROLE_USER",
            description = "Regular user role"
        )

        testUser = User(
            id = 1L,
            username = "testuser",
            password = "encodedPassword",
            email = "test@example.com",
            enabled = true,
            roles = mutableSetOf(userRole)
        )
    }

    @Test
    fun `should load user by username successfully`() {
        // Arrange
        `when`(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser))

        // Act
        val userDetails = customUserDetailsService.loadUserByUsername("testuser")

        // Assert
        assertNotNull(userDetails)
        assertEquals("testuser", userDetails.username)
        assertEquals("encodedPassword", userDetails.password)
        assertTrue(userDetails.isEnabled)
        assertEquals(1, userDetails.authorities.size)
        assertTrue(userDetails.authorities.any { it.authority == "ROLE_USER" })

        verify(userRepository, times(1)).findByUsername("testuser")
    }

    @Test
    fun `should throw UsernameNotFoundException when user not found`() {
        // Arrange
        `when`(userRepository.findByUsername("nonexistent")).thenReturn(Optional.empty())

        // Act & Assert
        val exception = assertThrows(UsernameNotFoundException::class.java) {
            customUserDetailsService.loadUserByUsername("nonexistent")
        }

        assertEquals("User not found with username: nonexistent", exception.message)
        verify(userRepository, times(1)).findByUsername("nonexistent")
    }
}
