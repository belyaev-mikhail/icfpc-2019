package ru.spbstu.sim

import ru.spbstu.map.*
import ru.spbstu.map.BoosterType.*
import ru.spbstu.map.Point
import ru.spbstu.map.Status.*
import ru.spbstu.util.dec
import ru.spbstu.util.inc
import java.awt.*
import java.awt.Color
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
    override fun toString(): String = "B($x,$y)"
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
                 val boosters: Map<BoosterType, Int> = mutableMapOf(),
                 val activeBoosters: Map<BoosterType, Int> = mutableMapOf()) {

    fun doCommand(cmd: Command) = when (cmd) {
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

class Simulator(val initialRobot: Robot, val initialGameMap: GameMap) {

    var currentRobot: Robot = initialRobot
    var gameMap: GameMap = initialGameMap

    fun die(msg: String): Nothing = throw SimulatorException(msg, currentRobot, gameMap)

    fun repaint() {
        gameMap[currentRobot.pos] = Cell(WRAP)

        for (mp in currentRobot.manipulatorPos) {
            // TODO: handle visibility
            val (status, _) = gameMap[mp]

            if (status != EMPTY) continue

            if (!gameMap.isVisible(currentRobot.pos, mp)) continue

            gameMap[mp] = gameMap[mp].copy(status = WRAP)
        }
    }

    init {
        repaint()
    }

    fun apply(cmd: Command, nested: Boolean = false) {

        // nested == true means we're processing second part of fast wheels

        when (cmd) {
            is MoveCommand -> {
                val newPos = currentRobot.pos.moveTo(cmd.dir)

                val (newCell, newBooster) = gameMap[newPos]

                when (newCell) {
                    EMPTY, WRAP -> {
                    }
                    WALL -> {
                        val hasDrill = DRILL in currentRobot.activeBoosters

                        if (!nested && !hasDrill) die("Cannot move through wall without a drill")
                        else if (nested && !hasDrill) return
                    }
                    SUPERWALL -> {
                        if (nested) return
                        die("Cannot move through outer wall")
                    }
                }

                when (newBooster) {
                    null -> {
                        // Do nothing
                    }
                    else -> {
                        val newBoosters = currentRobot.boosters.toMutableMap()
                        newBoosters.inc(newBooster)
                        currentRobot = currentRobot.copy(boosters = newBoosters)
                    }
                }

                currentRobot = currentRobot.copy(pos = newPos)
            }

            is NOOP -> {
            }

            is TURN_CW -> {
                currentRobot = currentRobot.rotateCW()
            }

            is TURN_CCW -> {
                currentRobot = currentRobot.rotateCCW()
            }

            is USE_FAST_WHEELS -> {
                val boosters = currentRobot.boosters.toMutableMap()
                if (FAST_WHEELS !in boosters) die("Cannot use fast wheels")
                boosters.dec(FAST_WHEELS)

                val activeBoosters = currentRobot.activeBoosters.toMutableMap()
                activeBoosters[FAST_WHEELS] = FAST_WHEELS.timer + 1 // will tick down immediately

                currentRobot = currentRobot.copy(boosters = boosters, activeBoosters = activeBoosters)
            }

            is USE_DRILL -> {
                val boosters = currentRobot.boosters.toMutableMap()
                if (DRILL !in boosters) die("Cannot use drill")
                boosters.dec(DRILL)

                val activeBoosters = currentRobot.activeBoosters.toMutableMap()
                activeBoosters[DRILL] = DRILL.timer + 1 // will tick down immediately

                currentRobot = currentRobot.copy(boosters = boosters, activeBoosters = activeBoosters)
            }

            is ATTACH_MANUPULATOR -> {
                val boosters = currentRobot.boosters.toMutableMap()
                if (MANIPULATOR_EXTENSION !in boosters) die("Cannot use manipulator extension")
                boosters.dec(MANIPULATOR_EXTENSION)

                val manupulators = currentRobot.manipulators.toMutableList()
                manupulators.add(Point(cmd.x, cmd.y))

                currentRobot = currentRobot.copy(boosters = boosters, manipulators = manupulators)
            }
        }

        repaint()

        if (!nested && cmd is MoveCommand && FAST_WHEELS in currentRobot.activeBoosters) {
            apply(cmd, true)
        }

        if (!nested) currentRobot = currentRobot.tick()
    }

    fun toPanel(cellSize: Int): JPanel = with(gameMap) {
        return object : JPanel() {
            init {
                this.preferredSize = Dimension((maxX - minX + 2) * cellSize, (maxY - minY + 2) * cellSize)
                this.minimumSize = this.preferredSize
                this.maximumSize = this.preferredSize
            }

            override fun paint(g: Graphics?) {
                super.paint(g)
                g as Graphics2D

                g.background = Color.BLACK

                fun drawPoint(point: Point) = with(point) {
                    g.fillRect((v0 + 1) * cellSize, (maxY - v1) * cellSize, cellSize, cellSize)
                    g.paint = Color.DARK_GRAY
                    g.drawRect((v0 + 1) * cellSize, (maxY - v1) * cellSize, cellSize, cellSize)
                }

                for (y in (-1 + minY)..(maxY + 1)) {
                    for (x in (-1 + minX)..(maxX + 1)) {
                        val p = Point(x, y)

                        val (status, booster) = cells[p] ?: Cell.Wall

                        when (status) {
                            Status.WALL -> g.paint = Color.BLACK
                            Status.EMPTY -> g.paint = Color.WHITE
                            Status.WRAP -> g.paint = Color.GRAY
                            else -> g.paint = Color.CYAN
                        }

                        when (booster) {
                            BoosterType.MANIPULATOR_EXTENSION -> g.paint = Color.YELLOW.darker()
                            BoosterType.FAST_WHEELS -> g.paint = Color(0xB5651D).darker()
                            BoosterType.DRILL -> g.paint = Color.GREEN
                            BoosterType.MYSTERY -> g.paint = Color.BLUE
                        }

                        // TODO: handle wrap + booster

                        drawPoint(p)
                    }
                }

                for(manip in currentRobot.manipulatorPos) {
                    g.paint = Color.YELLOW
                    drawPoint(manip)
                }

                g.paint = Color.RED
                drawPoint(currentRobot.pos)


            }
        }
    }

    fun display(cellSize: Int): JFrame {
        val frame = JFrame()
        frame.add(toPanel(cellSize))
        frame.pack()
        frame.isVisible = true
        return frame

        // frame.defaultCloseOperation = WindowConstants.EXIT_ON_CLOSE
    }
}
