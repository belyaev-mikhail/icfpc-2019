package ru.spbstu.generator

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.option
import org.organicdesign.fp.collections.PersistentHashMap
import ru.spbstu.map.Cell
import ru.spbstu.map.GameMap
import ru.spbstu.map.Point
import ru.spbstu.map.dumpMap
import java.io.File

object Main : CliktCommand() {
    val input: String by option().default("sample/puzzle.cond")
    val output: String by option().default("sample/task.desc")

    override fun run() {
        val rawParameters = File(input).readText()
        val parameters = Parameters.read(rawParameters)
        val map = TunnelGenerator(parameters).generate()

        val bonuses = BusterGenerator(map, parameters).generate()

        val totalMap = mutableMapOf<Point, Cell>()
        for(x in 0..parameters.mapSize) {
            for(y in 0..parameters.mapSize) {
                val point = Point(x, y)
                if(point in map) totalMap[point] = Cell.Wall
                else totalMap[point] = Cell.Empty
            }
        }

        File(output).apply { parentFile.mkdirs() }.printWriter().use { writer ->
            writer.print(totalMap.dumpMap())
            writer.print("#")
            writer.print("(${bonuses.robot.v0},${bonuses.robot.v1})")
            writer.print("##")
            listOf(
                    bonuses.cloningBoosters.joinToString(";") { (x, y) -> "C($x,$y)" },
                    bonuses.drills.joinToString(";") { (x, y) -> "L($x,$y)" },
                    bonuses.fastWheels.joinToString(";") { (x, y) -> "F($x,$y)" },
                    bonuses.manipulators.joinToString(";") { (x, y) -> "B($x,$y)" },
                    bonuses.spawnPoints.joinToString(";") { (x, y) -> "X($x,$y)" },
                    bonuses.teleports.joinToString(";") { (x, y) -> "R($x,$y)" }
            ).joinToString(";").let { writer.print(it) }
        }

        //Test(map, parameters).repaint()
    }
}

fun main(args: Array<String>) = Main.main(args)
