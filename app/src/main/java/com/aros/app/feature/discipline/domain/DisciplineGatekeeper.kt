package com.aros.app.feature.discipline.domain

import com.aros.app.feature.discipline.data.DisciplineDao
import com.aros.app.feature.discipline.data.DisciplineRuleEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

enum class DisciplineChallengeType(val label: String, val defaultFrictionSeconds: Int) {
    BREATH_GATE("10-Second Breath Gate", 10),
    PLEDGE_TYPING("Intention Pledge Verification", 15),
    ARITHMETIC("Mental Math Focus Barrier", 10)
}

data class DisciplineRule(
    val id: Long = 0,
    val name: String,
    val challengeType: DisciplineChallengeType,
    val frictionSeconds: Int,
    val targetAppOrHabit: String,
    val isActive: Boolean,
    val passCount: Int
)

class DisciplineGatekeeper(
    private val dao: DisciplineDao,
    scope: CoroutineScope
) {
    val rules: Flow<List<DisciplineRule>> = dao.getAllFlow().map { list ->
        list.map { entity ->
            DisciplineRule(
                id = entity.id,
                name = entity.name,
                challengeType = try {
                    DisciplineChallengeType.valueOf(entity.challengeType)
                } catch (_: Exception) {
                    DisciplineChallengeType.BREATH_GATE
                },
                frictionSeconds = entity.frictionSeconds,
                targetAppOrHabit = entity.targetAppOrHabit,
                isActive = entity.isActive,
                passCount = entity.passCount
            )
        }
    }

    init {
        scope.launch {
            if (dao.getAllList().isEmpty()) {
                seedDefaultDisciplineRules()
            }
        }
    }

    private suspend fun seedDefaultDisciplineRules() {
        val defaults = listOf(
            DisciplineRuleEntity(
                name = "Social Feed Friction",
                challengeType = DisciplineChallengeType.BREATH_GATE.name,
                frictionSeconds = 10,
                targetAppOrHabit = "Short-form video & algorithmic feeds",
                isActive = true,
                passCount = 14
            ),
            DisciplineRuleEntity(
                name = "Evening Commitment Gate",
                challengeType = DisciplineChallengeType.PLEDGE_TYPING.name,
                frictionSeconds = 15,
                targetAppOrHabit = "Late night aimless browsing",
                isActive = true,
                passCount = 8
            ),
            DisciplineRuleEntity(
                name = "Impulsive Purchase Pause",
                challengeType = DisciplineChallengeType.ARITHMETIC.name,
                frictionSeconds = 10,
                targetAppOrHabit = "Shopping & quick delivery apps",
                isActive = true,
                passCount = 5
            )
        )
        defaults.forEach { dao.insert(it) }
    }

    suspend fun toggleRuleActive(rule: DisciplineRule) {
        dao.update(
            DisciplineRuleEntity(
                id = rule.id,
                name = rule.name,
                challengeType = rule.challengeType.name,
                frictionSeconds = rule.frictionSeconds,
                targetAppOrHabit = rule.targetAppOrHabit,
                isActive = !rule.isActive,
                passCount = rule.passCount
            )
        )
    }

    suspend fun recordSuccessfulChallengePass(ruleId: Long) {
        dao.incrementPassCount(ruleId)
    }

    suspend fun addRule(
        name: String,
        challengeType: DisciplineChallengeType,
        frictionSeconds: Int,
        targetAppOrHabit: String
    ) {
        dao.insert(
            DisciplineRuleEntity(
                name = name.trim(),
                challengeType = challengeType.name,
                frictionSeconds = frictionSeconds,
                targetAppOrHabit = targetAppOrHabit.trim(),
                isActive = true,
                passCount = 0
            )
        )
    }

    suspend fun deleteRule(ruleId: Long) {
        dao.deleteById(ruleId)
    }
}
