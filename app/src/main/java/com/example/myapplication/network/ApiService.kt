package com.example.myapplication.network

import com.example.myapplication.model.ChatRequest
import com.example.myapplication.model.ChatResponse
import retrofit2.http.Body
import retrofit2.http.POST

interface ApiService {
    @POST("chat")
    suspend fun sendMessage(@Body request: ChatRequest): ChatResponse
}
