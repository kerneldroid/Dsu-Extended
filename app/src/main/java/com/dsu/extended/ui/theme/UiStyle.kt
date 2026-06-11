package com.dsu.extended.ui.theme

enum class UiStyle(val value: String) {
    EXPRESSIVE("expressive");

    companion object {
        fun fromPreference(value: String): UiStyle {
            return entries.firstOrNull { it.value == value } ?: EXPRESSIVE
        }
    }
}
