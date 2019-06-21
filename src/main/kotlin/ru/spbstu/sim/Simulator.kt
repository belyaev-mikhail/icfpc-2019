package ru.spbstu.sim

import org.organicdesign.fp.oneOf.Or
import ru.spbstu.map.GameMap
import ru.spbstu.map.Point
import ru.spbstu.map.Status.*
import ru.spbstu.map.moveTo
import ru.spbstu.sim.Booster.DRILL
import ru.spbstu.sim.Booster.FAST_WHEELS
import ru.spbstu.util.inc

interface Command

interface MoveCommand: Command {
    val dir: Point
}

object MOVE_UP : MoveCommand {
    override val dir: Point = Point(0, 1)

    override fun toString(): String = "W"
}

object MOVE_DOWN : MoveCommand {
    override val dir: Point = Point(0, -1)

    override fun toString(): String = "S"
}

object MOVE_LEFT : MoveCommand {
    override val dir: Point = Point(-1, 0)

    override fun toString(): String = "A"
}

object MOVE_RIGHT : MoveCommand {
    override val dir: Point = Point(1, 0)

    override fun toString(): String = "D"
}

object NOOP : Command {
    override fun toString(): String = "Z"
}

object TURN_CW : Command {
    override fun toString(): String = "E"
}

object TURN_CCW : Command {
    override fun toString(): String = "Q"
}

object USE_FAST_WHEELS : Command {
    override fun toString(): String = "F"
}

object USE_DRILL : Command {
    override fun toString(): String = "L"
}

data class ATTACH_MANUPULATOR(val x: Int, val y: Int) : Command {
    override fun toString(): String = "B($x,$y)"
}

enum class Booster(val timer: Int) {
    MANIPULATOR_EXTENSION(0), FAST_WHEELS(50), DRILL(30), MYSTERY(0)
}

enum class Orientation(val dx: Int, val dy: Int) {
    UP(0, 1), DOWN(0, -1), LEFT(-1, 0), RIGHT(1, 0);

    val rotateCW: Orientation by lazy {
        when (this) {
            UP -> RIGHT
            DOWN -> LEFT
            LEFT -> UP
            RIGHT -> DOWN
        }
    }

    val rotateCCW: Orientation by lazy {
        when (this) {
            UP -> LEFT
            DOWN -> RIGHT
            LEFT -> DOWN
            RIGHT -> UP
        }
    }
}

operator fun Point.plus(orientation: Point) = copy(v0 = v0 + orientation.v0, v1 = v1 + orientation.v1)

data class Robot(val pos: Point,
                 val orientation: Orientation = Orientation.RIGHT,
                 val manipulators: List<Point> = listOf(
                         Point(1, 1), Point(1, 0), Point(1, -1)
                 ),
                 val boosters: Map<Booster, Int> = mutableMapOf(),
                 val activeBoosters: Map<Booster, Int> = mutableMapOf()) {

    fun doCommand(cmd: Command) = when(cmd) {
        is MoveCommand -> move(cmd.dir)
        is TURN_CW -> rotateCW()
        is TURN_CCW -> rotateCCW()
        else -> this
    }

    fun move(dir: Point): Robot {
        return this.copy(pos = pos + dir)
    }

    fun rotateCW(): Robot {
        val newOrientation = orientation.rotateCW

        val newManipulators = manipulators.map { (dx, dy) -> Point(dy, -dx) }

        return this.copy(orientation = newOrientation, manipulators = newManipulators)
    }

    fun rotateCCW(): Robot {
        val newOrientation = orientation.rotateCCW

        val newManipulators = manipulators.map { (dx, dy) -> Point(-dy, dx) }

        return this.copy(orientation = newOrientation, manipulators = newManipulators)
    }

    fun getWrap(): List<Point> {
        val res = mutableListOf<Point>()

        res += pos

        for ((dx, dy) in manipulators) {
            res += Point(pos.v0 + dx, pos.v1 + dy)
        }

        return res
    }

    fun tick(): Robot {
        val newActiveBoosters = activeBoosters.mapValues { (_, remaining) -> remaining - 1 }
                .filterValues { it > 0 }

        return copy(activeBoosters = newActiveBoosters)
    }
}

class SimulatorException(msg: String, val robot: Robot, val map: GameMap) : Exception(msg)

class Simulator(val initialRobot: Robot, val initialGameMap: GameMap) {

    var currentRobot: Robot = initialRobot
    var gameMap: GameMap = initialGameMap

    fun die(msg: String): Nothing = throw SimulatorException(msg, currentRobot, gameMap)

    fun apply(cmd: Command) {
        with(currentRobot) {
            when (cmd) {
                is MoveCommand -> {
                    val newPos = pos.moveTo(cmd.dir)

                    val newCell = gameMap[newPos]

                    when (newCell) {
                        EMPTY, WRAP -> {
                        }
                        WALL -> {
                            if (DRILL !in activeBoosters) die("Cannot move through wall without a drill")
                        }
                        BOOSTER_F -> {
                            val newBoosters = boosters.toMutableMap()
                            newBoosters.inc(FAST_WHEELS)
                            currentRobot = currentRobot.copy(boosters = newBoosters)
                        }
                        BOOSTER_L -> {
                            val newBoosters = boosters.toMutableMap()
                            newBoosters.inc(FAST_WHEELS)
                            currentRobot = currentRobot.copy(boosters = newBoosters)
                        }
                        BOOSTER_X -> TODO()
                    }

                    currentRobot = currentRobot.copy(pos = newPos)
                }
            }
        }
    }
}
