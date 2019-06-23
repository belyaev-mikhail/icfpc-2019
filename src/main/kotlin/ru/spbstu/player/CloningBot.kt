package ru.spbstu.player

import org.graphstream.algorithm.APSP
import org.graphstream.graph.Graph
import org.graphstream.graph.Node
import ru.spbstu.map.*
import ru.spbstu.sim.Command
import ru.spbstu.sim.NOOP
import ru.spbstu.sim.Simulator
import ru.spbstu.sim.TICK
import ru.spbstu.util.withIdx
import ru.spbstu.wheels.MutableRef
import ru.spbstu.wheels.getValue
import ru.spbstu.wheels.peekFirstOrNull
import ru.spbstu.wheels.toMutableMap
import kotlin.math.ceil

fun Node.getNearest(nodes: Iterable<Node>): Node {
    val info = getAttribute<APSP.APSPInfo>(APSP.APSPInfo.ATTRIBUTE_NAME)!!
    return nodes.minBy { info.getShortestPathTo(it.id)?.size() ?: return this }!!
}

fun Node.getBlob() = getAttribute<SuperSmarterAStarBot.Blob>("blob")!!

fun pointsAStart(point: Point, blobMap: Set<Point>, gameMap: GameMap) = aStarSearch(
        from = point,
        goal = {
            it in blobMap
        },
        neighbours = {
            it.neighbours().filterNot { gameMap[it].status.isWall }.asSequence()
        },
        heur = { n ->
            blobMap.map { n.manhattanDistance(it) }.min()!!.toDouble()
        }
)


fun Graph.getCurrentNode(point: Point, sim: Simulator): Node =
        this.firstOrNull { point in it.getBlob().points } ?: run {
        val nearestBlobPoint = pointsAStart(point, this.flatMap { it.getBlob().points }.toSet(), sim.gameMap)!!.first()
        this.first { nearestBlobPoint in it.getBlob().points }
    }

fun CloningBotWithSegmentationByChristofidesSwarm(simref: MutableRef<Simulator>, points: Set<Point>,
                                                  botKtor: BotType = ::theMostSmartestPrioritySimulatingAstarBot,
                                                  idx: Int = 0) = sequence {
    val commands = mutableMapOf<Int, Sequence<Pair<Int, Command>>>()

    commands[idx] = CloningBot(simref, points, idx)

    val sim by simref

    val botNumber = sim.gameMap.boosterCells.count<Point, Cell> { it.value.booster == BoosterType.CLONING } + 1

    val blobs = SuperSmarterAStarBot.findBlobs(sim.gameMap).let {
        SuperSmarterAStarBot.optimizeBlobs(it)
    }
    val graph = SuperSmarterAStarBot.findGraph(blobs)

    val apsp = APSP()
    apsp.init(graph)
    apsp.isDirected = false
    apsp.compute()

    val optimalPath = Christofides.path(graph).toMutableList()
    val botPathSize = ceil(optimalPath.size.toDouble() / botNumber).toInt()

    val segments = optimalPath.chunked(botPathSize)

    val pathAssignments = mutableMapOf<Int, MutableList<Node>>()
    val graphAssignments = optimalPath.map { it to mutableListOf<Int>() }.toMutableMap()

    while (true) {
        //if (commands.isEmpty()) break

        val activeIdx = 0..sim.currentRobots.lastIndex

        for (botIdx in activeIdx) {
            val botPos = sim.currentRobots[botIdx].pos

            val botSegment = graph.first { botPos in it.getBlob().points }

            val botCommands = commands.getOrPut(botIdx) {
                if (botIdx in pathAssignments) {
                    val botPath = pathAssignments[botIdx]!!

                    val nextSegment = botPath.removeAt(0)

                    graphAssignments[nextSegment]?.add(botIdx)

                    if (botPath.isEmpty()) pathAssignments.remove(botIdx)

                    botKtor(simref, nextSegment.getBlob().points, botIdx)

                } else if (optimalPath.isNotEmpty()) {
                    val candidates = segments.filter { it.first() in optimalPath }
                            .flatMap { listOf(it.first(), it.last()) }

                    val candidate = botSegment.getNearest(candidates)
                    val botPath = segments.first { it.contains(candidate) }.toMutableList()
                    optimalPath.removeAll(botPath)

                    val nextSegment = botPath.removeAt(0)

                    graphAssignments[nextSegment]?.add(botIdx)

                    if (botPath.isNotEmpty()) pathAssignments[botIdx] = botPath

                    botKtor(simref, nextSegment.getBlob().points, botIdx)

                } else {

                    for ((segment, _) in graphAssignments.toMap()) {
                        if (segment.getBlob().points.all { sim.gameMap[it].status != Status.EMPTY }) {
                            graphAssignments -= segment
                        }
                    }

                    val segmentGroups = graphAssignments.entries
                            .groupBy { it.value.size }

                    val candidateGroup = segmentGroups[segmentGroups.keys.min()] ?: return@getOrPut sequenceOf()

                    val candidate = botSegment.getNearest(candidateGroup.map { it.key })
                    val candidateBlob = candidate.getBlob()

                    graphAssignments[candidate]?.add(botIdx)

                    botKtor(simref, candidateBlob.points, botIdx)
                }
            }

            val (cmd, rest) = botCommands.peekFirstOrNull()

            if (cmd == null) {
                commands.remove(botIdx)
                yield(botIdx to NOOP)
                continue
            } else {
                commands[botIdx] = rest
            }

            yield(cmd!!)
        }

        if (sim.hasSolved) break

        yield(0 to TICK)
    }
}

