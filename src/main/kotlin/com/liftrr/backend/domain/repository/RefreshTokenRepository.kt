package com.liftrr.backend.domain.repository

import com.liftrr.backend.domain.entity.RefreshToken
import com.liftrr.backend.domain.entity.User
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.*

@Repository
interface RefreshTokenRepository : JpaRepository<RefreshToken, Long> {
    fun findByToken(token: String): Optional<RefreshToken>
    fun deleteByUser(user: User)
}
