package com.notepadvault.app

data class Note(
    val id: String,
    val title: String,
    val content: String,
    val updatedAt: Long,
    val isHidden: Boolean = false,
    val category: String = "Personal"
)
