package ru.spbstu

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.types.int
import ru.spbstu.map.GameMap
import ru.spbstu.map.Status
import ru.spbstu.map.euclidDistance
import ru.spbstu.map.manhattanDistance
import ru.spbstu.parse.parseFile
import ru.spbstu.player.astarWalk
import ru.spbstu.sim.Robot
import ru.spbstu.sim.Simulator
import java.io.File

object Main : CliktCommand() {
    val map: String by option(help = "Map to run on").default("all")
    val gui: Boolean by option().flag(default = false)
    val guiCellSize: Int by option().int().default(10)
    val speed: Int by option().int().default(10)

//    val count: Int by option(help = "Number of greetings").int().default(1)
//    val name: String? by option(help = "The person to greet")

    override fun run() {
        if (map != "all") {
            val data = File("docs/part-1-initial/$map.desc").let { parseFile(it.name, it.readText()) }
            val map = GameMap(data)
            val sim = Simulator(Robot(data.initial), map)

            val path = sequence {
                while(true) {
                    val target = sim
                            .gameMap
                            .cells
                            .filter { it.value.status == Status.EMPTY }
                            .minBy { sim.currentRobot.pos.euclidDistance(it.key) }

                    target ?: break

                    val local = astarWalk(sim, target.key)
                    yieldAll(local)
                }
                println("Done!")
            }

            if (gui) {
                val frame = sim.display(guiCellSize)
                for(command in path) {
                    sim.apply(command)
                    Thread.sleep((1000.0 / speed).toLong())
                    frame.repaint()
                }
            }



        } else {
            val data = File("docs/part-1-initial").walkTopDown().filter { it.extension == "desc" }.map {
                parseFile(it.name, it.readText())
            }.toList()

            for (datum in data.take(50)) {
                val map = GameMap(datum)



                println(datum.name)
                println(map.toASCII())
            }
        }


    }
}

fun main(args: Array<String>) = Main.main(args)
