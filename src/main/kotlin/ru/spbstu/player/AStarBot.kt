package ru.spbstu.player

import ru.spbstu.map.Point
import ru.spbstu.map.Status
import ru.spbstu.sim.Simulator
import ru.spbstu.util.withIdx
import ru.spbstu.wheels.MutableRef
import ru.spbstu.wheels.getValue

fun astarBot(simref: MutableRef<Simulator>, points: Set<Point>, idx: Int = 0) =
        sequence {
            while (true) {
                val sim by simref

                val currentRobot = { sim.currentRobots[idx] }

                val target = sim.gameMap
                        .closestFrom(currentRobot().pos) { point, cell ->
                            point in points && cell.status == Status.EMPTY
                        }
                        .firstOrNull()

                target ?: break

                val local = astarWalk(sim, target.first, idx)
                yieldAll(local)
            }
        }.withIdx(idx)

fun enclosedAstarBot(simref: MutableRef<Simulator>, points: Set<Point>, idx: Int = 0) =
        sequence {
            while (true) {
                val sim by simref

                val currentRobot = { sim.currentRobots[idx] }

                val target = sim.gameMap
                        .closestFrom(currentRobot().pos) { point, cell ->
                            point in points && cell.status == Status.EMPTY
                        }
                        .firstOrNull()

                target ?: break

                val local = enclosedAstarWalk(sim, target.first, idx)
                yieldAll(local)
            }
        }.withIdx(idx)