package com.wakeproof.alarm.domain.snooze

import com.wakeproof.alarm.domain.model.Alarm
import com.wakeproof.alarm.domain.model.Difficulty
import com.wakeproof.alarm.domain.model.SnoozePolicy

/**
 * Decision made at the moment the snooze button is pressed (spec 4.1.2).
 * Everything the snoozed ring will need is fixed here so the ring in
 * +10 minutes uses exactly these parameters even if the alarm is edited
 * in between.
 */
data class SnoozeDecision(
    /** Absolute epoch ms when the snoozed ring must fire. */
    val targetAtMillis: Long,
    /** Task count of the snoozed session (1-4). */
    val sessionTaskCount: Int,
    /** Difficulty of the snoozed session. */
    val sessionDifficulty: Difficulty
)

/**
 * Pure snooze machine (spec 4.1.2):
 *
 * - snooze interval is a CONSTANT 10 minutes;
 * - the "once" limit applies to a single ringing event (срабатывание):
 *   consumed when the button is pressed, reset only by the next
 *   regular firing ([onRegularRingFired]);
 * - [SnoozePolicy.ONCE] repeats the ring with the same parameters;
 * - [SnoozePolicy.ONCE_HARDER] adds +1 task (max 4 per session); when
 *   4 tasks are already planned, the difficulty moves one level up
 *   (easy -> medium -> hardcore, hardcore is the ceiling);
 * - HARDCORE alarms can never snooze - the snooze policy setting is
 *   ignored (spec 4.2.1 hardcore definition).
 *
 * The snooze state lives in the alarm row (process-death and reboot
 * proof); this object holds all the transition rules and is fully
 * unit-testable without Android.
 */
object SnoozeMachine {

    /** Spec 4.1.2: fixed snooze interval. */
    const val INTERVAL_MINUTES = 10L
    const val INTERVAL_MS = INTERVAL_MINUTES * 60_000L

    /** Max cognitive tasks per session (spec 4.2.2), escalation ceiling. */
    const val MAX_SESSION_TASKS = Alarm.TASK_COUNT_MAX

    /** True when the ring screen may show the snooze button. */
    fun canSnooze(alarm: Alarm): Boolean =
        alarm.difficulty != Difficulty.HARDCORE &&
            alarm.snoozePolicy != SnoozePolicy.FORBIDDEN &&
            !alarm.isSnoozeUsed &&
            alarm.snoozeTargetAt <= 0L

    /**
     * Decision for the snooze button press. Caller is expected to check
     * [canSnooze] first.
     */
    fun decideSnooze(alarm: Alarm, nowMillis: Long): SnoozeDecision {
        val (taskCount, difficulty) = escalatedSessionParams(alarm)
        return SnoozeDecision(
            targetAtMillis = nowMillis + INTERVAL_MS,
            sessionTaskCount = taskCount,
            sessionDifficulty = difficulty
        )
    }

    /**
     * Session parameters of the snoozed ring (spec 4.1.2 escalation rule):
     * ONCE repeats unchanged; ONCE_HARDER adds one task up to the 4-task
     * cap, then raises the difficulty one level (hardcore is the last).
     */
    fun escalatedSessionParams(alarm: Alarm): Pair<Int, Difficulty> =
        when (alarm.snoozePolicy) {
            SnoozePolicy.FORBIDDEN, SnoozePolicy.ONCE ->
                alarm.taskCount to alarm.difficulty
            SnoozePolicy.ONCE_HARDER -> {
                val count = alarm.taskCount.coerceIn(Alarm.TASK_COUNT_MIN, MAX_SESSION_TASKS)
                if (count < MAX_SESSION_TASKS) {
                    (count + 1) to alarm.difficulty
                } else {
                    MAX_SESSION_TASKS to alarm.difficulty.nextHarder()
                }
            }
        }

    /**
     * The armed snoozed ring has fired: only the pending target is
     * consumed (so the scheduler returns to the regular occurrence),
     * while the limit, the session link AND the fixed session
     * parameters stay - the ringing UI reads the parameters right
     * after this transition, and a snoozed ring continues the SAME
     * morning session row (spec: one morning = one session; a snoozed
     * morning is never clean). Everything is cleared by the next
     * regular firing ([onRegularRingFired]).
     */
    fun onSnoozeRingFired(alarm: Alarm): Alarm = alarm.copy(
        snoozeTargetAt = 0L
    )

    /**
     * A new regular firing starts: the once-per-firing limit resets
     * (spec 4.1.2) and any stale snooze session state is cleared.
     */
    fun onRegularRingFired(alarm: Alarm): Alarm = alarm.copy(
        snoozeTargetAt = 0L,
        isSnoozeUsed = false,
        snoozeSessionTaskCount = 0,
        snoozeSessionDifficulty = null,
        snoozeSessionId = 0L
    )
}

/**
 * One difficulty level up for the snooze escalation ladder
 * (spec 4.1.2: easy -> medium -> hardcore, nothing above hardcore).
 */
fun Difficulty.nextHarder(): Difficulty = when (this) {
    Difficulty.EASY -> Difficulty.MEDIUM
    Difficulty.MEDIUM -> Difficulty.HARDCORE
    Difficulty.HARDCORE -> Difficulty.HARDCORE
}
