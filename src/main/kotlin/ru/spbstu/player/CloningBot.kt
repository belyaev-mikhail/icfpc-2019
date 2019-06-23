package ru.spbstu.player

import org.graphstream.algorithm.APSP
import org.graphstream.graph.Node
import ru.spbstu.map.BoosterType
import ru.spbstu.map.Point
import ru.spbstu.map.Status
import ru.spbstu.sim.Command
import ru.spbstu.sim.Simulator
import ru.spbstu.sim.TICK
import ru.spbstu.util.withIdx
import ru.spbstu.wheels.MutableRef
import ru.spbstu.wheels.getValue
import ru.spbstu.wheels.peekFirstOrNull

fun Node.getNearest(nodes: Iterable<Node>): Node {
    val info = getAttribute<APSP.APSPInfo>(APSP.APSPInfo.ATTRIBUTE_NAME)
    return nodes.minBy { info.getShortestPathTo(it.id).size() }!!
}

fun Node.getBlob() = getAttribute<SuperSmarterAStarBot.Blob>("blob")!!

fun CloningBotWithSegmentationSwarm(simref: MutableRef<Simulator>, points: Set<Point>,
        botKtor: BotType = SuperSmarterAStarBot,
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
        if (commands.isEmpty()) break

        val activeIdx = 0..sim.currentRobots.lastIndex

        for (activeId in activeIdx) {
            val botPos = sim.currentRobots[activeId].pos

            val botSegment = graph.first { botPos in it.getBlob().points }

            val botCommands = commands.getOrPut(activeId) {
                for ((segment, _) in graphAssignments.toMap()) {
                    if (segment.getBlob().points.all { sim.gameMap[it].status != Status.EMPTY}) {
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
        botKtor: BotType = SuperSmarterAStarBot,
        idx: Int = 0) = sequence {
    val commands = mutableMapOf<Int, Sequence<Pair<Int, Command>>>()

    commands[idx] = CloningBot(simref, points, idx)

    while (true) {
        if (commands.isEmpty()) break

        val sim by simref
        val activeIdx = 0..sim.currentRobots.lastIndex

        for (activeId in activeIdx) {
            val botCommands = commands.getOrPut(activeId) { botKtor(simref, points, activeId) }

            val (cmd, rest) = botCommands.peekFirstOrNull()

            if (cmd == null) {
                commands.remove(activeId)
                continue
            } else {
                commands[activeId] = rest
            }

            yield(cmd!!)
        }

        yield(0 to TICK)
    }
}

fun CloningBot(simref: MutableRef<Simulator>, points: Set<Point>, idx: Int) =
        sequence {
            while (true) {
                val sim by simref

                yieldAll(applyBoosters(sim, idx))

                val currentRobot = { sim.currentRobots[idx] }

                val closestBooster = checkNearestBooster(sim, currentRobot(), Double.MAX_VALUE) {
                    it.booster == BoosterType.CLONING ||
                            it.booster == BoosterType.MYSTERY &&
                            BoosterType.CLONING in sim.boosters
                }

                if (closestBooster != null) {
                    val local = astarForWalking(sim, closestBooster, idx)
                    yieldAll(local)

                    yieldAll(applyBoosters(sim, idx))

                } else {
                    break
                }
            }
        }.withIdx(idx)
