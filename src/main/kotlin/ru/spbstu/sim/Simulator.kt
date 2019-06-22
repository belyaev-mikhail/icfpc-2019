package ru.spbstu.sim

import org.organicdesign.fp.collections.ImList
import org.organicdesign.fp.collections.PersistentVector
import ru.spbstu.map.*
import ru.spbstu.map.BoosterType.*
import ru.spbstu.map.Status.*
import ru.spbstu.util.dec
import ru.spbstu.util.inc
import java.awt.Color
import java.awt.Color.*
import java.awt.Dimension
import java.awt.Graphics
import java.awt.Graphics2D
import javax.swing.JFrame
import javax.swing.JPanel

sealed class Command

sealed class MoveCommand : Command() {
    abstract val dir: Point
}

object MOVE_UP : MoveCommand() {
    override val dir: Point = Point(0, 1)

    override fun toString(): String = "W"
}

object MOVE_DOWN : MoveCommand() {
    override val dir: Point = Point(0, -1)

    override fun toString(): String = "S"
}

object MOVE_LEFT : MoveCommand() {
    override val dir: Point = Point(-1, 0)

    override fun toString(): String = "A"
}

object MOVE_RIGHT : MoveCommand() {
    override val dir: Point = Point(1, 0)

    override fun toString(): String = "D"
}

object NOOP : Command() {
    override fun toString(): String = "Z"
}

object TURN_CW : Command() {
    override fun toString(): String = "E"
}

object TURN_CCW : Command() {
    override fun toString(): String = "Q"
}

object USE_FAST_WHEELS : Command() {
    override fun toString(): String = "F"
}

object USE_DRILL : Command() {
    override fun toString(): String = "L"
}

data class ATTACH_MANUPULATOR(val x: Int, val y: Int) : Command() {
    constructor(p: Point) : this(p.v0, p.v1)

    override fun toString(): String = "B($x,$y)"
}

object RESET : Command() {
    override fun toString(): String = "R"
}

data class SHIFT_TO(val x: Int, val y: Int) : Command() {
    constructor(p: Point) : this(p.v0, p.v1)

    override fun toString(): String = "T($x,$y)"
}

object CLONE : Command() {
    override fun toString(): String = "C"
}

object TICK : Command() {
    override fun toString(): String = ""
}

