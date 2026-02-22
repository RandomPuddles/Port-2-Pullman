package com.port2pullman.app.model

/**
 * Intermediate model used during alarm creation / editing and
 * as the AI generation response. No [id] or [enabled] — those
 * are assigned when the alarm is persisted.
 */
data class AlarmDraft(
    val title: String = "",
    val rootCondition: CompositeCondition = CompositeCondition(Operator.AND),
    val readout: Boolean = false,
    val ring: Boolean = false,
    val triggerOnce: Boolean = false
)
