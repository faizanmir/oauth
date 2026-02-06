package com.liftrr.backend.domain.entity

import jakarta.persistence.*

@Entity
@Table(name = "oauth2_clients")
class OAuth2Client(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,

    @Column(nullable = false, unique = true, length = 100)
    val clientId: String,

    @Column(nullable = false)
    val clientSecret: String,

    @Column(nullable = false, length = 100)
    val clientName: String,

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "client_grant_types", joinColumns = [JoinColumn(name = "client_id")])
    @Column(name = "grant_type")
    val grantTypes: MutableSet<String> = mutableSetOf(),

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "client_redirect_uris", joinColumns = [JoinColumn(name = "client_id")])
    @Column(name = "redirect_uri")
    val redirectUris: MutableSet<String> = mutableSetOf(),

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "client_scopes", joinColumns = [JoinColumn(name = "client_id")])
    @Column(name = "scope")
    val scopes: MutableSet<String> = mutableSetOf(),

    @Column(nullable = false)
    val accessTokenTimeToLive: Long = 3600, // 1 hour in seconds

    @Column(nullable = false)
    val refreshTokenTimeToLive: Long = 86400, // 24 hours in seconds

    @Column(nullable = false)
    val requireProofKey: Boolean = false // PKCE support
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is OAuth2Client) return false
        return id != null && id == other.id
    }

    override fun hashCode(): Int {
        return id?.hashCode() ?: 0
    }

    override fun toString(): String {
        return "OAuth2Client(id=$id, clientId='$clientId', clientName='$clientName')"
    }
}
