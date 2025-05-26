package com.github.callmeqan.jarvismobile

data class ChatMessage(
    var message: String,
    var role: String = "user",
)
