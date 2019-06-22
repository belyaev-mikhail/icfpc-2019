package ru.spbstu.player

import ru.spbstu.map.BoosterType
import ru.spbstu.map.Point
import ru.spbstu.map.Status
import ru.spbstu.map.euclidDistance
import ru.spbstu.sim.Simulator
import ru.spbstu.util.withIdx
import ru.spbstu.wheels.MutableRef
import ru.spbstu.wheels.getValue

fun priorityAstarBot(simref: MutableRef<Simulator>, points: Set<Point>, idx: Int = 0) =
        sequence {
            while (true) {
                val sim by simref

                val currentRobot = { sim.currentRobots[idx] }

                val target = sim.gameMap
                        .closestFrom(currentRobot().pos) {
                            point, cell -> point in points && cell.status == Status.EMPTY
                        }
                        .firstOrNull()

                target ?: break

                val local = visibleAstarWalk(sim, target.first, idx)
                yieldAll(local)
            }
        }.withIdx(idx)


fun smarterPriorityAstarBot(simref: MutableRef<Simulator>, points: Set<Point>, idx: Int = 0) =
        sequence {
            while (true) {
                val sim by simref

                yieldAll(applyBoosters(sim, idx))

                val currentRobot = { sim.currentRobots[idx] }

                val target = sim.gameMap
                        .closestFrom(currentRobot().pos) {
                            point, cell -> point in points && cell.status == Status.EMPTY
                        }
                        .firstOrNull()

                target ?: break

                yieldAll(applyBoosters(sim, idx))

                val local = visibleAstarWalk(sim, target.first, idx)
                yieldAll(local)
            }
        }.withIdx(idx)


fun evenSmarterPriorityAstarBot(simref: MutableRef<Simulator>, points: Set<Point>, idx: Int = 0) =
        sequence {
            while (true) {
                val sim by simref

                yieldAll(applyBoosters(sim, idx))

                val currentRobot = { sim.currentRobots[idx] }

                val closestBooster = checkNearestBooster(sim, currentRobot()) {
                    it.booster == BoosterType.MANIPULATOR_EXTENSION
                }

                if (closestBooster != null) {
                    val local = astarWithoutTurnsWalk(sim, closestBooster, idx)
                    yieldAll(local)
                }

                yieldAll(applyBoosters(sim, idx))

                val target = sim.gameMap
                        .closestFrom(currentRobot().pos) {
                            point, cell -> point in points && cell.status == Status.EMPTY
                        }
                        .firstOrNull()

                target ?: break

                val local = visibleAstarWalk(sim, target.first, idx)
                yieldAll(local)
            }
        }.withIdx(idx)

fun theMostSmartestPriorityAstarBot(simref: MutableRef<Simulator>, points: Set<Point>, idx: Int = 0) =
        sequence {
            while (true) {
                val sim by simref

                yieldAll(applyBoosters(sim, idx))

                var currentRobot = sim.currentRobots[idx]

                val target = sim.gameMap
                        .closestFrom(currentRobot.pos) {
                            point, cell -> point in points && cell.status == Status.EMPTY
                        }
                        .firstOrNull()

                target ?: break

                val local = visibleAstarWalk(sim, target.first, idx)
                for (command in local) {
                    val booster = checkNearestBooster(sim, currentRobot) {
                        it.booster == BoosterType.MANIPULATOR_EXTENSION
                    }
                    if (booster != null) {
                        val pathToBooster = astarWithoutTurnsWalk(sim, booster, idx)
                        yieldAll(pathToBooster)
                        break
                    }
                    currentRobot = currentRobot.doCommand(command)
                    yield(command)
                }
            }
        }.withIdx(idx)
