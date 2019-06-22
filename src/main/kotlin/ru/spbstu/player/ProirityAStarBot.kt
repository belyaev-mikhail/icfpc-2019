package ru.spbstu.player

import ru.spbstu.map.BoosterType
import ru.spbstu.map.Status
import ru.spbstu.map.euclidDistance
import ru.spbstu.sim.Simulator
import ru.spbstu.wheels.MutableRef
import ru.spbstu.wheels.getValue

fun priorityAstarBot(simref: MutableRef<Simulator>) =
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

                val local = visibleAstarWalk(sim, target.key)
                yieldAll(local)
            }
        }


fun smarterPriorityAstarBot(simref: MutableRef<Simulator>) =
        sequence {
            while (true) {
                val sim by simref
                val target = sim
                        .gameMap
                        .cells
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


fun evenSmarterPriorityAstarBot(simref: MutableRef<Simulator>) =
        sequence {
            while (true) {
                val sim by simref
                yieldAll(applyBoosters(sim))

                val closestBooster = sim.gameMap
                        .cells
                        .filter { it.value.booster != null }
                        .filter { it.value.booster == BoosterType.MANIPULATOR_EXTENSION }
                        .minBy { sim.currentRobot.pos.euclidDistance(it.key) }

                if (closestBooster != null && sim.currentRobot.pos.euclidDistance(closestBooster.key) < 5.0) {
                    val local = astarWithoutTurnsWalk(sim, closestBooster.key)
                    yieldAll(local)
                }
                yieldAll(applyBoosters(sim))

                val target = sim.gameMap
                        .cells
                        .filter { it.value.status == Status.EMPTY }
                        .minBy {
                            sim.currentRobot.pos.euclidDistance(it.key)
                        }


                target ?: break

                val local = visibleAstarWalk(sim, target.key)
                yieldAll(local)
            }
        }
