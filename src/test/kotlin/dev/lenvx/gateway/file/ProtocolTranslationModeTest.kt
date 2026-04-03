package dev.lenvx.gateway.file

import dev.lenvx.gateway.file.ServerProperties.ProtocolTranslationMode
import kotlin.test.Test
import kotlin.test.assertEquals

class ProtocolTranslationModeTest {

    @Test
    fun defaultsToBuiltinForNullAndEmpty() {
        assertEquals(ProtocolTranslationMode.BUILTIN, ProtocolTranslationMode.fromProperty(null))
        assertEquals(ProtocolTranslationMode.BUILTIN, ProtocolTranslationMode.fromProperty(""))
    }

    @Test
    fun mapsLegacyExternalModesToBuiltin() {
        assertEquals(ProtocolTranslationMode.BUILTIN, ProtocolTranslationMode.fromProperty("plugin"))
        assertEquals(ProtocolTranslationMode.BUILTIN, ProtocolTranslationMode.fromProperty("external"))
        assertEquals(ProtocolTranslationMode.BUILTIN, ProtocolTranslationMode.fromProperty("vialimbo"))
        assertEquals(ProtocolTranslationMode.BUILTIN, ProtocolTranslationMode.fromProperty("viaproxy"))
    }

    @Test
    fun keepsStrictOffMode() {
        assertEquals(ProtocolTranslationMode.OFF, ProtocolTranslationMode.fromProperty("off"))
        assertEquals(ProtocolTranslationMode.OFF, ProtocolTranslationMode.fromProperty("disabled"))
        assertEquals(ProtocolTranslationMode.OFF, ProtocolTranslationMode.fromProperty("native"))
    }
}

