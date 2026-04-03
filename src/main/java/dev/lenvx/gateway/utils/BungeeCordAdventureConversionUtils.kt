package dev.lenvx.gateway.utils

import net.kyori.adventure.text.Component
import net.kyori.adventure.text.serializer.gson.GsonComponentSerializer
import net.md_5.bungee.api.chat.BaseComponent
import net.md_5.bungee.chat.ComponentSerializer

object BungeeCordAdventureConversionUtils {

	@JvmStatic
	fun toComponent(vararg components: BaseComponent): Component {
		return GsonComponentSerializer.gson().deserialize(ComponentSerializer.toString(*components))
	}

	@JvmStatic
	fun toBaseComponents(component: Component): Array<BaseComponent> {
		return ComponentSerializer.parse(GsonComponentSerializer.gson().serialize(component))
	}

}
