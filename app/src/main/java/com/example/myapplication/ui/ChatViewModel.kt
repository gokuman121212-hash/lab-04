package com.example.myapplication.ui

import androidx.compose.runtime.mutableStateListOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.myapplication.model.ChatRequest
import com.example.myapplication.network.RetrofitClient
import kotlinx.coroutines.launch

data class Message(val text: String, val isUser: Boolean)

class ChatViewModel : ViewModel() {
    val messages = mutableStateListOf<Message>()

    fun sendMessage(prompt: String) {
        if (prompt.isBlank()) return
        messages.add(Message(prompt, isUser = true))

        viewModelScope.launch {
            try {
                val response = RetrofitClient.apiService.sendMessage(ChatRequest(prompt))
                messages.add(Message(response.response, isUser = false))
            } catch (e: Exception) {
                messages.add(Message("Error de conexión: ", isUser = false))
            }
        }
    }
}
