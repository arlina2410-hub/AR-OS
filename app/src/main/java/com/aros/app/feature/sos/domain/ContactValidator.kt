package com.aros.app.feature.sos.domain

object ContactValidator {
    const val MAX_CONTACTS = 20

    private val BLOCKED_EMERGENCY_NUMBERS = setOf(
        "911", "112", "999", "000", "110", "119", "120", "122", "100", "101", "102", "108", "1091", "1098"
    )

    // Common country calling codes to strip for matching local emergency numbers
    private val COUNTRY_CALLING_CODES = listOf(
        "1", "44", "91", "61", "81", "86", "49", "33", "39", "7"
    )

    fun normalizeNumber(input: String): String {
        return input.replace(Regex("[\\s\\-\\(\\)\\.]"), "")
    }

    fun isEmergencyNumber(number: String): Boolean {
        val normalized = normalizeNumber(number)
        val digitsOnly = normalized.removePrefix("+")

        if (BLOCKED_EMERGENCY_NUMBERS.contains(digitsOnly)) {
            return true
        }

        // Check if removing country calling code reveals an emergency number (e.g. +91 112 -> 112)
        if (normalized.startsWith("+")) {
            for (code in COUNTRY_CALLING_CODES) {
                if (digitsOnly.startsWith(code)) {
                    val nationalNumber = digitsOnly.removePrefix(code)
                    if (BLOCKED_EMERGENCY_NUMBERS.contains(nationalNumber)) {
                        return true
                    }
                }
            }
        }

        return false
    }

    fun validateContact(
        name: String,
        number: String,
        currentContactCount: Int,
        isUpdate: Boolean = false
    ): Result<String> {
        val trimmedName = name.trim()
        if (trimmedName.isEmpty()) {
            return Result.failure(IllegalArgumentException("Contact name cannot be empty."))
        }

        val normalized = normalizeNumber(number)
        if (normalized.length < 3) {
            return Result.failure(IllegalArgumentException("Phone number is too short."))
        }

        // Check against blocked emergency services numbers
        if (isEmergencyNumber(normalized)) {
            return Result.failure(
                IllegalArgumentException(
                    "Emergency-service numbers can't be saved here — the phone's own Emergency SOS handles those."
                )
            )
        }

        if (!isUpdate && currentContactCount >= MAX_CONTACTS) {
            return Result.failure(
                IllegalStateException("Maximum limit of $MAX_CONTACTS trusted contacts reached.")
            )
        }

        return Result.success(normalized)
    }
}
