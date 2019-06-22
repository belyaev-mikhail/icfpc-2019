package ru.spbstu.player

import ru.spbstu.map.BoosterType
import ru.spbstu.map.Point
import ru.spbstu.map.euclidDistance
import ru.spbstu.sim.Command
import ru.spbstu.sim.Simulator
import ru.spbstu.sim.TICK
import ru.spbstu.wheels.MutableRef
import ru.spbstu.wheels.getValue
import ru.spbstu.wheels.peekFirstOrNull

fun CloningBotSwarm(simref: MutableRef<Simulator>, points: Set<Point>) = sequence {
    val commands = mutableMapOf<Int, Sequence<Command>>()

    commands[0] = CloningBot(simref, points, 0)

    while (true) {
        if (commands.isEmpty()) break

        val sim by simref
        val activeIdx = 0..sim.currentRobots.lastIndex

        for (idx in activeIdx) {
            val botCommands = commands.getOrPut(idx) { evenSmarterPriorityAstarBot(simref, points, idx) }

            val (cmd, rest) = botCommands.peekFirstOrNull()

            if (cmd == null) {
                commands.remove(idx)
                continue
            } else {
                commands[idx] = rest
            }

            yield(idx to cmd)
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

                val cells = sim.gameMap.cells.filter { it.key in points }

                val closestBooster = cells
                        .filter {
                            it.value.booster == BoosterType.CLONING ||
                                    it.value.booster == BoosterType.MYSTERY &&
                                    BoosterType.CLONING in sim.boosters
                        }.sortedBy { currentRobot().pos.euclidDistance(it.key) }
                        .firstOrNull()

                if (closestBooster != null) {
                    val local = astarWithoutTurnsWalk(sim, closestBooster.key, idx)
                    yieldAll(local)

                    yieldAll(applyBoosters(sim, idx))

                } else {
                    break
                }
            }
        } + evenSmarterPriorityAstarBot(simref, points, idx)