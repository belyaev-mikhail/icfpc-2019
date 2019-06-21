package ru.spbstu.map

import ru.spbstu.ktuples.Tuple2
import ru.spbstu.map.Status.*
import ru.spbstu.parse.Task
import java.awt.Color
import java.awt.Dimension
import java.awt.Graphics
import java.awt.Graphics2D
import javax.swing.JFrame
import javax.swing.JPanel
import javax.swing.WindowConstants

class MapException(msg: String) : Exception(msg)

data class Cell(val status: Status, val booster: BoosterType?) {
    constructor(status: Status) : this(status, null)

    fun toASCII(): String = when (status) {
        EMPTY -> booster?.toASCII() ?: status.toASCII()
        WRAP -> booster?.toASCII()?.toLowerCase() ?: status.toASCII()
        WALL -> status.toASCII()
    }

    companion object {
        val Empty = Cell(EMPTY)
        val Wrap = Cell(WRAP)
        val Wall = Cell(WALL)
    }
}

enum class BoosterType(val timer: Int, val ascii: String) {
    MANIPULATOR_EXTENSION(0, "B"),
    FAST_WHEELS(50, "F"),
    DRILL(30, "L"),
    MYSTERY(0, "X");

    fun toASCII(): String = ascii

    companion object {
        val nameMap = values().map { it.ascii to it }.toMap()

        fun from(s: String) = nameMap[s]
                ?: throw MapException("Wrong booster type: $s")
    }
}

enum class Status(val ascii: String) {
    EMPTY("."), WRAP("+"), WALL("#");

    fun toASCII(): String = ascii
}

//typealias Point = Tuple2<Int, Int>
data class Point(val v0: Int, val v1: Int)


typealias Shape = List<Point>

data class Booster(val coords: Point, val type: BoosterType)

typealias Obstacle = Shape

data class GameMap(
        val corners: List<Point>,
        val obstacles: List<Obstacle>,
        val boosters: List<Booster>) {

    constructor(task: Task) : this(task.map, task.obstacles, task.boosters)

    val cells = mutableMapOf<Point, Cell>()

    val minX: Int
    val maxX: Int
    val minY: Int
    val maxY: Int

    init {
        val mapPath = corners.toPath2D()

        val mapBounds = mapPath.bounds

        minX = mapBounds.minX.toInt()
        maxX = mapBounds.maxX.toInt()
        minY = mapBounds.minY.toInt()
        maxY = mapBounds.maxY.toInt()

        for (x in minX..maxX) {
            for (y in minY..maxY) {
                val p = Point(x, y)

                if (mapPath.contains(p)) {
                    cells[p] = Cell.Empty
                } else {
                    cells[p] = Cell.Wall
                }
            }
        }

        for (obstacle in obstacles) {
            val obsPath = obstacle.toPath2D()

            val obsBounds = obsPath.bounds

            for (x in obsBounds.minX.toInt()..obsBounds.maxX.toInt()) {
                for (y in obsBounds.minY.toInt()..obsBounds.maxY.toInt()) {
                    val p = Point(x, y)

                    if (obsPath.contains(p)) {
                        cells[p] = Cell.Wall
                    }
                }
            }
        }

        for ((coords, status) in boosters) {
            // TODO: sanity check?
            cells[coords] = Cell(EMPTY, status)
        }
    }

    operator fun get(p: Point): Cell = cells[p] ?: Cell.Wall

    operator fun set(p: Point, c: Cell) {
        cells[p] = c
    }

    fun toASCII(): String {
        val res = StringBuilder()

        for (y in maxY downTo minY) {
            for (x in minX..maxX) {
                val p = Point(x, y)

                res.append(cells[p]?.toASCII())
            }
            res.append("\n")
        }

        return res.toString()
    }

    fun toPanel(cellSize: Int): JPanel {
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

                        g.fillRect((x + 1) * cellSize, (maxY - y) * cellSize, cellSize, cellSize)

                        when (cells[p]) {
                            // Status.EMPTY, null, Status.WALL -> {}
                            else -> {
                                g.paint = Color.DARK_GRAY
                                g.drawRect((x + 1) * cellSize, (maxY - y) * cellSize, cellSize, cellSize)
                            }
                        }
                    }
                }
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
