package ru.spbstu.player

import ru.spbstu.map.BoosterType
import ru.spbstu.map.Point
import ru.spbstu.sim.Command
import ru.spbstu.sim.Simulator
import ru.spbstu.sim.TICK
import ru.spbstu.util.withIdx
import ru.spbstu.wheels.MutableRef
import ru.spbstu.wheels.getValue
import ru.spbstu.wheels.peekFirstOrNull

fun CloningBotSwarm(simref: MutableRef<Simulator>, points: Set<Point>, idx: Int = 0) = sequence {
    val commands = mutableMapOf<Int, Sequence<Pair<Int, Command>>>()

    commands[idx] = CloningBot(simref, points, idx)

    while (true) {
        if (commands.isEmpty()) break

        val sim by simref
        val activeIdx = 0..sim.currentRobots.lastIndex

        for (activeId in activeIdx) {
            val botCommands = commands.getOrPut(activeId) { theMostSmartestPriorityAstarBot(simref, points, activeId) }

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
        }.withIdx(idx) + theMostSmartestPriorityAstarBot(simref, points, idx)
