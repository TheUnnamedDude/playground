package no.playground

class Database {
    val sisteFor = mutableMapOf<String, MutableMap<String, Resultat>>()

    fun persister(macroCommandType: String, commandType: String, resultat: Resultat) {
        sisteFor.getOrPut(macroCommandType) {
            mutableMapOf()
        } += (commandType to resultat)
    }

    fun executedTypes(macroCommandType: String): Map<String, Resultat>? {
        return sisteFor[macroCommandType]
    }
}

class Akkumulator(
    private val løsninger: String
) {
    fun <T> løsning(parser: LøsningParser<T>): T {
        return parser.parse(løsninger)
    }
}

interface LøsningParser<T> {
    fun parse(input: String): T
}

interface Command {
    val type: String
    fun execute(database: Database, akkumulator: Akkumulator): Resultat
    fun resume(database: Database, akkumulator: Akkumulator): Resultat
}

abstract class MacroCommand : Command {
    internal abstract val subcommands: List<Command>
    override fun execute(database: Database, akkumulator: Akkumulator): Resultat {
        val gjenståendeKommandoer = gjennståendeKommandoer(database)
        for (command in gjenståendeKommandoer) {
            val resultat = command.execute(database, akkumulator)
            if (resultat.suspends) {
                persister(database, command, resultat)
                return Resultat.AvventerSystem
            }
        }
        return Resultat.Ok
    }

    override fun resume(database: Database, akkumulator: Akkumulator): Resultat {
        val resumedCommand = currentCommand(database)
        val resultat = resumedCommand.resume(database, akkumulator)
        persister(database, resumedCommand, resultat)
        if (resultat.suspends) {
            return Resultat.AvventerSystem
        }
        return execute(database, akkumulator)
    }

    fun persister(database: Database, command: Command, resultat: Resultat): Unit =
        database.persister(this.type, command.type, resultat)

    fun currentCommand(database: Database): Command = gjennståendeKommandoer(database).first()
    fun gjennståendeKommandoer(database: Database): List<Command> {
        val (currentCommand, resultat) = database.executedTypes(type)?.entries?.last() ?: return subcommands

        return if (resultat == Resultat.Ok) {
            subcommands.dropWhile { it.type != currentCommand }.drop(1)
        } else {
            subcommands.dropWhile { it.type != currentCommand }
        }
    }
}

enum class Resultat(val suspends: Boolean) {
    Ok(suspends = false),
    AvventerSystem(suspends = true),
    AvventerSaksbehandler(suspends = true),
    Failure(suspends = true)
}

