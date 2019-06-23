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
import ru.spbstu.sim.Command
import ru.spbstu.sim.Robot
import ru.spbstu.sim.SimFrame
import ru.spbstu.sim.Simulator
import ru.spbstu.util.awaitAll
import ru.spbstu.wheels.*
import ru.spbstu.util.log
import ru.spbstu.util.toSolution
import ru.spbstu.util.withAutoTick
import ru.spbstu.wheels.*
import java.io.File
import kotlin.reflect.jvm.reflect

object Main : CliktCommand() {
    val useAbsoluteMapPath: Boolean by option().flag(default = false)
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
        val data = File(file).let { parseFile(it.name, it.readText()) }
        log.debug("Running portfolio for $file")

        val best = run {
            val paths = listOf(
//                    "astarBot" to ::astarBot.withAutoTick(),
//                    "enclosedAstarBot" to ::enclosedAstarBot.withAutoTick(),
//                    "smarterAstarBot" to ::smarterAstarBot.withAutoTick(),
//                    "evenSmarterAstarBot" to ::evenSmarterAstarBot.withAutoTick(),
//                    "priorityAstarBot" to ::priorityAstarBot.withAutoTick(),
//                    "smarterPriorityAstarBot" to ::smarterPriorityAstarBot.withAutoTick(),
//                    "evenSmarterPriorityAstarBot" to ::evenSmarterPriorityAstarBot.withAutoTick(),
//                    "theMostSmartestPriorityAstarBot" to ::theMostSmartestPriorityAstarBot.withAutoTick(),
//                    "evenSmarterPrioritySimulatingAstarBot" to ::evenSmarterPrioritySimulatingAstarBot.withAutoTick(),
                    "smarterPrioritySimulatingAstarBot" to ::smarterPrioritySimulatingAstarBot.withAutoTick(),
                    "evenSmarterPrioritySimulatingAstarBot" to ::evenSmarterPrioritySimulatingAstarBot.withAutoTick(),
                    "theMostSmartestPrioritySimulatingAstarBot" to ::theMostSmartestPrioritySimulatingAstarBot.withAutoTick(),
                    "SuperSmarterAStarBot" to SuperSmarterAStarBot::run.withAutoTick(),
                    "CloningBotSwarm" to ::CloningBotSwarm,
                    "SimulatingCloningBotSwarm" to ::SimulatingCloningBotSwarm)
                    . map {
                val map = GameMap(data)
                val sim = Simulator(Robot(data.initial), map)

                val name = it.first
                val bot = it.second

                async {
                    val res = handleMapSingle(sim, bot)
                    File(File(File("candidates"), name), File(file.replace(".desc", ".sol")).name).apply { parentFile.mkdirs() }.bufferedWriter().use {
                        it.write(res.first.toSolution())
                    }
                    name to res
                }
            }.awaitAll()

            paths.minBy { it.second.second }
        }

        val name = best?.first
        val score = best?.second?.second
        val paths = best?.second?.first

        val sol = paths?.toSolution()

        File(File(solFolder), File(file.replace(".desc", ".sol")).name).apply { parentFile.mkdirs() }.bufferedWriter().use {
            log.debug("Solution for file $file: $sol")
            log.debug("Solution score for file $file: $score")
            log.debug("Best solution for file $file is $name")
            it.write(sol)
        }
    }

    suspend fun handleMapSingle(isim: Simulator, bot: (MutableRef<Simulator>, Set<Point>, Int) -> Sequence<Pair<Int, Command>>): Pair<Sequence<Pair<Int, Command>>, Int> {
        val mutSim = ref(isim)
        var sim by mutSim
        val path = bot(mutSim, sim.gameMap.cells.keys, 0).memoize()

        if (gui) {
            val frame = SimFrame(guiCellSize) { mutSim.value }
            for (command in path) {
                sim = sim.apply(command.first, command.second)
                delay((1000.0 / speed).toLong())
                frame.repaint()
            }
            delay(5000)
            frame.dispose()
        } else {
            for (command in path) {
                sim = sim.apply(command.first, command.second)
            }
        }

        return path to sim.time
    }

    override fun run() {
        if (mergeSolutions) { /* merge solution mode */
            val folder = File(candidatesFolder)
            val sols = File(solFolder)
            sols.mkdirs()
            check(folder.exists())
            check(folder.isDirectory)

            val dirs = folder.listFiles().filter { it.isDirectory }.sortedBy { it.name }
            val allNames = dirs.flatMapTo(mutableSetOf()) { it.list().filter { it.endsWith(".sol") }.asIterable() }
            for (name in allNames) {
                val scoring = mutableMapOf<File, Pair<Int, String>>()
                for (dir in dirs) {
                    val file = dir.listFiles { _, nm -> nm == name }.firstOrNull()
                            ?: continue
                    val text = file.readText()
                    val ans = parseAnswer(text)
                    val score = ans.maxBy { it.size }?.size!!
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

        val pool = newFixedThreadPoolContext(threads, "Pool")

        if (useAbsoluteMapPath) {
            runBlocking(pool) { handleMap(map) }
        } else if (map != "all") {
            val filename = "000$map".takeLast(3) // this is fucked up, but i'm tired of FileNotFoundException
            runBlocking(pool) { handleMap("docs/tasks/prob-$filename.desc") }
        } else {
            runBlocking(pool) {
                File("docs/tasks").walkTopDown().toList().filter { it.extension == "desc" }.sorted().map {
                    async { handleMap(it.absolutePath) }
                }.awaitAll()
            }
        }
    }
}

fun main(args: Array<String>) = Main.main(args)
