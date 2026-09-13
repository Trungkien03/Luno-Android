package com.luno.core.model

data class User(
    val id: String,
    val name: String,
    val avatarUrl: String,
    val isOnline: Boolean = false,
    val lastSeen: String = "Vừa mới truy cập"
)
