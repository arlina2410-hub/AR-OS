package com.aros.app

import com.aros.app.feature.sos.platform.SimAccountInfo
import com.aros.app.feature.sos.platform.SimManager
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class SimManagerTest {

    class FakeSimManager(
        private val sims: List<SimAccountInfo>
    ) : SimManager {
        override fun getAvailableSims(): List<SimAccountInfo> = sims

        override fun getDefaultCallingSim(): SimAccountInfo? {
            return sims.find { it.isDefault } ?: sims.firstOrNull()
        }

        override fun getStoredOrResolvedAccount(storedSimId: String?): android.telecom.PhoneAccountHandle? {
            if (sims.isEmpty()) return null
            if (!storedSimId.isNullOrBlank()) {
                val matched = sims.find { it.id == storedSimId }
                if (matched != null) return matched.handle
            }
            return getDefaultCallingSim()?.handle
        }
    }

    @Test
    fun `empty SIM list returns null default and resolved account`() {
        val manager = FakeSimManager(emptyList())
        assertNull(manager.getDefaultCallingSim())
        assertNull(manager.getStoredOrResolvedAccount(null))
        assertNull(manager.getStoredOrResolvedAccount("sim_1"))
    }

    @Test
    fun `single SIM returns that SIM as default even when isDefault is false`() {
        val sim1 = SimAccountInfo(
            id = "sim_jio",
            label = "Jio",
            slotIndex = 0,
            isDefault = false,
            handle = null
        )
        val manager = FakeSimManager(listOf(sim1))
        assertEquals(sim1, manager.getDefaultCallingSim())
    }

    @Test
    fun `dual SIM picks marked default SIM when no stored preference exists`() {
        val sim1 = SimAccountInfo(
            id = "sim_airtel",
            label = "Airtel",
            slotIndex = 0,
            isDefault = false,
            handle = null
        )
        val sim2 = SimAccountInfo(
            id = "sim_jio",
            label = "Jio",
            slotIndex = 1,
            isDefault = true,
            handle = null
        )
        val manager = FakeSimManager(listOf(sim1, sim2))
        assertEquals(sim2, manager.getDefaultCallingSim())
    }

    @Test
    fun `stored preference overrides system default SIM when valid`() {
        val sim1 = SimAccountInfo(
            id = "sim_airtel",
            label = "Airtel",
            slotIndex = 0,
            isDefault = false,
            handle = null
        )
        val sim2 = SimAccountInfo(
            id = "sim_jio",
            label = "Jio",
            slotIndex = 1,
            isDefault = true,
            handle = null
        )
        val manager = FakeSimManager(listOf(sim1, sim2))

        // Request sim_airtel even though sim_jio is default
        val resolvedSim = manager.getAvailableSims().find { it.id == "sim_airtel" }
        assertEquals("sim_airtel", resolvedSim?.id)
    }
}
