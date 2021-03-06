package ru.spbstu.player

import ru.spbstu.map.BoosterType.*
import ru.spbstu.map.Cell
import ru.spbstu.map.Point
import ru.spbstu.map.Status
import ru.spbstu.map.euclidDistance
import ru.spbstu.sim.*
import ru.spbstu.util.withIdx
import ru.spbstu.wheels.MutableRef
import ru.spbstu.wheels.getValue
import java.lang.Math.abs

fun checkNearestBooster(sim: Simulator, bot: Robot, maxDist: Double = 5.0, predicate: (Cell) -> Boolean): Point? {
    val nearestBooster = sim.gameMap
            .boosterCells
            .filter { predicate(it.value) }
            .minBy { bot.pos.euclidDistance(it.key) }

    val dist = nearestBooster?.key?.euclidDistance(bot.pos) ?: return null

    return when {
        0.0 < dist && dist < maxDist -> nearestBooster.key
        else -> null
    }
}

fun applyBoosters(sim: Simulator, idx: Int = 0) = sequence {
    when {
//        CLONING in sim.boosters && sim.gameMap[sim.currentRobots[idx].pos].booster == MYSTERY -> {
//            yield(CLONE)
//        }
        MANIPULATOR_EXTENSION in sim.boosters -> {
            val manipulatorXRange = sim.currentRobots[idx].manipulators.map { it.v0 }.sorted()
            val manipulatorYRange = sim.currentRobots[idx].manipulators.map { it.v1 }.sorted()

            if (manipulatorXRange.toSet().size == 1) { // vertical extension
                val newX = manipulatorXRange.first()

                val yLeft = manipulatorYRange.first()
                val yRight = manipulatorYRange.last()

                val newY = if (abs(yLeft) < abs(yRight)) {
                    yLeft - 1
                } else {
                    yRight + 1
                }

                val extensionCommand = ATTACH_MANUPULATOR(newX, newY)

                // sim.apply(extensionCommand)

                yield(extensionCommand)

            } else { // horizontal extension
                val newY = manipulatorYRange.first()

                val xLeft = manipulatorXRange.first()
                val xRight = manipulatorXRange.last()

                val newX = if (abs(xLeft) < abs(xRight)) {
                    xLeft - 1
                } else {
                    xRight + 1
                }

                val extensionCommand = ATTACH_MANUPULATOR(newX, newY)

                // sim.apply(extensionCommand)

                yield(extensionCommand)
            }
        }
    }
}

fun smarterAstarBot(simref: MutableRef<Simulator>, points: Set<Point>, idx: Int = 0) =
        sequence {
            while (true) {
                val sim by simref

                yieldAll(applyBoosters(sim, idx))

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

fun evenSmarterAstarBot(simref: MutableRef<Simulator>, points: Set<Point>, idx: Int = 0) =
        sequence {
            while (true) {
                val sim by simref

                yieldAll(applyBoosters(sim, idx))

                val currentRobot = { sim.currentRobots[idx] }

                val closestBooster = checkNearestBooster(sim, currentRobot()) {
                    it.booster == MANIPULATOR_EXTENSION
                }

                if (closestBooster != null) {
                    val local = astarForWalking(sim, closestBooster, idx)
                    yieldAll(local)
                }

                yieldAll(applyBoosters(sim, idx))

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

fun theMostSmartestAstarBot(simref: MutableRef<Simulator>, points: Set<Point>, idx: Int = 0) =
        sequence {
            while (true) {
                val sim by simref

                yieldAll(applyBoosters(sim, idx))

                val currentRobot = { sim.currentRobots[idx] }

                val target = sim.gameMap
                        .closestFrom(currentRobot().pos) { point, cell ->
                            point in points && cell.status == Status.EMPTY
                        }
                        .firstOrNull()

                target ?: break

                val local = astarWalk(sim, target.first, idx)

                for (command in local) {
                    val booster = checkNearestBooster(sim, currentRobot()) {
                        it.booster == MANIPULATOR_EXTENSION
                    }
                    if (booster != null) {
                        val pathToBooster = astarForWalking(sim, booster, idx)
                        yieldAll(pathToBooster)
                        break
                    }
                    // currentBot = currentBot.doCommand(command)
                    yield(command)
                }
            }
        }.withIdx(idx)
