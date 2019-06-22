package ru.spbstu.map

import ru.spbstu.sim.Orientation
import java.awt.geom.Path2D
import kotlin.math.abs
import kotlin.math.sqrt

fun Point.up() = Point(v0 + 1, v1)
fun Point.down() = Point(v0 - 1, v1)
fun Point.left() = Point(v0, v1 - 1)
fun Point.right() = Point(v0, v1 + 1)

fun Point.moveTo(dir: Point) = Point(v0 + dir.v0, v1 + dir.v1)

fun Point.manhattanDistance(that: Point) = maxOf(abs(this.v0 - that.v0), abs(this.v1 - that.v1))
fun sqr(d: Double) = d * d
fun Point.euclidDistance(that: Point) = sqrt(sqr(0.0 + this.v0 - that.v0) + sqr(0.0 + this.v1 - that.v1))

fun Point.moveTo(dir: Orientation) = Point(v0 + dir.dx, v1 + dir.dy)

fun Point.neighbours() = Orientation.values().map { this.moveTo(it) }

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

operator fun Point.times(other: Int) = Point(v0 * other, v1 * other)
operator fun Point.div(other: Int) = Point(v0 / other, v1 / other)
operator fun Point.plus(other: Int) = Point(v0 + other, v1 + other)
operator fun Point.plus(other: Point) = Point(v0 + other.v0, v1 + other.v1)
