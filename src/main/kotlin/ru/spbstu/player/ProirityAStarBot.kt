package ru.spbstu.player

import ru.spbstu.map.Point
import ru.spbstu.map.Status
import ru.spbstu.map.euclidDistance
import ru.spbstu.sim.Simulator
import ru.spbstu.wheels.MutableRef
import ru.spbstu.wheels.getValue

fun priorityAstarBot(simref: MutableRef<Simulator>, points: Set<Point>) =
        sequence {
            while(true) {
                val sim by simref
                val cells = sim.gameMap.cells.filter { it.key in points}
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
                val cells = sim.gameMap.cells.filter { it.key in points}
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


fun evenSmarterPriorityAstarBot(simref: MutableRef<Simulator>, points: Set<Point>) =
        sequence {
            while (true) {
                val sim by simref
                yieldAll(applyBoosters(sim))

                val cells = sim.gameMap.cells.filter { it.key in points}

                val closestBooster = checkNearestBooster(sim, sim.currentRobot)

                if (closestBooster != null) {
                    val local = astarWithoutTurnsWalk(sim, closestBooster)
                    yieldAll(local)
                }
                yieldAll(applyBoosters(sim))

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

fun theMostSmartestPriorityAstarBot(simref: MutableRef<Simulator>, points: Set<Point>) =
        sequence {
            while (true) {
                val sim by simref

                yieldAll(applyBoosters(sim))
                var currentBot = sim.currentRobot

                val cells = sim.gameMap.cells.filter { it.key in points}
                val target = cells
                        .filter { it.value.status == Status.EMPTY }
                        .minBy {
                            sim.currentRobot.pos.euclidDistance(it.key)
                        }


                target ?: break

                val local = visibleAstarWalk(sim, target.key)
                for (command in local) {
                    val booster = checkNearestBooster(sim, currentBot)
                    if (booster != null) {
                        val pathToBooster = astarWithoutTurnsWalk(sim, booster)
                        yieldAll(pathToBooster)
                        break
                    }
                    currentBot = currentBot.doCommand(command)
                    yield(command)
                }
            }
        }
