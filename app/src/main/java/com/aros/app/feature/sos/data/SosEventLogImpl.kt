package com.aros.app.feature.sos.data

import com.aros.app.feature.sos.domain.SosEvent
import com.aros.app.feature.sos.domain.SosEventLog
import com.aros.app.feature.sos.domain.SosEventType
import com.aros.app.feature.sos.domain.SosMode
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

class SosEventLogImpl(
    private val dao: SosEventDao,
    private val ioDispatcher: CoroutineDispatcher
) : SosEventLog {

    override val eventsFlow: Flow<List<SosEvent>> = dao.getAllFlow().map { entities ->
        entities.map { it.toDomain() }
    }

    override suspend fun logEvent(
        type: SosEventType,
        mode: SosMode,
        contactName: String?,
        deviceLocked: Boolean,
        screenInteractive: Boolean,
        detail: String
    ) = withContext(ioDispatcher) {
        dao.insert(
            SosEventEntity(
                type = type.name,
                mode = mode.name,
                contactName = contactName,
                deviceLocked = deviceLocked,
                screenInteractive = screenInteractive,
                detail = detail
            )
        )
        dao.pruneOldEntries()
    }

    override suspend fun clearLog() = withContext(ioDispatcher) {
        dao.clearAll()
    }

    private fun SosEventEntity.toDomain(): SosEvent {
        val eventType = SosEventType.fromLegacyOrName(type)
        val eventMode = try {
            SosMode.valueOf(mode)
        } catch (_: Exception) {
            SosMode.TEST
        }
        return SosEvent(
            id = id,
            timestamp = timestamp,
            type = eventType,
            mode = eventMode,
            contactName = contactName,
            deviceLocked = deviceLocked,
            screenInteractive = screenInteractive,
            detail = detail
        )
    }
}