fun CloningBotWithSegmentationSwarm(simref: MutableRef<Simulator>, points: Set<Point>,
                                    botKtor: BotType = ::theMostSmartestPrioritySimulatingAstarBot,
                                    idx: Int = 0) = sequence {
    val commands = mutableMapOf<Int, Sequence<Pair<Int, Command>>>()

    commands[idx] = CloningBot(simref, points, idx)

    val sim by simref

    val blobs = SuperSmarterAStarBot.findBlobs(sim.gameMap).let {
        SuperSmarterAStarBot.optimizeBlobs(it)
    }
    val graph = SuperSmarterAStarBot.findGraph(blobs)

    val apsp = APSP()
    apsp.init(graph)
    apsp.isDirected = false
    apsp.compute()

    val graphAssignments = mutableMapOf<Node, MutableList<Int>>()

    for (node in graph) {
        graphAssignments[node] = mutableListOf()
    }

    while (true) {
        //if (commands.isEmpty()) break

        val activeIdx = 0..sim.currentRobots.lastIndex

        for (activeId in activeIdx) {
            val botPos = sim.currentRobots[activeId].pos

//            val botSegment = graph.first { botPos in it.getBlob().points }
            val botSegment = graph.getCurrentNode(botPos, sim)

            val botCommands = commands.getOrPut(activeId) {
                for ((segment, _) in graphAssignments.toMap()) {
                    if (segment.getBlob().points.all { sim.gameMap[it].status != Status.EMPTY }) {
                        graphAssignments.remove(segment)
                    }
                }

                val segmentGroups = graphAssignments.entries
                        .groupBy { it.value.size }

                val candidateGroup = segmentGroups[segmentGroups.keys.min()] ?: return@getOrPut sequenceOf()

                val candidate = botSegment.getNearest(candidateGroup.map { it.key })
                val candidateBlob = candidate.getBlob()

                graphAssignments[candidate]?.add(activeId)

                botKtor(simref, candidateBlob.points, activeId)
            }

            val (cmd, rest) = botCommands.peekFirstOrNull()

            if (cmd == null) {
                commands.remove(activeId)
                yield(activeId to NOOP)
                continue
            } else {
                commands[activeId] = rest
            }

            yield(cmd!!)
        }

        if (sim.hasSolved) break

        yield(0 to TICK)
    }
}

fun CloningBotSwarm(simref: MutableRef<Simulator>, points: Set<Point>,
                    botKtor: BotType = ::theMostSmartestPrioritySimulatingAstarBot,
                    idx: Int = 0) = sequence {
    val commands = mutableMapOf<Int, Sequence<Pair<Int, Command>>>()

    commands[idx] = CloningBot(simref, points, idx)

    while (true) {
        //if (commands.isEmpty()) break

        val sim by simref
        val activeIdx = 0..sim.currentRobots.lastIndex

        for (activeId in activeIdx) {
            val botCommands = commands.getOrPut(activeId) { botKtor(simref, points, activeId) }

            val (cmd, rest) = botCommands.peekFirstOrNull()

            if (cmd == null) {
                commands.remove(activeId)
                yield(activeId to NOOP)
                continue
            } else {
                commands[activeId] = rest
            }

            yield(cmd!!)
        }

        if (sim.hasSolved) break

        yield(0 to TICK)
    }
}

fun CloningBot(simref: MutableRef<Simulator>, points: Set<Point>, idx: Int) =
        sequence {
            while (true) {
                val sim by simref

                yieldAll(applySimulatingBoosters(sim, idx))

                val currentRobot = { sim.currentRobots[idx] }

                val closestBooster = checkNearestBooster(sim, currentRobot(), Double.MAX_VALUE) {
                    it.booster == BoosterType.CLONING ||
                            it.booster == BoosterType.MYSTERY &&
                            BoosterType.CLONING in sim.boosters
                }

                if (closestBooster != null) {
                    val local = simulatingAStarForWalking(sim, closestBooster, idx)
                    yieldAll(local)

                    yieldAll(applySimulatingBoosters(sim, idx))

                } else {
                    break
                }
            }
        }.withIdx(idx)
