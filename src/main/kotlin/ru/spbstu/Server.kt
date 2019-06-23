package ru.spbstu

import com.googlecode.jsonrpc4j.JsonRpcHttpClient
import com.sun.net.httpserver.HttpServer
import kotlinx.coroutines.*
import ru.spbstu.generator.GeneratorMain
import ru.spbstu.map.GameMap
import ru.spbstu.map.Point
import ru.spbstu.parse.parseFile
import ru.spbstu.player.*
import ru.spbstu.sim.Command
import ru.spbstu.sim.Robot
import ru.spbstu.sim.Simulator
import ru.spbstu.util.log
import ru.spbstu.util.toSolution
import ru.spbstu.util.withAutoTick
import ru.spbstu.wheels.*
import java.io.File
import java.net.InetSocketAddress
import java.net.URL
import java.nio.file.Paths


data class SubmitParams(val block_num: Int, val sol_path: String, val desc_path: String)

object Server {

    private const val TIMEOUT_MS = 10L

    val pool = newFixedThreadPoolContext(10, "Pool")

    private fun handleMap(file: String): String? {
        val data = File(file).let { parseFile(it.name, it.readText()) }
        log.debug("Running portfolio for $file")

        val best = runBlocking(pool) {
            val paths = listOf(
                    "astarBot" to ::astarBot.withAutoTick(),
                    "smarterAstarBot" to ::smarterAstarBot.withAutoTick(),
                    "evenSmarterAstarBot" to ::evenSmarterAstarBot.withAutoTick(),
                    "priorityAstarBot" to ::priorityAstarBot.withAutoTick(),
                    "smarterPriorityAstarBot" to ::smarterPriorityAstarBot.withAutoTick(),
                    "evenSmarterPriorityAstarBot" to ::evenSmarterPriorityAstarBot.withAutoTick(),
                    "theMostSmartestPriorityAstarBot" to ::theMostSmartestPriorityAstarBot.withAutoTick(),
                    "theMostSmartestPrioritySimulatingAstarBot" to ::theMostSmartestPrioritySimulatingAstarBot.withAutoTick(),
                    "evenSmarterPrioritySimulatingAstarBot" to ::evenSmarterPrioritySimulatingAstarBot.withAutoTick(),
                    "SuperSmarterAStarBot" to SuperSmarterAStarBot::run.withAutoTick(),
                    "CloningBotSwarm" to ::CloningBotSwarm)
                    .map {
                        val map = GameMap(data)
                        val sim = Simulator(Robot(data.initial), map)

                        val name = it.first
                        val bot = it.second
                        async {
                            val res = handleMapSingle(sim, bot)
                            name to res
                        }
                    }.awaitAll()

            paths.minBy { it.second.second }
        } ?: return null
        val name = best.first
        val score = best.second.second
        val paths = best.second.first

        val sol = paths.toSolution()

        val resultFile = File(file.replace(".desc", ".sol"))

        resultFile.bufferedWriter().use {
            log.debug("Solution for file $file: $sol")
            log.debug("Solution score for file $file: $score")
            log.debug("Best solution for file $file is $name")
            it.write(sol)
        }
        return resultFile.absolutePath
    }



    private fun handleMapSingle(isim: Simulator, bot: (MutableRef<Simulator>, Set<Point>, Int) -> Sequence<Pair<Int, Command>>): Pair<Sequence<Pair<Int, Command>>, Int> {
        val mutSim = ref(isim)
        var sim by mutSim
        val path = bot(mutSim, sim.gameMap.cells.keys, 0).memoize()

        for (command in path) {
            sim = sim.apply(command.first, command.second)
        }

        return path to sim.time
    }

    val LAMBDA_PROVIDER = URL("http://localhost:8332/")

    fun checkForBlock(){
          val client = JsonRpcHttpClient(LAMBDA_PROVIDER)
        client("getblockinfo", Unit)
    }

    private fun submitResults(params: SubmitParams) {
        val client = JsonRpcHttpClient(LAMBDA_PROVIDER)
        client("submit", params)
        log.debug("Submitted: $params")
    }

    fun processBlock(path: String) {
        try {
            val blockDir = File(path)
            val blockNumber = Integer.valueOf(blockDir.name)
            val descFile = Paths.get(blockDir.path, "task.desc").toFile().absolutePath
            val puzzleFile = Paths.get(blockDir.path, "puzzle.cond").toFile().absolutePath

            val solutionFile = handleMap(descFile) ?: return

            val puzzleResult = puzzleFile.replace(".cond", ".desc")
            GeneratorMain.generate(puzzleFile, puzzleResult)

            val block_num = blockNumber
            val sol_path = solutionFile
            val desc_path = puzzleResult

            submitResults(SubmitParams(block_num, sol_path, desc_path))
        } catch (ex: Exception) {
            ex.printStackTrace()
        }
    }


}


fun main(args: Array<String>) {
    HttpServer.create(InetSocketAddress(32345), 0).apply {
        createContext("/newBlock") { http ->
            val newBlockPath = http.requestBody.bufferedReader().readText().trim()
            Server.pool.executor.execute {
                Server.processBlock(newBlockPath)
            }
            log.debug("Accepted new block: $newBlockPath")
        }

        start()



        log.debug("Start waiting for blocks")
//
//        runBlocking {
//            while (true) {
//                Server.checkForBlock()
//                delay(10000L)
//            }
//        }

    }
}
