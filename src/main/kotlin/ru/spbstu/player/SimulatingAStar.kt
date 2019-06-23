package ru.spbstu.player

import ru.spbstu.map.*
import ru.spbstu.sim.*
import ru.spbstu.util.log
import ru.spbstu.util.withIdx
import ru.spbstu.wheels.*
import java.util.*

data class SimulatorAndCommand(val sim: Simulator, val command: Command, val idx: Int) {
    val robot: Robot get() = sim.currentRobots[idx]

    val neighbors by lazy {
        val commands = listOf(TURN_CW, TURN_CCW, MOVE_UP, MOVE_RIGHT, MOVE_LEFT, MOVE_DOWN)
        commands.mapNotNull {
            tryEx { sim.apply(idx, it) }.getOrNull()?.run { SimulatorAndCommand(this, it, idx) }
        }
    }

    override fun equals(other: Any?): Boolean = other is SimulatorAndCommand
            && robot.pos == other.robot.pos && robot.orientation == other.robot.orientation && idx == other.idx
//            && robot.activeBoosters == other.robot.activeBoosters

    override fun hashCode(): Int = Objects.hash(robot.pos, robot.orientation /*, robot.activeBoosters */, idx)
}

fun simulatingAStar(sim: Simulator, target: Point, idx: Int) = aStarSearch(
        SimulatorAndCommand(sim, NOOP, idx),
        heur = { e ->
            e.robot.manipulatorPos
                    .map { it.manhattanDistance(target).toDouble() + if (e.sim.gameMap.isVisible(e.robot.pos, it)) 0.1 else 0.0 }
                    .min()
                    ?: Double.MAX_VALUE
        },
        goal = { it.sim.gameMap[target].status == Status.WRAP },
        neighbours = { it.neighbors.asSequence() }
)?.dropLast(1)?.map { it.command }?.reversed() ?: listOf(NOOP)

fun simulatingEnclosedAStar(sim: Simulator, target: Point, idx: Int) = aStarSearch(
        SimulatorAndCommand(sim, NOOP, idx),
        heur = { e ->
            e.robot.manipulatorPos
                    .map {
                        it.manhattanDistance(target).toDouble()
                        +if (e.sim.gameMap.isVisible(e.robot.pos, it)) 0.1 else 0.0
                        +if (e.sim.gameMap.inEnclosedArea(e.robot.pos)) 1.0 else 0.0
                    }
                    .min()
                    ?: Double.MAX_VALUE
        },
        goal = { target in it.sim.currentRobots[it.idx].manipulatorPos && it.sim.gameMap.isVisible(it.robot.pos, target) },
        neighbours = { it.neighbors.asSequence() }
)?.dropLast(1).orEmpty().map { it.command }.reversed()

fun simulatingAStarForWalking(sim: Simulator, target: Point, idx: Int) = aStarSearch(
        SimulatorAndCommand(sim, NOOP, idx),
        heur = { e ->
            e.robot.manipulatorPos
                    .map { it.manhattanDistance(target).toDouble() + if (e.sim.gameMap.isVisible(e.robot.pos, it)) 0.1 else 0.0 }
                    .min()
                    ?: Double.MAX_VALUE
        },
        goal = { target == it.sim.currentRobots[it.idx].pos },
        neighbours = { it.neighbors.asSequence() }
)?.dropLast(1)?.map { it.command }?.reversed() ?: listOf(NOOP)

fun applySimulatingBoosters(sim: Simulator, idx: Int = 0) = sequence {
    when {
        BoosterType.CLONING in sim.boosters && sim.gameMap[sim.currentRobots[idx].pos].booster == BoosterType.MYSTERY -> {
            yield(CLONE)
        }
        BoosterType.MANIPULATOR_EXTENSION in sim.boosters -> {
            val manipulatorXRange = sim.currentRobots[idx].manipulators.map { it.v0 }.sorted()
            val manipulatorYRange = sim.currentRobots[idx].manipulators.map { it.v1 }.sorted()

            if (manipulatorXRange.toSet().size == 1) { // vertical extension
                val newX = manipulatorXRange.first()

                val yLeft = manipulatorYRange.first()
                val yRight = manipulatorYRange.last()

                val newY = if (Math.abs(yLeft) < Math.abs(yRight)) {
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

                val newX = if (Math.abs(xLeft) < Math.abs(xRight)) {
                    xLeft - 1
                } else {
                    xRight + 1
                }

                val extensionCommand = ATTACH_MANUPULATOR(newX, newY)

                // sim.apply(extensionCommand)

                yield(extensionCommand)
            }
        }
        BoosterType.FAST_WHEELS in sim.boosters && BoosterType.FAST_WHEELS !in sim.currentRobots[idx].activeBoosters -> {
            val speedUpCommand = USE_FAST_WHEELS
            yield(speedUpCommand)
        }
    }
}

fun prioritySimulatingAstarBot(simref: MutableRef<Simulator>, points: Set<Point>, idx: Int) =
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

                val local = simulatingAStar(sim, target.first, idx)
                yieldAll(local)
            }
        }.withIdx(idx)

