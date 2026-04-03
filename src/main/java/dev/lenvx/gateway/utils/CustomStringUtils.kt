package dev.lenvx.gateway.utils

object CustomStringUtils {

	@JvmStatic
	@JvmOverloads
	fun arrayContains(compare: String, args: Array<String>, ignoreCase: Boolean = true): Boolean {
		return if (ignoreCase) {
			args.any { it.equals(compare, ignoreCase = true) }
		} else {
			args.any { it == compare }
		}
	}

	@JvmStatic
	fun splitStringToArgs(str: String): Array<String> {
		val tokens = mutableListOf<String>()
		val sb = StringBuilder()
		var insideQuote = false

		for (c in str.toCharArray()) {
			if (c == '"') {
				insideQuote = !insideQuote
			} else if (c == ' ' && !insideQuote) {
				if (sb.isNotEmpty()) {
					tokens.add(sb.toString())
				}
				sb.setLength(0)
			} else {
				sb.append(c)
			}
		}
		if (sb.isNotEmpty()) {
			tokens.add(sb.toString())
		}

		return tokens.toTypedArray()
	}

	@JvmStatic
	fun getIndexOfArg(str: String, ordinal: Int): Int {
		val sb = StringBuilder()
		var insideQuote = false
		var pos = 0
		var found = 0

		for (c in str.toCharArray()) {
			if (c == '"') {
				insideQuote = !insideQuote
			} else if (c == ' ' && !insideQuote) {
				if (sb.isNotEmpty()) {
					found++
				}
				sb.setLength(0)
			} else {
				sb.append(c)
			}
			if (found == ordinal) {
				return pos
			}
			pos++
		}

		return -1
	}

}

