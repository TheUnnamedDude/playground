package no.playground

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class MacroCommandTest {
    @Test
    fun `command uten resume`() {
        val database = Database()
        val command = TestMacro()
        val akkumulator = Akkumulator("")
        assertEquals(Resultat.Ok, command.execute(database, akkumulator))
        assertEquals(1, command.subMacro.subSubCommand.executes)
        assertEquals(1, command.subCommand.executes)
    }

    @Test
    fun `command med resume`() {
        val database = Database()
        val command = ResumeMacro()
        assertEquals(Resultat.AvventerSystem, command.execute(database, Akkumulator("")))
        assertEquals(1, command.subMacro.subCommandResume.executes)
        assertEquals(0, command.subMacro.subCommandNoResume.executes)

        assertEquals(Resultat.Ok, command.resume(database, Akkumulator("")))
        assertEquals(1, command.subMacro.subCommandResume.resumes)
        assertEquals(1, command.subMacro.subCommandNoResume.executes)

    }

    @Test
    fun `executes commands only once when resumed`() {
        val database = Database()
        val command = UnpersistedGapResumeMacro()

        assertEquals(Resultat.AvventerSystem, command.execute(database, Akkumulator("")))

        command.resume(database, Akkumulator(""))
        assertEquals(1, command.subCommandNoResume.executes)
        assertEquals(1, command.subCommandResume.executes)
    }

    class UnpersistedGapResumeMacro : MacroCommand() {
        override val type = "ResumeMacro"
        val subCommandNoResume = CountingCommand("subCommandNoResume")
        val subCommandResume = CountingCommand("SubCommandResume", executeResult = Resultat.AvventerSystem)
        val subCommandNoResume2 = CountingCommand("subCommandNoResume2")

        override val subcommands = listOf(
            subCommandNoResume,
            subCommandResume,
            subCommandNoResume2
        )
    }


    class ResumeMacro : MacroCommand() {
        override val type = "ResumeMacro"
        val subMacro = ResumeSubMacro()
        override val subcommands = listOf(subMacro)

        class ResumeSubMacro : MacroCommand() {
            override val type = "ResumeSubMacro"
            val subCommandResume = CountingCommand("SubCommandResume", executeResult = Resultat.AvventerSystem)
            val subCommandNoResume = CountingCommand("subCommandNoResume")

            override val subcommands = listOf(subCommandResume, subCommandNoResume)
        }
    }

    class TestMacro : MacroCommand() {
        val subCommand = CountingCommand("SubCommand")
        val subMacro = SubMacro()

        override val type = "TestMacro"
        override val subcommands: List<Command> = listOf(
            subCommand, subMacro
        )

        class SubMacro : MacroCommand() {
            override val type = "SubMacro"

            val subSubCommand = CountingCommand("SubSubCommand")
            override val subcommands = listOf(subSubCommand)
        }
    }

    class CountingCommand(
        override val type: String,
        private val executeResult: Resultat = Resultat.Ok
    ) : Command {
        var executes: Int = 0
        var resumes: Int = 0

        override fun execute(database: Database, akkumulator: Akkumulator): Resultat {
            println("Execute $type")
            executes ++
            return executeResult
        }

        override fun resume(database: Database, akkumulator: Akkumulator): Resultat {
            println("Resume $type")
            resumes ++
            return Resultat.Ok
        }
    }
}
