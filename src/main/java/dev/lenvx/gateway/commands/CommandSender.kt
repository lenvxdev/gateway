@file:Suppress("removal")

package dev.lenvx.gateway.commands

import net.kyori.adventure.audience.Audience
import net.kyori.adventure.audience.MessageType
import net.kyori.adventure.bossbar.BossBar
import net.kyori.adventure.identity.Identity
import net.kyori.adventure.inventory.Book
import net.kyori.adventure.sound.Sound
import net.kyori.adventure.sound.SoundStop
import net.kyori.adventure.text.Component
import net.kyori.adventure.title.TitlePart
import net.md_5.bungee.api.chat.BaseComponent
import java.util.UUID

interface CommandSender : Audience {
    fun sendMessage(component: Array<BaseComponent>, uuid: UUID)
    fun sendMessage(component: BaseComponent, uuid: UUID)
    fun sendMessage(message: String, uuid: UUID)
    fun sendMessage(component: Array<BaseComponent>)
    fun sendMessage(component: BaseComponent)
    fun sendMessage(message: String)
    fun hasPermission(permission: String): Boolean
    val name: String

    override fun sendMessage(source: Identity, message: Component, type: MessageType)
    override fun openBook(book: Book)
    override fun stopSound(stop: SoundStop)
    override fun playSound(sound: Sound, emitter: Sound.Emitter)
    override fun playSound(sound: Sound, x: Double, y: Double, z: Double)
    override fun playSound(sound: Sound)
    override fun sendActionBar(message: Component)
    override fun sendPlayerListHeaderAndFooter(header: Component, footer: Component)
    override fun <T : Any> sendTitlePart(part: TitlePart<T>, value: T)
    override fun clearTitle()
    override fun resetTitle()
    override fun showBossBar(bar: BossBar)
    override fun hideBossBar(bar: BossBar)
}



