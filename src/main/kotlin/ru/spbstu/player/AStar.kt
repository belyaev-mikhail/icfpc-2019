package ru.spbstu.player

import ru.spbstu.map.Point
import ru.spbstu.map.manhattanDistance
import ru.spbstu.sim.*
import ru.spbstu.wheels.getOption
import java.util.*

@PublishedApi
internal fun <T> reconstructPath(value: T, paths: MutableMap<T, T>): List<T> {
    val res = mutableListOf(value)
    var current = value
    while (true) {
        val interm = paths.getOption(current)
        if (interm.isNotEmpty()) {
            current = interm.get()
            res += current
        } else break
    }
    return res
}

inline fun <T> aStarSearch(from: T,
                           crossinline heur: (T) -> Double,
                           crossinline goal: (T) -> Boolean,
                           crossinline neighbours: (T) -> Sequence<T>): List<T>? {
    val closed = mutableSetOf<T>()
    val open: PriorityQueue<Pair<T, Int>> = PriorityQueue(compareBy { (v, l) -> heur(v) + l })
    val paths: MutableMap<T, T> = mutableMapOf()

    open += (from to 0)

    closed += from

    while (!open.isEmpty()) {
        val (peek, len) = open.remove()

        if (goal(peek)) return reconstructPath(peek, paths)

        for (e in neighbours(peek)) if (e !in closed) {
            paths[e] = peek
            open += (e to (len + 1))
            closed += e
        }
    }
    return null
}

data class RobotAndCommand(val v0: Robot, val v1: Command) {
    override fun equals(other: Any?) = other is RobotAndCommand
            && v0.pos == other.v0.pos
            && v0.orientation == other.v0.orientation

    override fun hashCode(): Int = Objects.hash(v0.pos, v0.orientation)
}

fun astarWalk(sim: Simulator, target: Point): List<Command> {
    val robot = sim.currentRobot

    check(!sim.gameMap[target].status.isWall)

    return aStarSearch(
            RobotAndCommand(robot, USE_DRILL),
            heur = { (robot, _) ->
                (robot.manipulatorPos.map { it.manhattanDistance(target) }.min()
                        ?: Int.MAX_VALUE).toDouble()
            },
            goal = { (robot, _) ->
                robot.manipulatorPos.contains(target) && sim.gameMap.isVisible(robot.pos, target)
            },
            neighbours = { (me, _) ->
                val commands = listOf(TURN_CW, TURN_CCW, MOVE_UP, MOVE_RIGHT, MOVE_LEFT, MOVE_DOWN)
                commands.map { RobotAndCommand(me.doCommand(it), it) }
                        .asSequence()
                        .filter { !sim.gameMap[it.v0.pos].status.isWall }
            }
    )?.dropLast(1).orEmpty().map { it.v1 }.reversed()
}

fun astarWithoutTurnsWalk(sim: Simulator, target: Point): List<Command> {
    val robot = sim.currentRobot

    check(!sim.gameMap[target].status.isWall)

    return aStarSearch(
            RobotAndCommand(robot, USE_DRILL),
            heur = { (robot, _) ->
                robot.pos.manhattanDistance(target).toDouble()
            },
            goal = { (robot, _) ->
                robot.pos == target
            },
            neighbours = { (me, _) ->
                val commands = listOf(MOVE_UP, MOVE_RIGHT, MOVE_LEFT, MOVE_DOWN)
                commands.map { RobotAndCommand(me.doCommand(it), it) }
                        .asSequence()
                        .filter { !sim.gameMap[it.v0.pos].status.isWall }
            }
    )?.dropLast(1).orEmpty().map { it.v1 }.reversed()
}

fun visibleAstarWalk(sim: Simulator, target: Point): List<Command> {
    val robot = sim.currentRobot

    check(!sim.gameMap[target].status.isWall)

    return aStarSearch(
            RobotAndCommand(robot, USE_DRILL),
            heur = { (robot, _) ->
                robot.manipulatorPos
                        .map { it.manhattanDistance(target).toDouble() + if (sim.gameMap.isVisible(robot.pos, it)) 0.1 else 0.0 }
                        .min()
                        ?: Double.MAX_VALUE
            },
            goal = { (robot, _) ->
                robot.manipulatorPos.contains(target) && sim.gameMap.isVisible(robot.pos, target)
            },
            neighbours = { (me, _) ->
                val commands = listOf(TURN_CW, TURN_CCW, MOVE_UP, MOVE_RIGHT, MOVE_LEFT, MOVE_DOWN)
                commands.map { RobotAndCommand(me.doCommand(it), it) }
                        .asSequence()
                        .filter { !sim.gameMap[it.v0.pos].status.isWall }
            }
    )?.dropLast(1).orEmpty().map { it.v1 }.reversed()
}