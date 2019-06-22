package ru.spbstu.map

import ru.spbstu.sim.Orientation
import java.awt.Color
import java.awt.Dimension
import java.awt.Graphics
import java.awt.geom.Path2D
import java.util.*
import javax.swing.JFrame
import kotlin.collections.ArrayList
import kotlin.collections.HashSet
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

data class DummyPoint(val x: Int, val y: Int) : Comparable<DummyPoint> {
    constructor(p: Point) : this(p.v0, p.v1)
    constructor(l: List<Int>) : this(l[0], l[1])

    fun movedBy(dx: Int = 0, dy: Int = 0) = DummyPoint(x + dx, y + dy)
    fun multipliedBy(scale: Int) = DummyPoint(x * scale, y * scale)

    fun toIntList() = listOf(x, y)

    operator fun get(id: Int): Int = when (id) {
        0 -> x
        1 -> y
        else -> throw IllegalArgumentException()
    }

    override fun compareTo(other: DummyPoint) = if (x == other.x) y.compareTo(other.y)
    else x.compareTo(other.x)

    override fun toString() = "($x, $y)"
}


class Scaler {

    val scaleMapping = HashMap<DummyPoint, List<DummyPoint>>()
    val unscaleMapping = HashMap<DummyPoint, DummyPoint>()


    fun scale(point: DummyPoint) = scaleMapping.computeIfAbsent(point) {
        val basePoint = it.multipliedBy(scale)
        val scaling = (0 until scale).map { dx ->
            (0 until scale).map { dy ->
                basePoint.movedBy(dx, dy)
            }
        }.flatten().toSet()
        scaling.forEach { sp -> unscaleMapping[sp] = it }
        return@computeIfAbsent scaling.toList()
    }


    fun unscale(point: DummyPoint) = unscaleMapping[point.movedBy(1, 1)]!!

    fun unscaleRectangle(rectangle: Pair<DummyPoint, DummyPoint>): Pair<DummyPoint, DummyPoint> {
        val (p1, p2) = rectangle
        val first = unscaleMapping[p1]!!
        val second = unscaleMapping[p2]!!
        return first to second
    }

    companion object {
        const val scale = 3 // must be 3 or greater
    }

}

fun rectilinearBoundary(whites: List<DummyPoint>, blacks: List<DummyPoint>): List<DummyPoint> {

    val scaler = Scaler()
    blacks.forEach { scaler.scale(it) }
    val points = whites.flatMap { scaler.scale(it) }

    val boundaryPoints = HashSet<DummyPoint>().toMutableSet()
    val allPoints = points.toSet()

    val visitedPoints = HashSet<DummyPoint>()
    val queue = LinkedList<DummyPoint>()
    queue.add(allPoints.first())

    while (queue.isNotEmpty()) {
        val point = queue.poll()
        if (visitedPoints.contains(point)) continue
        if (!allPoints.contains(point)) continue

        visitedPoints.add(point)

        for (dx in listOf(-1, 0, 1))
            for (dy in listOf(-1, 0, 1)) {
                if (dx == 0 && dy == 0) continue
                val p = point.movedBy(dx, dy)
                if (allPoints.contains(p)) queue.add(p)
            }

        run {
            val dx = 1
            val dy = 1
            val p = point.movedBy(dx, dy)
            if(!allPoints.contains(p)) boundaryPoints.add(point)
        }

        run {
            val dx = -1
            val dy = -1
            val p = point.movedBy(dx, dy)
            if(!allPoints.contains(p)) boundaryPoints.add(p)
        }

//        for (dx in listOf(-1, 0, 1))
//            for (dy in listOf(-1, 0, 1)) {
//                if (dx == 0 && dy == 0) continue
//                val p = point.movedBy(dx, dy)
//                if (allPoints.contains(p)) queue.add(p)
//                else boundaryPoints.add(point)
//            }
    }


    val (boundary, pointsOnBoundary) = findBoundary(boundaryPoints.toList())
    //Draw(boundary.toSet(), 600, 600).apply { repaint() }

    return boundary.map { scaler.unscale(it) }.toSet().toList()

}

class Draw(val points: Set<DummyPoint>, val mapHeight: Int, val mapWidth: Int) : JFrame() {
    var redPoint: DummyPoint = DummyPoint(0, 0)
    private val SIZE = 4

