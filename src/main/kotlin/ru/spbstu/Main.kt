package ru.spbstu

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.types.int
import kotlinx.coroutines.*
import kotlinx.coroutines.selects.select
import ru.spbstu.map.*
import ru.spbstu.parse.parseFile
import ru.spbstu.player.astarBot
import ru.spbstu.player.astarWalk
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
        val map = GameMap(data)
        val sim = Simulator(Robot(data.initial), map)

        val path = astarBot(sim).memoize()

        if (gui) {
            val frame = sim.display(guiCellSize)
            for(command in path) {
                sim.apply(command)
                Thread.sleep((1000.0 / speed).toLong())
                frame.repaint()
            }
        } else {
            for(command in path) {
                sim.apply(command)
            }
        }

        File(File(solFolder), File(file.replace(".desc", ".sol")).name).apply { parentFile.mkdirs() }.bufferedWriter().use {
            println("Solution for file $file: ${path.joinToString("")}")
            it.write(path.map { it.toString() }.joinToString(""))
        }

    }

    override fun run() {
        if (map != "all") {
            handleMap("docs/tasks/$map.desc")
        } else {
            runBlocking(newFixedThreadPoolContext(threads, "Pool")) {
                val asyncs = File("docs/tasks").walkTopDown().toList().filter { it.extension == "desc" }.map {
                    launch { handleMap(it.absolutePath) }
                }

                while(asyncs.any { it.isActive }) {
                    yield()
                }
            }
        }
    }
}

fun main(args: Array<String>) = Main.main(args)
