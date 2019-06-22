package ru.spbstu

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.types.int
import kotlinx.coroutines.*
import ru.spbstu.map.GameMap
import ru.spbstu.map.Point
import ru.spbstu.parse.parseFile
import ru.spbstu.player.*
import ru.spbstu.sim.Command
import ru.spbstu.sim.Robot
import ru.spbstu.sim.SimFrame
import ru.spbstu.sim.Simulator
import ru.spbstu.wheels.*
import ru.spbstu.util.log
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
            val paths = listOf(::astarBot, ::smarterAstarBot, ::evenSmarterAstarBot,
                    ::priorityAstarBot,::smarterPriorityAstarBot, ::evenSmarterPriorityAstarBot,
                    ::superSmarterAstarBot)
                    .map {
                        val map = GameMap(data)
                        val sim = Simulator(Robot(data.initial), map)

                        it.name to async { handleMapSingle(sim, it) }
                    }

            while (paths.any { it.second.isActive }) {
                yield()
            }

            paths.map { it.first to it.second.await() }.minBy { it.second.count() }
        }

        File(File(solFolder), File(file.replace(".desc", ".sol")).name).apply { parentFile.mkdirs() }.bufferedWriter().use {
            log.debug("Solution for file $file: ${path?.second?.joinToString("")}")
            log.debug("Solution score for file $file: ${path?.second?.count()}")
            log.debug("Best solution for file $file is ${path?.first}")
            it.write(path?.second?.map { it.toString() }?.joinToString(""))
        }
    }

    suspend fun handleMapSingle(isim: Simulator, bot: (MutableRef<Simulator>, Set<Point>) -> Sequence<Command>): Sequence<Command> {
        val mutSim = ref(isim)
        var sim by mutSim
        val path = bot(mutSim, sim.gameMap.cells.keys).memoize()

        if (gui) {
            val frame = SimFrame(guiCellSize) { mutSim.value }
            for (command in path) {
                sim = sim.apply(command)
                delay((1000.0 / speed).toLong())
                frame.repaint()
            }
            delay(5000)
            frame.dispose()
        } else {
            for (command in path) {
                sim = sim.apply(command)
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
