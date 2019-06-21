package ru.spbstu.map

import ru.spbstu.sim.Orientation
import java.awt.geom.Path2D

fun Point.moveTo(dir: Point) = Point(v0 + dir.v0, v1 + dir.v1)

fun Point.moveTo(dir: Orientation) = Point(v0 + dir.dx, v1 + dir.dy)

fun Shape.toPath2D(): Path2D {
    val path2d = Path2D.Float()

    val (xStart, yStart) = first()

    path2d.moveTo(xStart.toFloat(), yStart.toFloat())

    for ((x, y) in drop(1)) {
        path2d.lineTo(x.toFloat(), y.toFloat())
    }

    path2d.closePath()

    return path2d
}

fun Path2D.contains(p: Point): Boolean = contains(p.v0 + 0.5, p.v1 + 0.5)
