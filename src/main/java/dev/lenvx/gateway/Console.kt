@file:Suppress("removal")

package dev.lenvx.gateway

import dev.lenvx.gateway.commands.CommandSender
import dev.lenvx.gateway.consolegui.ConsoleTextOutput
import dev.lenvx.gateway.utils.CustomStringUtils
import net.kyori.adventure.audience.MessageType
import net.kyori.adventure.bossbar.BossBar
import net.kyori.adventure.identity.Identity
import net.kyori.adventure.inventory.Book
import net.kyori.adventure.sound.Sound
import net.kyori.adventure.sound.Sound.Emitter
import net.kyori.adventure.sound.SoundStop
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer
import net.kyori.adventure.title.TitlePart
import net.md_5.bungee.api.ChatColor
import net.md_5.bungee.api.chat.BaseComponent
import org.fusesource.jansi.Ansi
import org.fusesource.jansi.Ansi.Attribute
import org.jline.reader.Candidate
import org.jline.reader.EndOfFileException
import org.jline.reader.LineReader
import org.jline.reader.LineReader.SuggestionType
import org.jline.reader.LineReaderBuilder
import org.jline.reader.UserInterruptException
import org.jline.terminal.Size
import org.jline.terminal.Terminal
import org.jline.terminal.TerminalBuilder
import java.io.*
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import kotlin.system.exitProcess

