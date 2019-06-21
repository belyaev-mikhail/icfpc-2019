package ru.spbstu.player

import ru.spbstu.map.Status
import ru.spbstu.map.euclidDistance
import ru.spbstu.map.neighbours
import ru.spbstu.sim.Simulator
import ru.spbstu.wheels.MutableRef
import ru.spbstu.wheels.getValue

fun astarBot(simref: MutableRef<Simulator>) =
    sequence {
        while(true) {
            val sim by simref
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