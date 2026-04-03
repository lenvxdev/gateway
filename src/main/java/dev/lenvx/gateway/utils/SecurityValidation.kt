package dev.lenvx.gateway.utils

object SecurityValidation {

    private val disallowedControlChars = Regex("[\\u0000-\\u001F&&[^\\u0009]]")

    @JvmStatic
    fun isChatMessageAllowed(message: String, maxLength: Int): Boolean {
        return message.length <= maxLength
    }

    @JvmStatic
    fun isCommandAllowed(command: String, maxLength: Int): Boolean {
        return command.length <= maxLength && !command.contains(disallowedControlChars)
    }

    @JvmStatic
    fun isPluginPayloadAllowed(payloadSize: Int, maxBytes: Int): Boolean {
        return payloadSize in 0..maxBytes
    }

    @JvmStatic
    fun areCommandArgsSafe(args: Array<String>, maxArgs: Int, maxArgLength: Int): Boolean {
        if (args.isEmpty() || args.size > maxArgs) {
            return false
        }
        return args.all { arg ->
            arg.isNotBlank() && arg.length <= maxArgLength && !arg.contains(disallowedControlChars)
        }
    }
}
