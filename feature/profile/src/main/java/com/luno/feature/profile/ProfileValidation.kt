package com.luno.feature.profile

object ProfileValidation {

    fun validateDisplayName(name: String): String? {
        val trimmed = name.trim()
        if (trimmed.length < 2) return "Tên hiển thị phải có ít nhất 2 ký tự"
        if (trimmed.length > 50) return "Tên hiển thị không vượt quá 50 ký tự"
        if (trimmed.contains(Regex("[<>/\\\\{}\\[\\]]"))) return "Tên chứa ký tự không hợp lệ"
        return null
    }

    fun validateUsername(username: String): String? {
        val trimmed = username.trim()
        if (trimmed.isBlank()) return null
        if (trimmed.length < 3) return "Username phải có ít nhất 3 ký tự"
        if (trimmed.length > 30) return "Username không vượt quá 30 ký tự"
        if (!trimmed.matches(Regex("^[a-zA-Z_][a-zA-Z0-9_]*$"))) {
            return "Username chỉ chứa chữ cái, số và dấu gạch dưới, không bắt đầu bằng số"
        }
        return null
    }

    fun validateBio(bio: String): String? {
        if (bio.length > 150) return "Tiểu sử không vượt quá 150 ký tự"
        return null
    }
}