    init {
        this.preferredSize = Dimension((10 + mapWidth) * SIZE, (10 + mapHeight) * SIZE)
        this.pack()
        this.isVisible = true
        this.defaultCloseOperation = EXIT_ON_CLOSE
    }

    override fun paint(g: Graphics) {
        super.paint(g)

        g.color = Color.WHITE
        g.fillRect(0, 0, (10 + mapWidth) * SIZE, (10 + mapHeight) * SIZE)
        g.color = Color.BLUE
        for(p in points) {
            g.fillRect((10 + p.x) * SIZE, (10 + p.y) * SIZE, SIZE, SIZE)
        }
        g.color = Color.RED
        g.fillRect((10 + redPoint.x) * SIZE, (10 + redPoint.y) * SIZE, SIZE * 4, SIZE * 4)
    }
}

fun findBoundary(boundaryPoints: List<DummyPoint>): Pair<List<DummyPoint>, Set<DummyPoint>> {
    val orderedBoundaryPoints = boundaryPoints.sorted()
    val boundaryPointsSet = orderedBoundaryPoints.toSet()
    val pointsOnBoundary = HashSet<DummyPoint>()

    var point = orderedBoundaryPoints.first()

    val boundary = ArrayList<DummyPoint>()
    boundary.add(point)

    var xDirection = 1
    var yDirection = 0

    while (true) {

        pointsOnBoundary.add(point)

        val nexPoint = point.movedBy(xDirection, yDirection)

        if (boundaryPointsSet.contains(nexPoint)) {
            println(nexPoint)
            point = nexPoint
            continue
        }

        if (boundary.size > 2 && point == boundary.first()) break

        boundary.add(point)

        if (xDirection != 0) {
            val oldXDirection = xDirection
            val topNext = point.movedBy(0, 1)
            val botNext = point.movedBy(0, -1)
            val goTop = boundaryPointsSet.contains(topNext)
            val goBot = boundaryPointsSet.contains(botNext)

            xDirection = 0
            if (goTop) {
                point = topNext
                yDirection = 1
            } else if(goBot) {
                point = botNext
                yDirection = -1
            } else {
                xDirection = oldXDirection
                if(boundary.isNotEmpty()) boundary.removeAt(boundary.lastIndex)
                point = nexPoint
            }
        } else {
            val oldYDirection = yDirection
            val leftNext = point.movedBy(-1, 0)
            val rightNext = point.movedBy(1, 0)
            val goLeft = boundaryPointsSet.contains(leftNext)
            val goRight = boundaryPointsSet.contains(rightNext)
            yDirection = 0
            if (goLeft) {
                point = leftNext
                xDirection = -1
            } else if (goRight) {
                point = rightNext
                xDirection = 1
            }
            else {
                yDirection = oldYDirection
                if(boundary.isNotEmpty()) boundary.removeAt(boundary.lastIndex)
                point = nexPoint
            }
        }
    }

    return boundary to pointsOnBoundary
}


fun GameMap.getWindow(lst: List<Point>): List<Cell> = lst.map { this[it] }
fun GameMap.corners(): List<Point> {
    fun isCorner(lst: List<Point>) =
            abs(lst.count { get(it).status.isWall } - lst.count { get(it).status == Status.EMPTY }) > 1

    for(x in 0..maxX) {
        for(y in 0..maxY) {
            val window = listOf(Point(x-1,y-1), Point(x-1, y), Point(x, y-1), Point(x, y))
        }
    }

    while(true) {

    }

}

fun GameMap.rectilinearBoundary(): List<Point> {
    val res = rectilinearBoundary(
            this.cells.filter { it.value.status == Status.EMPTY }.mapTo(mutableListOf()) { DummyPoint(it.key) },
            this.cells.filter { it.value.status.isWall }.mapTo(mutableListOf()) { DummyPoint(it.key) })
            .orEmpty()

    return res.map { Point(it.x, it.y) }
}

fun GameMap.dump(): String {
    val sb = StringBuilder()
    sb.append(rectilinearBoundary().joinToString(",") { (x, y) -> "($x,$y)" })
    /**/
    return "$sb"
}
