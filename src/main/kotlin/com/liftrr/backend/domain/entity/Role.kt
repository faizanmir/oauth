package com.liftrr.backend.domain.entity

import jakarta.persistence.*

@Entity
@Table(name = "roles")
class Role(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,

    @Column(nullable = false, unique = true, length = 50)
    val name: String,

    @Column(length = 255)
    val description: String? = null,

    @ManyToMany(mappedBy = "roles")
    val users: MutableSet<User> = mutableSetOf()
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is Role) return false
        return id != null && id == other.id
    }

    override fun hashCode(): Int {
        return id?.hashCode() ?: 0
    }

    override fun toString(): String {
        return "Role(id=$id, name='$name', description=$description)"
    }
}
