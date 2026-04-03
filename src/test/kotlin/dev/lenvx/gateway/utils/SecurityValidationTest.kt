package dev.lenvx.gateway.utils

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class SecurityValidationTest {

    @Test
    fun chatLengthValidation() {
        assertTrue(SecurityValidation.isChatMessageAllowed("hello", 5))
        assertFalse(SecurityValidation.isChatMessageAllowed("hello!", 5))
    }

    @Test
    fun commandValidationRejectsControlChars() {
        assertTrue(SecurityValidation.isCommandAllowed("spawn", 10))
        assertFalse(SecurityValidation.isCommandAllowed("sp\nawn", 10))
    }

    @Test
    fun commandArgsValidation() {
        assertTrue(SecurityValidation.areCommandArgsSafe(arrayOf("say", "hello"), maxArgs = 4, maxArgLength = 16))
        assertFalse(SecurityValidation.areCommandArgsSafe(arrayOf(""), maxArgs = 4, maxArgLength = 16))
        assertFalse(SecurityValidation.areCommandArgsSafe(arrayOf("a", "b", "c"), maxArgs = 2, maxArgLength = 16))
    }

    @Test
    fun pluginPayloadValidation() {
        assertTrue(SecurityValidation.isPluginPayloadAllowed(10, 10))
        assertFalse(SecurityValidation.isPluginPayloadAllowed(11, 10))
    }
}
