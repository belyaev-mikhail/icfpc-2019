package ru.spbstu

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.types.int
import kotlinx.coroutines.*
import ru.spbstu.map.GameMap
import ru.spbstu.map.Point
import ru.spbstu.parse.parseAnswer
import ru.spbstu.parse.parseFile
import ru.spbstu.player.*
import ru.spbstu.sim.*
import ru.spbstu.wheels.*
import ru.spbstu.util.log
import ru.spbstu.wheels.memoize
import java.io.File
import java.lang.Exception

object Main : CliktCommand() {
    val map: String by option(help = "Map to run on").default("all")
    val gui: Boolean by option().flag(default = false)
    val guiCellSize: Int by option().int().default(10)
    val speed: Int by option().int().default(10)
    val solFolder: String by option().default("solutions")
    val threads: Int by option().int().default(1)
    val mergeSolutions: Boolean by option().flag(default = false)
    val candidatesFolder by option().default("candidates")

//    val count: Int by option(help = "Number of greetings").int().default(1)
//    val name: String? by option(help = "The person to greet")

    suspend fun CoroutineScope.handleMap(file: String) {
        try {
            val data = File(file).let { parseFile(it.name, it.readText()) }
            log.debug("Running portfolio for $file")

            val path = runBlocking(newFixedThreadPoolContext(threads, "Pool")) {
                val paths = listOf(SuperSmarterAStarBot::run)
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
        } catch (ex: Exception) {
            ex.printStackTrace()
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
        check(sim.hasSolved)
        return path
    }

    override fun run() {
        if(mergeSolutions) { /* merge solution mode */
            val folder = File(candidatesFolder)
            val sols = File(solFolder)
            sols.mkdirs()
            check(folder.exists())
            check(folder.isDirectory)

            val dirs = folder.listFiles().filter { it.isDirectory }.sortedBy { it.name }
            val allNames = dirs.flatMapTo(mutableSetOf()) { it.list().filter { it.endsWith(".sol") }.asIterable() }
            for(name in allNames) {
                val scoring = mutableMapOf<File, Pair<Int, String>>()
                for(dir in dirs) {
                    val file = dir.listFiles { _, nm -> nm == name  }.firstOrNull() ?: continue
                    val text = file.readText()
                    val ans = parseAnswer(text)
                    var score = 0
                    var numberOfBots = 1
                    val iterator = ans.iterator()
                    while(iterator.hasNext()) {
                        for(i in 1..numberOfBots) {
                            check(iterator.hasNext())
                            val command = iterator.next()
                            if(command === CLONE) ++numberOfBots
                        }
                        ++score
                    }
                    scoring[dir] = score to text
                }
                File(sols, name).printWriter().use {
                    val top = scoring.minBy { it.value.first }
                    log.debug("File: $name")
                    check(top != null)
                    log.debug("Top score: ${top.value.first}; folder: ${top.key}")
                    it.print(top.value.second)
                }
            }
            return
        }

        if (map != "all") {
            runBlocking { handleMap("docs/tasks/prob-$map.desc") }
        } else {
            runBlocking(newFixedThreadPoolContext(threads, "Pool")) {
                val asyncs = File("docs/tasks").walkTopDown().toList().filter { it.extension == "desc" }.map {
                    async { handleMap(it.absolutePath) }
                }

                while (asyncs.any { it.isActive }) {
                    yield()
                }
            }
        }
    }
}

fun main(args: Array<String>) = Main.main(args)
