package ru.spbstu.player

import ru.spbstu.map.BoosterType
import ru.spbstu.map.Point
import ru.spbstu.map.Status
import ru.spbstu.map.euclidDistance
import ru.spbstu.sim.Simulator
import ru.spbstu.wheels.MutableRef
import ru.spbstu.wheels.getValue

fun priorityAstarBot(simref: MutableRef<Simulator>, points: Set<Point>) =
        sequence {
            while (true) {
                val sim by simref
                val cells = sim.gameMap.cells.filter { it.key in points }
                val target = cells
                        .filter { it.value.status == Status.EMPTY }
                        .minBy {
                            sim.currentRobot.pos.euclidDistance(it.key)
                        }

                target ?: break

                val local = visibleAstarWalk(sim, target.key)
                yieldAll(local)
            }
        }


fun smarterPriorityAstarBot(simref: MutableRef<Simulator>, points: Set<Point>) =
        sequence {
            while (true) {
                val sim by simref
                val cells = sim.gameMap.cells.filter { it.key in points }
                val target = cells
                        .filter { it.value.status == Status.EMPTY }
                        .minBy {
                            sim.currentRobot.pos.euclidDistance(it.key)
                        }

                target ?: break

                yieldAll(applyBoosters(sim))

                val local = visibleAstarWalk(sim, target.key)
                yieldAll(local)
            }
        }


fun evenSmarterPriorityAstarBot(simref: MutableRef<Simulator>, points: Set<Point>, idx: Int) =
        sequence {
            while (true) {
                val sim by simref

                yieldAll(applyBoosters(sim, idx))

                val currentRobot = { sim.currentRobots[idx] }

                val cells = sim.gameMap.cells.filter { it.key in points }

                val closestBooster = cells
                        .filter { it.value.booster != null }
                        .filter { it.value.booster == BoosterType.MANIPULATOR_EXTENSION }
                        .minBy { currentRobot().pos.euclidDistance(it.key) }

                if (closestBooster != null && currentRobot().pos.euclidDistance(closestBooster.key) < 5.0) {
                    val local = astarWithoutTurnsWalk(sim, closestBooster.key, idx)
                    yieldAll(local)
                }

                yieldAll(applyBoosters(sim, idx))

                val target = cells
                        .filter { it.value.status == Status.EMPTY }
                        .minBy {
                            currentRobot().pos.euclidDistance(it.key)
                        }

                target ?: break

                val local = visibleAstarWalk(sim, target.key, idx)
                yieldAll(local)
            }
        }
