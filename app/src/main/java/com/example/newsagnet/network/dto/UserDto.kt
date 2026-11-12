package com.example.newsagnet.network.dto

import com.example.newsagnet.model.UserProfile
import com.google.gson.annotations.SerializedName

/**
 * Representation of a user profile in authentication responses.
 */
data class UserDto(
    @SerializedName("id")
    val id: String,

    @SerializedName("email")
    val email: String,

    @SerializedName("display_name")
    val displayName: String?,

    @SerializedName("auth_provider")
    val authProvider: String
) {
    fun toDomain(): UserProfile {
        return UserProfile(id, email, displayName, authProvider)
    }
}
