package ru.spbstu.player

import ru.spbstu.ktuples.Tuple
import ru.spbstu.ktuples.Tuple2
import ru.spbstu.map.Point
import ru.spbstu.map.Status
import ru.spbstu.map.manhattanDistance
import ru.spbstu.sim.*
import ru.spbstu.wheels.*
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

        println("Peeking element $peek with score ${heur(peek)} and len $len")
        println("peek in closed: ${peek in closed}")

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

    check(sim.gameMap[target] != Status.WALL)

    return aStarSearch(
            RobotAndCommand(robot, USE_DRILL),
            heur = { (robot, _) ->
                (robot.getWrap().map { it.manhattanDistance(target) }.min() ?: Int.MAX_VALUE).toDouble()
            },
            goal = { (robot, _) ->
                robot.getWrap().contains(target)
            },
            neighbours = { (me, _) ->
                val commands = listOf(TURN_CW, TURN_CCW, MOVE_UP, MOVE_RIGHT, MOVE_LEFT, MOVE_DOWN)
                commands.map { RobotAndCommand(me.doCommand(it), it) }
                        .asSequence()
                        .filter { sim.gameMap[it.v0.pos] != Status.WALL }
            }
    ).orEmpty().map { it.v1 }
}

