package me.gabber235.typewriter.interaction

import me.gabber235.typewriter.entry.Query
import me.gabber235.typewriter.entry.dialogue.DialogueSequence
import me.gabber235.typewriter.entry.entries.*
import me.gabber235.typewriter.entry.entries.SystemTrigger.*
import me.gabber235.typewriter.entry.entries.cinematic.CinematicSequence
import me.gabber235.typewriter.entry.matches
import org.bukkit.entity.Player
import java.time.Duration

class Interaction(val player: Player) {
	private var dialogue: DialogueSequence? = null
	private var cinematic: CinematicSequence? = null
	val hasDialogue: Boolean
		get() = dialogue != null

	val hasCinematic: Boolean
		get() = cinematic != null

	fun tick(delta: Duration) {
		dialogue?.tick()
		cinematic?.tick(delta)
	}

	/**
	 * Handles an event.
	 * All [SystemTrigger]'s are handled by the plugin itself.
	 */
	fun onEvent(event: Event) {
		triggerActions(event)
		handleDialogue(event)
		handleCinematic(event)
	}

	/**
	 * Triggers all actions that are registered for the given event.
	 *
	 * @param event The event that should be triggered
	 */
	private fun triggerActions(event: Event) {
		// Trigger all actions
		val actions = Query.findWhere<ActionEntry> { it in event && it.criteria.matches(event.player.uniqueId) }
		actions.forEach { action ->
			action.execute(event.player)
		}
		val newTriggers = actions.flatMap { it.triggers }
			.map { EntryTrigger(it) }
			.filter { it !in event } // Stops infinite loops
		if (newTriggers.isNotEmpty()) {
			InteractionHandler.triggerEvent(Event(event.player, newTriggers))
		}
	}

	private fun handleDialogue(event: Event) {
		if (DIALOGUE_NEXT in event) {
			onDialogueNext()
			return
		}
		if (DIALOGUE_END in event) {
			dialogue?.end()
			dialogue = null
			return
		}

		// Try to trigger new/next dialogue
		tryTriggerNextDialogue(event)
	}


	/**
	 * Tries to trigger a new dialogue.
	 * If no dialogue can be found, it will end the dialogue sequence.
	 */
	private fun tryTriggerNextDialogue(event: Event) {
		val nextDialogue = Query.findWhere<DialogueEntry> { it in event }
			.sortedByDescending { it.criteria.size }
			.firstOrNull { it.criteria.matches(event.player.uniqueId) }

		if (nextDialogue != null) {
			// If there is no sequence yet, start a new one
			if (dialogue == null) {
				dialogue = DialogueSequence(player, nextDialogue)
				dialogue?.init()
			} else {
				// If there is a sequence, trigger the next dialogue
				dialogue?.next(nextDialogue)
			}
		} else if (dialogue?.isActive == false) {
			// If there is no next dialogue and the sequence isn't active anymore, we can end the sequence
			InteractionHandler.triggerEvent(Event(player, DIALOGUE_END))
		}
	}

	/**
	 * Called when the player clicks the next button.
	 * If there is no next dialogue, the sequence will be ended.
	 */
	private fun onDialogueNext() {
		val dialog = dialogue ?: return
		if (dialog.triggers.isEmpty()) {
			InteractionHandler.triggerEvent(Event(player, DIALOGUE_END))
			return
		}
		val triggers = dialog.triggers.map { EntryTrigger(it) }
		InteractionHandler.triggerEvent(Event(player, triggers))
		return
	}

	private fun handleCinematic(event: Event) {
		if (CINEMATIC_END in event) {
			cinematic?.end()
			cinematic = null
			return
		}

		val entries = Query.findWhere<CinematicEntry> { it in event && it.criteria.matches(event.player.uniqueId) }
		if (entries.isNotEmpty() && !hasCinematic) {
			cinematic = CinematicSequence(player)
		}
		entries.forEach { entry ->
			cinematic?.add(entry)
		}
	}

	fun end() {
		dialogue?.end()
		cinematic?.end()
	}
}