fun smarterPrioritySimulatingAstarBot(simref: MutableRef<Simulator>, points: Set<Point>, idx: Int = 0) =
        sequence {
            while (true) {
                val sim by simref

                val currentRobot = { sim.currentRobots[idx] }
                yieldAll(applySimulatingBoosters(sim, idx))

                val target = sim.gameMap
                        .closestFrom(currentRobot().pos) { point, cell ->
                            point in points && cell.status == Status.EMPTY
                        }
                        .firstOrNull()

                target ?: break

                val local = simulatingAStar(sim, target.first, idx)
                yieldAll(local)
            }
        }.withIdx(idx)

fun evenSmarterPrioritySimulatingAstarBot(simref: MutableRef<Simulator>, points: Set<Point>, idx: Int = 0) =
        sequence {
            while (true) {
                val sim by simref

                val currentRobot = { sim.currentRobots[idx] }

                yieldAll(applySimulatingBoosters(sim, idx))

                val closestBooster = checkNearestBooster(sim, currentRobot()) {
                    it.booster == BoosterType.MANIPULATOR_EXTENSION || it.booster == BoosterType.FAST_WHEELS
                }

                if (closestBooster != null) {
                    val local = simulatingAStarForWalking(sim, closestBooster, idx)
                    yieldAll(local)
                }

                yieldAll(applySimulatingBoosters(sim, idx))

                val target = sim.gameMap
                        .closestFrom(currentRobot().pos) { point, cell ->
                            point in points && cell.status == Status.EMPTY
                        }
                        .firstOrNull()

                target ?: break

                val local = simulatingAStar(sim, target.first, idx)
                yieldAll(local)
            }
        }.withIdx(idx)

fun theMostSmartestPrioritySimulatingAstarBot(simref: MutableRef<Simulator>, points: Set<Point>, idx: Int = 0) =
        sequence {
            while (true) {
                val sim by simref
                val currentRobot = { sim.currentRobots[idx] }

                yieldAll(applySimulatingBoosters(sim, idx))

                val target = sim.gameMap
                        .closestFrom(currentRobot().pos) { point, cell ->
                            point in points && cell.status == Status.EMPTY
                        }
                        .firstOrNull()

                target ?: break

                val local = simulatingAStar(sim, target.first, idx)
                for (command in local) {
                    val booster = checkNearestBooster(sim, currentRobot()) {
                        it.booster == BoosterType.MANIPULATOR_EXTENSION || it.booster == BoosterType.FAST_WHEELS
                    }
                    if (booster != null) {
                        val pathToBooster = simulatingAStarForWalking(sim, booster, idx)
                        yieldAll(pathToBooster)
                        break
                    }
                    yield(command)
                }
            }
        }.withIdx(idx)

// this method is too complex and also fails, so just get rid of it
//fun theMostSmartestPrioritySimulatingEnclosedAstarBot(simref: MutableRef<Simulator>, points: Set<Point>, idx: Int = 0) =
//        sequence {
//            while (true) {
//                val sim by simref
//                val currentRobot = { sim.currentRobots[idx] }
//
//                log.error("Current robot position: ${currentRobot().pos}")
//                yieldAll(applySimulatingBoosters(sim, idx))
//
//                val target = sim.gameMap
//                        .closestFrom(currentRobot().pos) {
//                            point, cell -> point in points && cell.status == Status.EMPTY
//                        }
//                        .take(20)
//                        .sortedBy { sim.gameMap.enclosedArea(it.first, 10) }
//                        .firstOrNull()
//
//                target ?: break
//                log.error("Target: ${target}")
//
//                val local = visibleAstarWalk(sim, target.first, idx)
//                for (command in local) {
//                    val booster = checkNearestBooster(sim, currentRobot()) {
//                        it.booster == BoosterType.MANIPULATOR_EXTENSION || it.booster == BoosterType.FAST_WHEELS
//                    }
//                    if (booster != null) {
//                        val pathToBooster = simulatingAStarForWalking(sim, booster, idx)
//                        yieldAll(pathToBooster)
//                        break
//                    }
//                    log.error("Robot position before: ${currentRobot().pos}")
//                    log.error("Active boosters before: ${currentRobot().activeBoosters}")
//                    log.error("Executing command: $command")
//                    yield(command)
//                    log.error("Robot position after: ${currentRobot().pos}")
//                    log.error("Active boosters after: ${currentRobot().activeBoosters}")
//                }
//            }
//        }.withIdx(idx)