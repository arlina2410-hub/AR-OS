package com.aros.app

import com.aros.app.feature.sos.domain.ContactValidator
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ContactValidatorTest {

    @Test
    fun testU12_NormalizesPhoneNumber() {
        val result = ContactValidator.validateContact(
            name = "Sarah",
            number = "+1 (555) 234-5678",
            currentContactCount = 0
        )
        assertTrue(result.isSuccess)
        assertEquals("+15552345678", result.getOrNull())
    }

    @Test
    fun testU12_BlocksEmergencyNumbers() {
        listOf("911", "112", "999", "000", "+911", "1091", "1098").forEach { emergencyNum ->
            val result = ContactValidator.validateContact(
                name = "Emergency",
                number = emergencyNum,
                currentContactCount = 0
            )
            assertTrue("Should block $emergencyNum", result.isFailure)
            val msg = result.exceptionOrNull()?.message ?: ""
            assertTrue("Message should match spec: $msg", msg.contains("Emergency-service numbers can't be saved here"))
        }
    }

    @Test
    fun testU12_BlocksEmergencyNumbersWithCountryCode() {
        listOf("+91112", "+1911", "+44999").forEach { emergencyNum ->
            val result = ContactValidator.validateContact(
                name = "Emergency",
                number = emergencyNum,
                currentContactCount = 0
            )
            assertTrue("Should block $emergencyNum with country code", result.isFailure)
        }
    }

    @Test
    fun testU12_EnforcesTwentyContactCap() {
        val result = ContactValidator.validateContact(
            name = "Twenty First",
            number = "555-1234",
            currentContactCount = 20,
            isUpdate = false
        )
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull()?.message?.contains("Maximum limit") == true)
    }

    @Test
    fun testU12_AllowsUpdateWhenAtCap() {
        val result = ContactValidator.validateContact(
            name = "Existing Contact",
            number = "555-1234",
            currentContactCount = 20,
            isUpdate = true
        )
        assertTrue(result.isSuccess)
    }

    @Test
    fun testU12_RejectsBlankName() {
        val result = ContactValidator.validateContact(
            name = "   ",
            number = "555-1234",
            currentContactCount = 0
        )
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull()?.message?.contains("cannot be empty") == true)
    }
}
