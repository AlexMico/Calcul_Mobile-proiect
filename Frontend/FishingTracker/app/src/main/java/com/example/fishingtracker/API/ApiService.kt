package com.example.fishingtracker.API
import com.example.fishingtracker.data.LoginRequest
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST

interface ApiService {
    @POST("login")
    suspend fun Login(@Body request: LoginRequest): Response<String>
}