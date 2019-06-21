package ru.spbstu

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.types.int
import kotlinx.coroutines.*
import ru.spbstu.map.GameMap
import ru.spbstu.parse.parseFile
import ru.spbstu.player.astarBot
import ru.spbstu.player.evenSmarterAstarBot
import ru.spbstu.player.smarterAstarBot
import ru.spbstu.sim.Command
import ru.spbstu.sim.Robot
import ru.spbstu.sim.Simulator
import ru.spbstu.wheels.memoize
import java.io.File

object Main : CliktCommand() {
    val map: String by option(help = "Map to run on").default("all")
    val gui: Boolean by option().flag(default = false)
    val guiCellSize: Int by option().int().default(10)
    val speed: Int by option().int().default(10)
    val solFolder: String by option().default("solutions")
    val threads: Int by option().int().default(1)

//    val count: Int by option(help = "Number of greetings").int().default(1)
//    val name: String? by option(help = "The person to greet")

    fun handleMap(file: String) {
        val data = File(file).let { parseFile(it.name, it.readText()) }

        val path = runBlocking(newFixedThreadPoolContext(threads, "Pool")) {
            val paths = listOf(::astarBot, ::smarterAstarBot, ::evenSmarterAstarBot)
                    .map {
                        val map = GameMap(data)
                        val sim = Simulator(Robot(data.initial), map)

                        async { handleMapSingle(sim, it) }
                    }

            while (paths.any { it.isActive }) {
                yield()
            }

            paths.map { it.await() }.minBy { it.count() }
        }

        File(File(solFolder), File(file.replace(".desc", ".sol")).name).apply { parentFile.mkdirs() }.bufferedWriter().use {
            println("Solution for file $file: ${path?.joinToString("")}")
            println("Solution score for file $file: ${path?.count()}")
            it.write(path?.map { it.toString() }?.joinToString(""))
        }
    }

    suspend fun handleMapSingle(sim: Simulator, bot: (Simulator) -> Sequence<Command>): Sequence<Command> {
        val path = bot(sim).memoize()

        if (gui) {
            val frame = sim.display(guiCellSize)
            for(command in path) {
                sim.apply(command)
                Thread.sleep((1000.0 / speed).toLong())
                frame.repaint()
            }
            delay(5000)
            frame.dispose()
        } else {
            for(command in path) {
                sim.apply(command)
            }
        }

        return path
    }

    override fun run() {
        if (map != "all") {
            handleMap("docs/tasks/prob-$map.desc")
        } else {
            runBlocking(newFixedThreadPoolContext(threads, "Pool")) {
                val asyncs = File("docs/tasks").walkTopDown().toList().filter { it.extension == "desc" }.map {
                    launch { handleMap(it.absolutePath) }
                }

                while (asyncs.any { it.isActive }) {
                    yield()
                }
            }
        }
    }
}

fun main(args: Array<String>) = Main.main(args)
