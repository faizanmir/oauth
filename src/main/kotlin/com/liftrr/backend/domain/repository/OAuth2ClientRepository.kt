package com.liftrr.backend.domain.repository

import com.liftrr.backend.domain.entity.OAuth2Client
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.*

@Repository
interface OAuth2ClientRepository : JpaRepository<OAuth2Client, Long> {
    fun findByClientId(clientId: String): Optional<OAuth2Client>
}
