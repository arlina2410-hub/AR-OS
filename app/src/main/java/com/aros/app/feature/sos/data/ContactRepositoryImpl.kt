package com.aros.app.feature.sos.data

import com.aros.app.feature.sos.domain.ContactRepository
import com.aros.app.feature.sos.domain.ContactRole
import com.aros.app.feature.sos.domain.ContactValidator
import com.aros.app.feature.sos.domain.SosContact
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class ContactRepositoryImpl(
    private val dao: SosContactDao
) : ContactRepository {

    override fun getAllContactsFlow(): Flow<List<SosContact>> {
        return dao.getAllFlow().map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override suspend fun getAllContacts(): List<SosContact> {
        return dao.getAllList().map { it.toDomain() }
    }

    override fun getPrimaryContactFlow(): Flow<SosContact?> {
        return dao.getPrimaryFlow().map { it?.toDomain() }
    }

    override suspend fun getPrimaryContact(): SosContact? {
        return dao.getPrimary()?.toDomain()
    }

    override suspend fun addContact(name: String, number: String, role: ContactRole): Result<Long> {
        val currentCount = dao.getCount()
        val validatedNumberResult = ContactValidator.validateContact(
            name = name,
            number = number,
            currentContactCount = currentCount,
            isUpdate = false
        )

        return validatedNumberResult.fold(
            onSuccess = { cleanNumber ->
                // If setting as primary or this is the first contact, ensure single primary invariant
                val effectiveRole = if (role == ContactRole.PRIMARY || currentCount == 0) {
                    dao.clearPrimaryRole()
                    ContactRole.PRIMARY
                } else {
                    role
                }

                val id = dao.insert(
                    SosContactEntity(
                        name = name.trim(),
                        number = cleanNumber,
                        role = effectiveRole.name,
                        sortOrder = currentCount
                    )
                )
                Result.success(id)
            },
            onFailure = { error ->
                Result.failure(error)
            }
        )
    }

    override suspend fun updateContact(contact: SosContact): Result<Unit> {
        val count = dao.getCount()
        val validatedResult = ContactValidator.validateContact(
            name = contact.name,
            number = contact.number,
            currentContactCount = count,
            isUpdate = true
        )

        return validatedResult.fold(
            onSuccess = { cleanNumber ->
                if (contact.role == ContactRole.PRIMARY) {
                    dao.clearPrimaryRole()
                }
                dao.update(
                    SosContactEntity(
                        id = contact.id,
                        name = contact.name.trim(),
                        number = cleanNumber,
                        role = contact.role.name,
                        sortOrder = contact.sortOrder
                    )
                )
                Result.success(Unit)
            },
            onFailure = { error ->
                Result.failure(error)
            }
        )
    }

    override suspend fun deleteContact(id: Long): Result<Unit> {
        val target = dao.getAllList().find { it.id == id }
        dao.deleteById(id)

        // If we deleted the primary contact, promote the next available contact to primary
        if (target?.role == ContactRole.PRIMARY.name) {
            val remaining = dao.getAllList()
            if (remaining.isNotEmpty()) {
                dao.setPrimaryRole(remaining.first().id)
            }
        }
        return Result.success(Unit)
    }

    override suspend fun setPrimaryContact(id: Long): Result<Unit> {
        dao.clearPrimaryRole()
        dao.setPrimaryRole(id)
        return Result.success(Unit)
    }

    private fun SosContactEntity.toDomain(): SosContact {
        val domainRole = try {
            ContactRole.valueOf(role)
        } catch (_: Exception) {
            ContactRole.BACKUP
        }
        return SosContact(
            id = id,
            name = name,
            number = number,
            role = domainRole,
            sortOrder = sortOrder
        )
    }
}
