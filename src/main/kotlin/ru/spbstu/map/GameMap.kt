package ru.spbstu.map

import ru.spbstu.ktuples.Tuple2
import ru.spbstu.parse.Task

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

data class GameMap(
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

                    if (mapPath.contains(p)) {
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
}
