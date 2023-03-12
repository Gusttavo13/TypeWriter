package com.caleb.typewriter.griefdefender.entries.event

import com.caleb.typewriter.griefdefender.GriefDefenderAdapter
import lirand.api.extensions.server.server
import me.gabber235.typewriter.adapters.Colors
import me.gabber235.typewriter.adapters.Entry
import me.gabber235.typewriter.adapters.modifiers.Help
import me.gabber235.typewriter.entry.*
import me.gabber235.typewriter.entry.entries.EventEntry
import me.gabber235.typewriter.utils.Icons
import java.util.*


@Entry("on_enter_gd_region", "When a player enters a GriefDefender region", Colors.YELLOW, Icons.SQUARE_CHECK)
class EnterRegionEventEntry(
	override val id: String = "",
	override val name: String = "",
	override val triggers: List<String> = emptyList(),
	@Help("The region to check for.")
	val region: String = "",
) : EventEntry


@EntryListener(EnterRegionEventEntry::class)
fun onEnterRegions(event: GriefDefenderAdapter.RegionEnterEvent, query: Query<EnterRegionEventEntry>) {
	val player = server.getPlayer(event.getPlayerUUID()) ?: return
	query findWhere { it.region == event.getClaim().uniqueId.toString() } startDialogueWithOrNextDialogue  player
}