open class Console @Throws(IOException::class) constructor(`in`: InputStream?, out: PrintStream?, err: PrintStream?) :
    dev.lenvx.gateway.commands.CommandSender {

    private val terminal: Terminal
    private val tabReader: LineReader

    private val inputStream: InputStream?
    @Suppress("unused")
    private val printStreamOut: PrintStream
    @Suppress("unused")
    private val printStreamErr: PrintStream
    val logs: PrintStream
    private val commandExecutor: ExecutorService = Executors.newVirtualThreadPerTaskExecutor()

    init {
        val fileName = SimpleDateFormat("yyyy'-'MM'-'dd'_'HH'-'mm'-'ss'_'zzz'.log'").format(Date()).replace(":", "")
        val dir = File("logs")
        dir.mkdirs()
        val logsFile = File(dir, fileName)
        logs = PrintStream(Files.newOutputStream(logsFile.toPath()), true, StandardCharsets.UTF_8.toString())

        if (`in` != null) {
            System.setIn(`in`)
            inputStream = System.`in`
        } else {
            inputStream = null
        }

        System.setOut(ConsoleOutputStream(this, out ?: PrintStream(object : OutputStream() {
            override fun write(b: Int) {
            }
        }), logs))
        printStreamOut = System.`out`

        System.setErr(ConsoleErrorStream(this, err ?: PrintStream(object : OutputStream() {
            override fun write(b: Int) {
            }
        }), logs))
        printStreamErr = System.`err`

        terminal = TerminalBuilder.builder().streams(inputStream, out).jansi(true).build()
        tabReader = LineReaderBuilder.builder().terminal(terminal).completer { _, line, candidates ->
            val args = CustomStringUtils.splitStringToArgs(line.line())
            val tab = Gateway.instance!!.pluginManager.getTabOptions(Gateway.instance!!.console, args)
            for (each in tab) {
                candidates.add(Candidate(each))
            }
        }.build()
        tabReader.unsetOpt(LineReader.Option.INSERT_TAB)
        tabReader.setVariable(LineReader.SECONDARY_PROMPT_PATTERN, "")
        tabReader.setAutosuggestion(SuggestionType.NONE)

        if (terminal.width <= 0 || terminal.height <= 0) {
            terminal.size = Size(80, 24)
        }
    }

    override val name: String
        get() = CONSOLE

    override fun hasPermission(permission: String): Boolean {
        return Gateway.instance!!.permissionsManager.hasPermission(this, permission)
    }

    @Deprecated("")
    override fun sendMessage(component: BaseComponent, uuid: UUID) {
        sendMessage(component)
    }

    @Deprecated("")
    override fun sendMessage(component: Array<BaseComponent>, uuid: UUID) {
        sendMessage(component)
    }

    override fun sendMessage(message: String, uuid: UUID) {
        sendMessage(message)
    }

    @Deprecated("")
    override fun sendMessage(component: BaseComponent) {
        sendMessage(arrayOf(component))
    }

    @Deprecated("")
    override fun sendMessage(component: Array<BaseComponent>) {
        sendMessage(component.joinToString("") { it.toLegacyText() })
    }

    override fun sendMessage(source: Identity, message: Component, type: MessageType) {
        sendMessage(PlainTextComponentSerializer.plainText().serialize(message))
    }

    override fun openBook(book: Book) {
    }

    override fun stopSound(stop: SoundStop) {
    }

    override fun playSound(sound: Sound, emitter: Emitter) {
    }

    override fun playSound(sound: Sound, x: Double, y: Double, z: Double) {
    }

    override fun playSound(sound: Sound) {
    }

    override fun sendActionBar(message: Component) {
    }

    override fun sendPlayerListHeaderAndFooter(header: Component, footer: Component) {
    }

    override fun <T : Any> sendTitlePart(part: TitlePart<T>, value: T) {
    }

    override fun clearTitle() {
    }

    override fun resetTitle() {
    }

    override fun showBossBar(bar: BossBar) {
    }

    override fun hideBossBar(bar: BossBar) {
    }

    override fun sendMessage(message: String) {
        stashLine()
        val date = SimpleDateFormat("HH':'mm':'ss").format(Date())
        ConsoleTextOutput.appendText(ChatColor.stripColor("[$date Info] $message"), true)
        logs.println(ChatColor.stripColor("[$date Info] $message"))
        terminal.writer().append("[").append(date).append(" Info] ").append(translateToConsole(sanitizeForTerminal(message))).append("\n")
        terminal.writer().flush()
        unstashLine()
    }

    fun run() {
        if (inputStream == null) {
            return
        }
        while (true) {
            try {
                val command = tabReader.readLine(PROMPT).trim()
                if (command.isNotEmpty()) {
                    val input = CustomStringUtils.splitStringToArgs(command)
                    commandExecutor.submit { Gateway.instance!!.dispatchCommand(this, *input) }
                }
            } catch (e: UserInterruptException) {
                exitProcess(0)
            } catch (e: EndOfFileException) {
                break
            }
        }
    }

    fun stashLine() {
        try {
            tabReader.callWidget(LineReader.CLEAR)
        } catch (ignore: Exception) {
        }
    }

    fun unstashLine() {
        try {
            tabReader.callWidget(LineReader.REDRAW_LINE)
            tabReader.callWidget(LineReader.REDISPLAY)
            tabReader.terminal.writer().flush()
        } catch (ignore: Exception) {
        }
    }

    companion object {
        val REPLACEMENTS: MutableMap<ChatColor, String> = HashMap()
        private const val CONSOLE = "CONSOLE"
        private const val PROMPT = "> "
        const val ERROR_RED = "\u001B[31;1m"
        const val RESET_COLOR = "\u001B[0m"

        init {
            REPLACEMENTS[ChatColor.BLACK] = Ansi.ansi().a(Attribute.RESET).fg(Ansi.Color.BLACK).boldOff().toString()
            REPLACEMENTS[ChatColor.DARK_BLUE] = Ansi.ansi().a(Attribute.RESET).fg(Ansi.Color.BLUE).boldOff().toString()
            REPLACEMENTS[ChatColor.DARK_GREEN] = Ansi.ansi().a(Attribute.RESET).fg(Ansi.Color.GREEN).boldOff().toString()
            REPLACEMENTS[ChatColor.DARK_AQUA] = Ansi.ansi().a(Attribute.RESET).fg(Ansi.Color.CYAN).boldOff().toString()
            REPLACEMENTS[ChatColor.DARK_RED] = Ansi.ansi().a(Attribute.RESET).fg(Ansi.Color.RED).boldOff().toString()
            REPLACEMENTS[ChatColor.DARK_PURPLE] = Ansi.ansi().a(Attribute.RESET).fg(Ansi.Color.MAGENTA).boldOff().toString()
            REPLACEMENTS[ChatColor.GOLD] = Ansi.ansi().a(Attribute.RESET).fg(Ansi.Color.YELLOW).boldOff().toString()
            REPLACEMENTS[ChatColor.GRAY] = Ansi.ansi().a(Attribute.RESET).fg(Ansi.Color.WHITE).boldOff().toString()
            REPLACEMENTS[ChatColor.DARK_GRAY] = Ansi.ansi().a(Attribute.RESET).fg(Ansi.Color.BLACK).bold().toString()
            REPLACEMENTS[ChatColor.BLUE] = Ansi.ansi().a(Attribute.RESET).fg(Ansi.Color.BLUE).bold().toString()
            REPLACEMENTS[ChatColor.GREEN] = Ansi.ansi().a(Attribute.RESET).fg(Ansi.Color.GREEN).bold().toString()
            REPLACEMENTS[ChatColor.AQUA] = Ansi.ansi().a(Attribute.RESET).fg(Ansi.Color.CYAN).bold().toString()
            REPLACEMENTS[ChatColor.RED] = Ansi.ansi().a(Attribute.RESET).fg(Ansi.Color.RED).bold().toString()
            REPLACEMENTS[ChatColor.LIGHT_PURPLE] = Ansi.ansi().a(Attribute.RESET).fg(Ansi.Color.MAGENTA).bold().toString()
            REPLACEMENTS[ChatColor.YELLOW] = Ansi.ansi().a(Attribute.RESET).fg(Ansi.Color.YELLOW).bold().toString()
            REPLACEMENTS[ChatColor.WHITE] = Ansi.ansi().a(Attribute.RESET).fg(Ansi.Color.WHITE).bold().toString()
            REPLACEMENTS[ChatColor.MAGIC] = Ansi.ansi().a(Attribute.BLINK_SLOW).toString()
            REPLACEMENTS[ChatColor.BOLD] = Ansi.ansi().a(Attribute.UNDERLINE_DOUBLE).toString()
            REPLACEMENTS[ChatColor.STRIKETHROUGH] = Ansi.ansi().a(Attribute.STRIKETHROUGH_ON).toString()
            REPLACEMENTS[ChatColor.UNDERLINE] = Ansi.ansi().a(Attribute.UNDERLINE).toString()
            REPLACEMENTS[ChatColor.ITALIC] = Ansi.ansi().a(Attribute.ITALIC).toString()
            REPLACEMENTS[ChatColor.RESET] = Ansi.ansi().a(Attribute.RESET).toString()
        }

        private fun sanitizeForTerminal(str: String): String {
            return str.replace("\\u001B\\[[;\\d]*m".toRegex(), "").replace("[\\p{Cntrl}&&[^\r\n\t]]".toRegex(), "")
        }

        @JvmStatic
        fun translateToConsole(str: String): String {
            var result = str
            for ((key, value) in REPLACEMENTS) {
                result = result.replace(key.toString(), value)
            }
            result = result.replace(("(?i)" + ChatColor.COLOR_CHAR + "x(" + ChatColor.COLOR_CHAR + "[0-9a-f]){6}").toRegex(), "")
            return result + RESET_COLOR
        }
    }

    class ConsoleOutputStream(private val console: Console, `out`: OutputStream, private val logs: PrintStream) : PrintStream(`out`) {

        override fun printf(l: Locale?, format: String, vararg args: Any?): PrintStream {
            console.stashLine()
            val date = SimpleDateFormat("HH':'mm':'ss").format(Date())
            ConsoleTextOutput.appendText(ChatColor.stripColor(String.format(l, "[$date Info]$format", *args)))
            logs.printf(l, ChatColor.stripColor("[$date Info]$format"), *args)
            val stream = super.printf(l, translateToConsole("[$date Info]$format"), *args)
            console.unstashLine()
            return stream
        }

        override fun printf(format: String, vararg args: Any?): PrintStream {
            console.stashLine()
            val date = SimpleDateFormat("HH':'mm':'ss").format(Date())
            ConsoleTextOutput.appendText(ChatColor.stripColor(String.format("[$date Info]$format", *args)))
            logs.printf(ChatColor.stripColor("[$date Info]$format"), *args)
            val stream = super.printf(ChatColor.stripColor("[$date Info]$format"), *args)
            console.unstashLine()
            return stream
        }

        override fun println() {
            console.stashLine()
            val date = SimpleDateFormat("HH':'mm':'ss").format(Date())
            ConsoleTextOutput.appendText(ChatColor.stripColor("[$date Info]"), true)
            logs.println(ChatColor.stripColor("[$date Info]"))
            super.println(ChatColor.stripColor("[$date Info]"))
            console.unstashLine()
        }

        override fun println(x: Boolean) {
            console.stashLine()
            val date = SimpleDateFormat("HH':'mm':'ss").format(Date())
            ConsoleTextOutput.appendText(ChatColor.stripColor("[$date Info] $x"), true)
            logs.println(ChatColor.stripColor("[$date Info]$x"))
            super.println(ChatColor.stripColor("[$date Info]$x"))
            console.unstashLine()
        }

        override fun println(x: Char) {
            console.stashLine()
            val date = SimpleDateFormat("HH':'mm':'ss").format(Date())
            ConsoleTextOutput.appendText(ChatColor.stripColor("[$date Info] $x"), true)
            logs.println(ChatColor.stripColor("[$date Info]$x"))
            super.println(ChatColor.stripColor("[$date Info]$x"))
            console.unstashLine()
        }

        override fun println(x: CharArray) {
            console.stashLine()
            val date = SimpleDateFormat("HH':'mm':'ss").format(Date())
            ConsoleTextOutput.appendText(ChatColor.stripColor("[$date Info] " + String(x)), true)
            logs.println(ChatColor.stripColor("[$date Info]" + String(x)))
            super.println(ChatColor.stripColor("[$date Info]" + String(x)))
            console.unstashLine()
        }

        override fun println(x: Double) {
            console.stashLine()
            val date = SimpleDateFormat("HH':'mm':'ss").format(Date())
            ConsoleTextOutput.appendText(ChatColor.stripColor("[$date Info] $x"), true)
            logs.println(ChatColor.stripColor("[$date Info]$x"))
            super.println(ChatColor.stripColor("[$date Info]$x"))
            console.unstashLine()
        }

        override fun println(x: Float) {
            console.stashLine()
            val date = SimpleDateFormat("HH':'mm':'ss").format(Date())
            ConsoleTextOutput.appendText(ChatColor.stripColor("[$date Info] $x"), true)
            logs.println(ChatColor.stripColor("[$date Info]$x"))
            super.println(ChatColor.stripColor("[$date Info]$x"))
            console.unstashLine()
        }

        override fun println(x: Int) {
            console.stashLine()
            val date = SimpleDateFormat("HH':'mm':'ss").format(Date())
            ConsoleTextOutput.appendText(ChatColor.stripColor("[$date Info] $x"), true)
            logs.println(ChatColor.stripColor("[$date Info]$x"))
            super.println(ChatColor.stripColor("[$date Info]$x"))
            console.unstashLine()
        }

        override fun println(x: Long) {
            console.stashLine()
            val date = SimpleDateFormat("HH':'mm':'ss").format(Date())
            ConsoleTextOutput.appendText(ChatColor.stripColor("[$date Info] $x"), true)
            logs.println(ChatColor.stripColor("[$date Info]$x"))
            super.println(ChatColor.stripColor("[$date Info]$x"))
            console.unstashLine()
        }

        override fun println(x: Any?) {
            console.stashLine()
            val date = SimpleDateFormat("HH':'mm':'ss").format(Date())
            ConsoleTextOutput.appendText(ChatColor.stripColor("[$date Info] $x"), true)
            logs.println(ChatColor.stripColor("[$date Info]$x"))
            super.println(ChatColor.stripColor("[$date Info]$x"))
            console.unstashLine()
        }

        override fun println(string: String?) {
            console.stashLine()
            val date = SimpleDateFormat("HH':'mm':'ss").format(Date())
            ConsoleTextOutput.appendText(ChatColor.stripColor("[$date Info] $string"), true)
            logs.println(ChatColor.stripColor("[$date Info] $string"))
            super.println(ChatColor.stripColor("[$date Info] $string"))
            console.unstashLine()
        }
    }

    class ConsoleErrorStream(private val console: Console, `out`: OutputStream, private val logs: PrintStream) : PrintStream(`out`) {

        override fun printf(l: Locale?, format: String, vararg args: Any?): PrintStream {
            console.stashLine()
            val date = SimpleDateFormat("HH':'mm':'ss").format(Date())
            ConsoleTextOutput.appendText(ChatColor.stripColor(String.format(l, "[$date Error]$format", *args)))
            logs.printf(l, ChatColor.stripColor("[$date Error]$format"), *args)
            val stream = super.printf(l, ERROR_RED + ChatColor.stripColor("[$date Error]$format") + RESET_COLOR, *args)
            console.unstashLine()
            return stream
        }

        override fun printf(format: String, vararg args: Any?): PrintStream {
            console.stashLine()
            val date = SimpleDateFormat("HH':'mm':'ss").format(Date())
            ConsoleTextOutput.appendText(ChatColor.stripColor(String.format("[$date Error]$format", *args)))
            logs.printf(ChatColor.stripColor("[$date Error]$format"), *args)
            val stream = super.printf(ERROR_RED + ChatColor.stripColor("[$date Error]$format") + RESET_COLOR, *args)
            console.unstashLine()
            return stream
        }

        override fun println() {
            console.stashLine()
            val date = SimpleDateFormat("HH':'mm':'ss").format(Date())
            ConsoleTextOutput.appendText(ChatColor.stripColor("[$date Error]"), true)
            logs.println(ChatColor.stripColor("[$date Error]"))
            super.println(ERROR_RED + ChatColor.stripColor("[$date Error]") + RESET_COLOR)
            console.unstashLine()
        }

        override fun println(x: Boolean) {
            console.stashLine()
            val date = SimpleDateFormat("HH':'mm':'ss").format(Date())
            ConsoleTextOutput.appendText(ChatColor.stripColor("[$date Error] $x"), true)
            logs.println(ChatColor.stripColor("[$date Error]$x"))
            super.println(ERROR_RED + ChatColor.stripColor("[$date Error]$x") + RESET_COLOR)
            console.unstashLine()
        }

        override fun println(x: Char) {
            console.stashLine()
            val date = SimpleDateFormat("HH':'mm':'ss").format(Date())
            ConsoleTextOutput.appendText(ChatColor.stripColor("[$date Error] $x"), true)
            logs.println(ChatColor.stripColor("[$date Error]$x"))
            super.println(ERROR_RED + ChatColor.stripColor("[$date Error]$x") + RESET_COLOR)
            console.unstashLine()
        }

        override fun println(x: CharArray) {
            console.stashLine()
            val date = SimpleDateFormat("HH':'mm':'ss").format(Date())
            ConsoleTextOutput.appendText(ChatColor.stripColor("[$date Error] " + String(x)), true)
            logs.println(ChatColor.stripColor("[$date Error]" + String(x)))
            super.println(ERROR_RED + ChatColor.stripColor("[$date Error]" + String(x)) + RESET_COLOR)
            console.unstashLine()
        }

        override fun println(x: Double) {
            console.stashLine()
            val date = SimpleDateFormat("HH':'mm':'ss").format(Date())
            ConsoleTextOutput.appendText(ChatColor.stripColor("[$date Error] $x"), true)
            logs.println(ChatColor.stripColor("[$date Error]$x"))
            super.println(ERROR_RED + ChatColor.stripColor("[$date Error]$x") + RESET_COLOR)
            console.unstashLine()
        }

        override fun println(x: Float) {
            console.stashLine()
            val date = SimpleDateFormat("HH':'mm':'ss").format(Date())
            ConsoleTextOutput.appendText(ChatColor.stripColor("[$date Error] $x"), true)
            logs.println(ChatColor.stripColor("[$date Error]$x"))
            super.println(ERROR_RED + ChatColor.stripColor("[$date Error]$x") + RESET_COLOR)
            console.unstashLine()
        }

        override fun println(x: Int) {
            console.stashLine()
            val date = SimpleDateFormat("HH':'mm':'ss").format(Date())
            ConsoleTextOutput.appendText(ChatColor.stripColor("[$date Error] $x"), true)
            logs.println(ChatColor.stripColor("[$date Error]$x"))
            super.println(ERROR_RED + ChatColor.stripColor("[$date Error]$x") + RESET_COLOR)
            console.unstashLine()
        }

        override fun println(x: Long) {
            console.stashLine()
            val date = SimpleDateFormat("HH':'mm':'ss").format(Date())
            ConsoleTextOutput.appendText(ChatColor.stripColor("[$date Error] $x"), true)
            logs.println(ChatColor.stripColor("[$date Error]$x"))
            super.println(ERROR_RED + ChatColor.stripColor("[$date Error]$x") + RESET_COLOR)
            console.unstashLine()
        }

        override fun println(x: Any?) {
            console.stashLine()
            val date = SimpleDateFormat("HH':'mm':'ss").format(Date())
            ConsoleTextOutput.appendText(ChatColor.stripColor("[$date Error] $x"), true)
            logs.println(ChatColor.stripColor("[$date Error]$x"))
            super.println(ERROR_RED + ChatColor.stripColor("[$date Error]$x") + RESET_COLOR)
            console.unstashLine()
        }

        override fun println(string: String?) {
            console.stashLine()
            val date = SimpleDateFormat("HH':'mm':'ss").format(Date())
            ConsoleTextOutput.appendText(ChatColor.stripColor("[$date Error] $string"), true)
            logs.println(ChatColor.stripColor("[$date Error] $string"))
            super.println(ERROR_RED + ChatColor.stripColor("[$date Error] $string") + RESET_COLOR)
            console.unstashLine()
        }
    }
}





