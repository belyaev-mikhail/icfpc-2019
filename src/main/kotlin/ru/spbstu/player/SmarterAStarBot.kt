package ru.spbstu.player

import ru.spbstu.map.BoosterType.MANIPULATOR_EXTENSION
import ru.spbstu.map.Status
import ru.spbstu.map.euclidDistance
import ru.spbstu.sim.ATTACH_MANUPULATOR
import ru.spbstu.sim.Simulator
import java.lang.Math.abs

fun smarterAstarBot(sim: Simulator) =
        sequence {
            while (true) {
                val target = sim
                        .gameMap
                        .cells
                        .filter { it.value.status == Status.EMPTY }
                        .minBy {
                            sim.currentRobot.pos.euclidDistance(it.key)
                        }

                target ?: break

                when {
                    MANIPULATOR_EXTENSION in sim.currentRobot.boosters -> {
                        val manipulatorXRange = sim.currentRobot.manipulators.map { it.v0 }.sorted()
                        val manipulatorYRange = sim.currentRobot.manipulators.map { it.v1 }.sorted()

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

                val local = astarWalk(sim, target.key)
                yieldAll(local)
            }
        }