object NOT_EXIST: Command() {
    override fun toString(): String = "N"
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
                 val activeBoosters: Map<BoosterType, Int> = mutableMapOf()) {

    fun doCommand(cmd: Command) = when (cmd) {
        is MoveCommand -> move(cmd.dir)
        is TURN_CW -> rotateCW()
        is TURN_CCW -> rotateCCW()
        is ATTACH_MANUPULATOR -> attachManipulator(cmd.x, cmd.y)
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

    fun attachManipulator(x: Int, y: Int): Robot {
        val newManipulators = manipulators + Point(x, y)

        return this.copy(manipulators = newManipulators)
    }

    val manipulatorPos: List<Point>
        get() {
            val res = mutableListOf<Point>()

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

fun Simulator(currentRobot: Robot, gameMap: GameMap) =
        Simulator(null, PersistentVector.ofIter(listOf(currentRobot)), gameMap).repaint(0)

data class Simulator
internal constructor(val ignore: Any?,
                     val currentRobots: ImList<Robot>,
                     val gameMap: GameMap,
                     val time: Int = 0,
                     val boosters: Map<BoosterType, Int> = mapOf(),
                     val freshBoosters: Map<BoosterType, Int> = mapOf(),
                     val teleports: Set<Point> = setOf()) {

    fun die(idx: Int, msg: String): Nothing = throw SimulatorException(msg, currentRobots[idx], gameMap)

    fun repaint(idx: Int): Simulator {
        var newGameMap = gameMap

        val oldBotCell = newGameMap[currentRobots[idx].pos]

        newGameMap = newGameMap.set(
                currentRobots[idx].pos,
                oldBotCell.copy(status = WRAP)
        )

        for (mp in currentRobots[idx].manipulatorPos) {
            val (status, _) = newGameMap[mp]

            if (status != EMPTY) continue

            if (!newGameMap.isVisible(currentRobots[idx].pos, mp)) continue

            newGameMap = newGameMap.set(mp, newGameMap[mp].copy(status = WRAP))
        }
        return copy(gameMap = newGameMap)
    }

    @Deprecated("Should use version with explicit bot index")
    val currentRobot: Robot
        get() = currentRobots[0]

    @Deprecated("Should use version with explicit bot index")
    fun apply(cmd: Command, nested: Boolean = false): Simulator {
        return apply(0, cmd, nested)
    }

    fun apply(idx: Int, cmd: Command, nested: Boolean = false): Simulator {

        // nested == true means we're processing second part of fast wheels

        var newGameMap = gameMap
        var newCurrentRobot = currentRobots[idx]
        var newTeleports = teleports
        var newBoosters = boosters
        var newFreshBoosters = freshBoosters
        var newTime = time

        var shouldClone = false

        val (_, newBooster) = newGameMap[newCurrentRobot.pos]

        when (newBooster) {
            null, MYSTERY -> {
                // Do nothing
            }
            else -> {
                newFreshBoosters = newFreshBoosters.inc(newBooster)
                newGameMap = newGameMap.set(
                        newCurrentRobot.pos,
                        Cell.Wrap
                )
            }
        }

        when (cmd) {
            is MoveCommand -> {
                val newPos = newCurrentRobot.pos.moveTo(cmd.dir)

                val (newCell, _) = gameMap[newPos]

                when (newCell) {
                    EMPTY, WRAP -> {
                    }
                    WALL -> {
                        val hasDrill = DRILL in newCurrentRobot.activeBoosters

                        if (!nested && !hasDrill) die(idx, "Cannot move through wall without a drill")
                        else if (nested && !hasDrill) return this
                    }
                    SUPERWALL -> {
                        if (nested) return this
                        die(idx, "Cannot move through outer wall")
                    }
                }

                newCurrentRobot = newCurrentRobot.copy(pos = newPos)
            }

            is NOOP -> {
            }

            is TURN_CW -> {
                newCurrentRobot = newCurrentRobot.rotateCW()
            }

            is TURN_CCW -> {
                newCurrentRobot = newCurrentRobot.rotateCCW()
            }

            is USE_FAST_WHEELS -> {
                if (FAST_WHEELS !in newBoosters) die(idx, "Cannot use fast wheels")
                newBoosters = newBoosters.dec(FAST_WHEELS)

                val activeBoosters = newCurrentRobot.activeBoosters.toMutableMap()
                activeBoosters[FAST_WHEELS] = FAST_WHEELS.timer + 1 // will tick down immediately

                newCurrentRobot = newCurrentRobot.copy(activeBoosters = activeBoosters)
            }

            is USE_DRILL -> {
                if (DRILL !in newBoosters) die(idx, "Cannot use drill")
                newBoosters = newBoosters.dec(FAST_WHEELS)

                val activeBoosters = newCurrentRobot.activeBoosters.toMutableMap()
                activeBoosters[DRILL] = DRILL.timer + 1 // will tick down immediately

                newCurrentRobot = newCurrentRobot.copy(activeBoosters = activeBoosters)
            }

            is ATTACH_MANUPULATOR -> {
                if (MANIPULATOR_EXTENSION !in newBoosters) die(idx, "Cannot use manipulator extension")
                newBoosters = newBoosters.dec(MANIPULATOR_EXTENSION)

                val manupulators = newCurrentRobot.manipulators.toMutableList()
                manupulators.add(Point(cmd.x, cmd.y))

                newCurrentRobot = newCurrentRobot.copy(manipulators = manupulators)
            }

            is RESET -> {
                if (TELEPORT !in newBoosters) die(idx, "Cannot use teleport")
                newBoosters = newBoosters.dec(TELEPORT)

                if (gameMap[newCurrentRobot.pos].booster == MYSTERY || newCurrentRobot.pos in newTeleports)
                    die(idx, "Cannot reset teleport here")

                newTeleports += newCurrentRobot.pos
            }

            is SHIFT_TO -> {
                val newPos = Point(cmd.x, cmd.y)

                if (newPos !in newTeleports)
                    die(idx, "Cannot shift with $cmd")

                newCurrentRobot = newCurrentRobot.copy(pos = newPos)
            }

            is CLONE -> {
                if (CLONING !in newBoosters) die(idx, "Cannot use clone")
                newBoosters = newBoosters.dec(CLONING)

                if (gameMap[newCurrentRobot.pos].booster != MYSTERY)
                    die(idx, "Cannot use clone here")

                shouldClone = true
            }

            is TICK -> {
                newTime += 1

                for ((k, v) in newBoosters) {
                    newBoosters = newBoosters + (k to v + (newFreshBoosters[k] ?: 0))
                }
                for ((k, v) in newFreshBoosters) {
                    if (k in newBoosters) continue
                    newBoosters = newBoosters + (k to v)
                }

                newFreshBoosters = emptyMap()
            }
        }

        var newCurrentRobots = currentRobots.replace(idx, newCurrentRobot)
        if (shouldClone) newCurrentRobots = newCurrentRobots.append(Robot(newCurrentRobot.pos))

        var newSim = this.copy(
                gameMap = newGameMap,
                currentRobots = newCurrentRobots,
                boosters = newBoosters,
                freshBoosters = newFreshBoosters,
                teleports = newTeleports,
                time = newTime
        ).repaint(idx)

        if (!nested && cmd is MoveCommand && FAST_WHEELS in newSim.currentRobots[idx].activeBoosters) {
            newSim = newSim.apply(idx, cmd, true)
        }

        return if (!nested) newSim.tick(idx) else newSim
    }

    fun tick(idx: Int): Simulator {
        val newCurrentRobot = currentRobots[idx].tick()
        return copy(
                currentRobots = currentRobots.replace(idx, newCurrentRobot)
        )
    }

    val hasSolved by lazy { gameMap.cells.all<Point, Cell> { it.value.status != EMPTY } }

}

class SimFrame(val cellSize: Int, val mutSim: () -> Simulator) : JFrame() {
    init {
        add(panel())
        pack()
        isVisible = true

    }

    fun panel(): JPanel =
            object : JPanel() {
                init {
                    with(mutSim().gameMap) {
                        preferredSize = Dimension((maxX - minX + 2) * cellSize, (maxY - minY + 2) * cellSize)
                        minimumSize = preferredSize
                        maximumSize = preferredSize
                    }
                }

                override fun paint(g: Graphics?) =
                        with(mutSim()) {
                            with(gameMap) {
                                super.paint(g)
                                g as Graphics2D

                                g.background = BLACK

                                fun drawPoint(point: Point) = with(point) {
                                    g.fillRect((v0 + 1) * cellSize, (maxY - v1) * cellSize, cellSize, cellSize)
                                    g.paint = DARK_GRAY
                                    g.drawRect((v0 + 1) * cellSize, (maxY - v1) * cellSize, cellSize, cellSize)
                                }

                                for (y in (-1 + minY)..(maxY + 1)) {
                                    for (x in (-1 + minX)..(maxX + 1)) {
                                        val p = Point(x, y)

                                        val (status, booster) = cells[p]
                                                ?: Cell.Wall

                                        when (status) {
                                            WALL, SUPERWALL -> g.paint = BLACK
                                            EMPTY -> g.paint = WHITE
                                            WRAP -> g.paint = GRAY
                                            else -> g.paint = CYAN
                                        }

                                        when (booster) {
                                            BoosterType.MANIPULATOR_EXTENSION -> g.paint = Color.YELLOW.darker()
                                            BoosterType.FAST_WHEELS -> g.paint = Color(0xB5651D).darker()
                                            BoosterType.DRILL -> g.paint = Color.GREEN
                                            BoosterType.MYSTERY -> g.paint = Color.BLUE
                                            BoosterType.TELEPORT -> g.paint = Color.MAGENTA
                                            BoosterType.CLONING -> g.paint = Color.PINK
                                        }

                                        if (p in teleports) g.paint = Color.PINK

                                        // TODO: handle wrap + booster

                                        drawPoint(p)
                                    }
                                }

                                for (currentRobot in currentRobots) {
                                    for (manip in currentRobot.manipulatorPos) {
                                        g.paint = YELLOW
                                        drawPoint(manip)
                                    }

                                    g.paint = RED
                                    drawPoint(currentRobot.pos)
                                }
                            }
                        }
            }

}
