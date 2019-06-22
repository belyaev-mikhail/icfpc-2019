package ru.spbstu.player

import ru.spbstu.map.Point
import ru.spbstu.map.Status
import ru.spbstu.map.euclidDistance
import ru.spbstu.sim.Simulator
import ru.spbstu.util.withIdx
import ru.spbstu.wheels.MutableRef
import ru.spbstu.wheels.getValue

fun astarBot(simref: MutableRef<Simulator>, points: Set<Point>, idx: Int) =
        sequence {
            while (true) {
                val sim by simref
                val currentRobot = { sim.currentRobots[idx] }
                val cells = sim.gameMap.cells.filter { it.key in points }
                val target = cells
                        .filter { it.value.status == Status.EMPTY }
                        .minBy {
                            currentRobot().pos.euclidDistance(it.key)
                        }

                target ?: break

                val local = astarWalk(sim, target.key)
                yieldAll(local)
            }
        }.withIdx(idx)
