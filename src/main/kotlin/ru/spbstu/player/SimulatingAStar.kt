package ru.spbstu.player

import ru.spbstu.map.*
import ru.spbstu.sim.*
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

    override fun hashCode(): Int = Objects.hash(robot.pos, robot.orientation, idx)
}

fun simulatingAStar(sim: Simulator, target: Point, idx: Int) = aStarSearch(
        SimulatorAndCommand(sim, NOOP, idx),
        heur = { e ->
            e.robot.manipulatorPos
                    .map { it.manhattanDistance(target).toDouble() + if (e.sim.gameMap.isVisible(e.robot.pos, it)) 0.1 else 0.0 }
                    .min()
                    ?: Double.MAX_VALUE
        },
        goal = { target in it.sim.currentRobots[it.idx].manipulatorPos && it.sim.gameMap.isVisible(it.robot.pos, target) },
        neighbours = { it.neighbors.asSequence() }
)?.dropLast(1).orEmpty().map { it.command }.reversed()

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
)?.dropLast(1).orEmpty().map { it.command }.reversed()

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

fun evenSmarterPrioritySimulatingAstarBot(simref: MutableRef<Simulator>, points: Set<Point>, idx: Int = 0) =
        sequence {
            while (true) {
                val sim by simref

                val currentRobot = { sim.currentRobots[idx] }

                yieldAll(applyBoosters(sim, idx))

                val closestBooster = checkNearestBooster(sim, currentRobot()) {
                    it.booster == BoosterType.MANIPULATOR_EXTENSION || it.booster == BoosterType.FAST_WHEELS
                }

                if (closestBooster != null) {
                    val local = simulatingAStarForWalking(sim, closestBooster, idx)
                    yieldAll(local)
                }

                yieldAll(applyBoosters(sim, idx))

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

                yieldAll(applyBoosters(sim, idx))

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

fun theMostSmartestPrioritySimulatingEnclosedAstarBot(simref: MutableRef<Simulator>, points: Set<Point>, idx: Int = 0) =
        sequence {
            while (true) {
                val sim by simref
                val currentRobot = { sim.currentRobots[idx] }

                yieldAll(applyBoosters(sim, idx))

                val target = sim.gameMap
                        .closestFrom(currentRobot().pos) {
                            point, cell -> point in points && cell.status == Status.EMPTY
                        }
                        .take(20)
                        .sortedBy { sim.gameMap.enclosedArea(it.first, 10) }
                        .firstOrNull()

                target ?: break

                val local = visibleAstarWalk(sim, target.first, idx)
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