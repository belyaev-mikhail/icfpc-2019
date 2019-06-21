package ru.spbstu.player

import ru.spbstu.map.Status
import ru.spbstu.map.euclidDistance
import ru.spbstu.map.neighbours
import ru.spbstu.sim.Simulator

fun astarBot(sim: Simulator) =
    sequence {
        while(true) {
            val target = sim
                    .gameMap
                    .cells
                    .filter { it.value.status == Status.EMPTY }
                    .minBy {
                        sim.currentRobot.pos.euclidDistance(it.key)
                    }

            target ?: break

            val local = astarWalk(sim, target.key)
            yieldAll(local)
        }
    }