package dev.lenvx.gateway.network

import dev.lenvx.gateway.file.ServerProperties.ProtocolTranslationMode
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ClientConnectionProtocolGateTest {

    @Test
    fun builtInTranslationAllowsMismatchedProtocols() {
        assertTrue(ClientConnection.isTranslationEnabled(ProtocolTranslationMode.BUILTIN))
        assertTrue(ClientConnection.isProtocolSupported(clientProtocol = 765, serverProtocol = 774, mode = ProtocolTranslationMode.BUILTIN))
    }

    @Test
    fun strictModeRequiresExactProtocolMatch() {
        assertFalse(ClientConnection.isTranslationEnabled(ProtocolTranslationMode.OFF))
        assertTrue(ClientConnection.isProtocolSupported(clientProtocol = 774, serverProtocol = 774, mode = ProtocolTranslationMode.OFF))
        assertFalse(ClientConnection.isProtocolSupported(clientProtocol = 765, serverProtocol = 774, mode = ProtocolTranslationMode.OFF))
    }
}

