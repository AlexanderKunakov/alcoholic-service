package ru.buhinder.alcoholicservice.entity

import java.util.UUID
import org.springframework.data.annotation.Id
import org.springframework.data.relational.core.mapping.Table

@Table("alcoholic")
data class AlcoholicEntity(
    @Id val id: UUID,
    val firstname: String,
    val lastName: String,
    val age: Int?,
    val login: String,
    val password: String,
    val email: String,
    val photoId: UUID? = null
)
