package ru.spbstu.map

import ru.spbstu.ktuples.Tuple2
import java.awt.geom.Path2D

enum class Status {
    EMPTY, WRAP, WALL, BOOSTER_B, BOOSTER_F, BOOSTER_L, BOOSTER_X
}

typealias Point = Tuple2<Int, Int>

typealias Shape = List<Point>

fun Shape.toPath2D(): Path2D {
    val path2d = Path2D.Float()

    val (xStart, yStart) = first()

    path2d.moveTo(xStart.toDouble(), yStart.toDouble())

    for ((x, y) in drop(1)) {
        path2d.lineTo(x.toFloat(), y.toFloat())
    }

    path2d.closePath()

    return path2d
}

fun Path2D.contains(p: Point): Boolean = contains(p.v0 + 0.5, p.v1 + 0.5)

data class Obstacle(val corners: List<Point>)

data class Map(
        val corners: List<Point>,
        val obstacles: List<Obstacle>) {

    val cells = mutableMapOf<Point, Status>()

    init {
        val mapPath = corners.toPath2D()

        val mapBounds = mapPath.bounds

        for (x in mapBounds.minX.toInt()..mapBounds.maxX.toInt()) {
            for (y in mapBounds.minY.toInt()..mapBounds.maxY.toInt()) {
                val p = Point(x, y)

                if (mapPath.contains(p)) {
                    cells[p] = Status.EMPTY
                } else {
                    cells[p] = Status.WALL
                }
            }
        }

        for (obstacle in obstacles) {
            val obsPath = obstacle.corners.toPath2D()

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
    }
}
