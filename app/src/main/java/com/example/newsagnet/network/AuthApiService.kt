package com.example.newsagnet.network

import com.example.newsagnet.network.dto.AuthRequestDto
import com.example.newsagnet.network.dto.AuthResponseDto
import com.example.newsagnet.network.dto.LogoutResponseDto
import com.example.newsagnet.network.dto.PreferencesUpdateDto
import com.example.newsagnet.network.dto.UserDto
import com.example.newsagnet.network.dto.UserPreferencesDto
import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.PUT

/**
 * Retrofit interface defining authentication and user identity / cloud preference endpoints.
 */
interface AuthApiService {

    @POST("auth/google")
    fun loginWithGoogleCall(
        @Body request: AuthRequestDto
    ): Call<AuthResponseDto>

    @POST("auth/logout")
    fun logoutCall(
        @Header("Authorization") authHeader: String?
    ): Call<LogoutResponseDto>

    @GET("users/me")
    fun getMeCall(
        @Header("Authorization") authHeader: String
    ): Call<UserDto>

    @GET("users/me/preferences")
    fun getPreferencesCall(
        @Header("Authorization") authHeader: String
    ): Call<UserPreferencesDto>

    @PUT("users/me/preferences")
    fun updatePreferencesCall(
        @Header("Authorization") authHeader: String,
        @Body body: PreferencesUpdateDto
    ): Call<UserPreferencesDto>
}
