package dev.romerobrayan.tinto.core.domain.model

data class Category(
    val id: String,
    val name: String,
    val iconKey: String,
    val colorHex: String,
    val isSystem: Boolean,
)
