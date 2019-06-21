package ru.spbstu.map

import ru.spbstu.ktuples.Tuple2
import ru.spbstu.map.Map.GUISettings.cellSize
import ru.spbstu.parse.Task
import java.awt.Color
import java.awt.Dimension
import java.awt.Graphics
import java.awt.Graphics2D
import javax.swing.JFrame
import javax.swing.JPanel

enum class Status(val ascii: String) {
    EMPTY("."), WRAP("+"), WALL("#"), BOOSTER_B("B"), BOOSTER_F("F"), BOOSTER_L("L"), BOOSTER_X("X");

    companion object {
        fun fromBoosterType(type: String) = when (type) {
            "B" -> BOOSTER_B
            "F" -> BOOSTER_F
            "L" -> BOOSTER_L
            "X" -> BOOSTER_X
            else -> TODO()
        }
    }

    fun toASCII(): String = ascii
}

typealias Point = Tuple2<Int, Int>

typealias Shape = List<Point>

data class Booster(val coords: Point, val type: Status)

typealias Obstacle = Shape

data class Map(
        val corners: List<Point>,
        val obstacles: List<Obstacle>,
        val boosters: List<Booster>) {

    constructor(task: Task) : this(task.map, task.obstacles, task.boosters)

    val cells = mutableMapOf<Point, Status>()

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
                    cells[p] = Status.EMPTY
                } else {
                    cells[p] = Status.WALL
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
                        cells[p] = Status.WALL
                    }
                }
            }
        }

        for ((coords, status) in boosters) {
            cells[coords] = status
        }
    }

    operator fun get(p: Point): Status = cells[p] ?: Status.WALL

    operator fun set(p: Point, s: Status) {
        cells[p] = s
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

    private object GUISettings {
        const val cellSize = 10
    }

    fun toPanel(): JPanel {
        return object: JPanel() {
            init {
                this.preferredSize = Dimension((maxX - minX + 2) * cellSize, (maxY - minY + 2) * cellSize)
                this.minimumSize = this.preferredSize
                this.maximumSize = this.preferredSize
            }

            override fun paint(g: Graphics?) {
                super.paint(g)
                g as Graphics2D

                g.background = Color.BLACK

                for (y in (-1)..(maxY + 1)) {
                    for (x in (-1)..(maxX + 1)) {
                        val p = Point(x, y)

                        when(cells[p]) {
                            null, Status.WALL -> g.paint = Color.BLACK
                            Status.EMPTY -> g.paint = Color.WHITE
                            Status.WRAP -> g.paint = Color.GRAY
                            Status.BOOSTER_B -> g.paint = Color.YELLOW.darker()
                            Status.BOOSTER_F -> g.paint = Color(0xB5651D).darker()
                            Status.BOOSTER_L -> g.paint = Color.GREEN
                            Status.BOOSTER_X -> g.paint = Color.BLUE
                            else -> g.paint = Color.CYAN
                        }
                        g.fillRect((x + 1) * cellSize, (maxY - y) * cellSize, cellSize, cellSize)
                        when(cells[p]) {
                            //Status.EMPTY, null, Status.WALL -> {}
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

    fun display() {
        val frame = JFrame()
        frame.add(toPanel())
        frame.pack()
        frame.isVisible = true
    }
}
