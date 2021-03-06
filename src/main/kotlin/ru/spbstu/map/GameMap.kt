package ru.spbstu.map

import org.organicdesign.fp.collections.ImMap
import org.organicdesign.fp.collections.PersistentHashMap
import ru.spbstu.ktuples.Tuple2
import ru.spbstu.map.Status.*
import ru.spbstu.parse.Task
import ru.spbstu.wheels.ArrayDeque
import ru.spbstu.wheels.memo
import kotlin.math.sign

class MapException(msg: String) : Exception(msg)

data class Cell(val status: Status, val booster: BoosterType?) {
    constructor(status: Status) : this(status, null)

    fun toASCII(): String = when (status) {
        EMPTY -> booster?.toASCII() ?: status.toASCII()
        WRAP -> booster?.toASCII()?.toLowerCase() ?: status.toASCII()
        WALL, SUPERWALL -> status.toASCII()
    }

    companion object {
        val Empty = Cell(EMPTY)
        val Wrap = Cell(WRAP)
        val Wall = Cell(WALL)
        val Superwall = Cell(SUPERWALL)
    }
}

enum class BoosterType(val timer: Int, val ascii: String) {
    MANIPULATOR_EXTENSION(0, "B"),
    FAST_WHEELS(50, "F"),
    DRILL(30, "L"),
    MYSTERY(0, "X"),
    TELEPORT(0, "R"),
    CLONING(0, "C");

    fun toASCII(): String = ascii

    companion object {
        val nameMap = values().map { it.ascii to it }.toMap()

        fun from(s: String) = nameMap[s]
                ?: throw MapException("Wrong booster type: $s")
    }
}

enum class Status(val ascii: String) {
    EMPTY("."), WRAP("+"), WALL("#"), SUPERWALL("@");

    fun toASCII(): String = ascii

    val isWall by lazy {
        when (this) {
            WALL, SUPERWALL -> true
            else -> false
        }
    }
}

typealias Point = Tuple2<Int, Int>

typealias Shape = List<Point>

data class Booster(val coords: Point, val type: BoosterType)

typealias Obstacle = Shape

operator fun <K, V> ImMap<K, V>.plus(kv: Pair<K, V>) = assoc(kv.first, kv.second)

fun GameMap(corners: List<Point>, obstacles: List<Obstacle>, boosters: List<Booster>): GameMap {
    val mapPath = corners.toPath2D()
    val cells = mutableMapOf<Point, Cell>()
    val boosterCells = mutableMapOf<Point, Cell>()

    val mapBounds = mapPath.bounds

    val minX = mapBounds.minX.toInt()
    val maxX = mapBounds.maxX.toInt()
    val minY = mapBounds.minY.toInt()
    val maxY = mapBounds.maxY.toInt()

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
    for (x in -1..maxX) {
        cells[Point(x, -1)] = Cell.Superwall
        cells[Point(x, maxY)] = Cell.Superwall
    }
    for (y in -1..maxY) {
        cells[Point(-1, y)] = Cell.Superwall
        cells[Point(maxX, y)] = Cell.Superwall
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
        boosterCells[coords] = Cell(EMPTY, status)
    }

    return GameMap(
            PersistentHashMap.of(cells.entries),
            PersistentHashMap.of(boosterCells.entries),
            minX, maxX, minY, maxY)
}

fun GameMap(task: Task) = GameMap(task.map, task.obstacles, task.boosters)

data class GameMap(
        val cells: ImMap<Point, Cell>,
        val boosterCells: ImMap<Point, Cell>,
        val minX: Int,
        val maxX: Int,
        val minY: Int,
        val maxY: Int) {

    operator fun get(p: Point): Cell = cells[p] ?: Cell.Superwall

//    operator fun set(p: Point, c: Cell) {
//        cells[p] = c
//    }

    fun set(p: Point, c: Cell): GameMap {
        val oldCell = get(p)
        val newBoosterCells = if (oldCell.booster != c.booster) {
            boosterCells.without(p)
        } else {
            boosterCells
        }
        return copy(
                cells = cells + (p to c),
                boosterCells = newBoosterCells
        )
    }

    fun isVisible(from: Point, to: Point): Boolean = memo(mutableMapOf(), from to to) {
        val points = getSupercoverLine(from, to)

        for (p in points) {
            if (get(p).status.isWall) {
                return false
            }
        }

        return true
    }

    fun getSupercoverLine(from: Point, to: Point): List<Point> {
        val dx = to.v0 - from.v0
        val dy = to.v1 - from.v1

        val nx = Math.abs(dx)
        val ny = Math.abs(dy)

        val signX = sign(dx.toFloat()).toInt()
        val signY = sign(dy.toFloat()).toInt()

        var p = from

        val points = mutableListOf(p)

        var ix = 0
        var iy = 0

        while (ix < nx || iy < ny) {
            when {
                (0.5 + ix) / nx > (0.5 + iy) / ny -> {
                    // next step is vertical
                    p = p.copy(v1 = p.v1 + signY)
                    iy++
                }
                (0.5 + ix) / nx < (0.5 + iy) / ny -> {
                    // next step is horizontal
                    p = p.copy(v0 = p.v0 + signX)
                    ix++
                }
                else -> {
                    // next step is diagonal
                    p = p.copy(v0 = p.v0 + signX, v1 = p.v1 + signY)
                    ix++
                    iy++
                }
            }
            points += p
        }

        return points
    }

    fun closestFrom(start: Point, pred: (Point, Cell) -> Boolean) = sequence {
        val visited = mutableSetOf<Point>()
        val next = ArrayDeque<Point>()

        start.neighbours().forEach { next.put(it) }

        while (!next.isEmpty()) {
            val n = next.take()

            if (n in visited) continue
            if (n.v0 !in minX..maxX || n.v1 !in minY..maxY) continue

            yield(n to get(n))

            visited += n

            // TODO: DO WE NEED THIS?
            if (get(n).status == Status.SUPERWALL) continue

            n.neighbours().forEach { next.put(it) }
        }
    }.filter { pred(it.first, it.second) }

    fun enclosedArea(point: Point, area: Int) = closestFrom(point) { _, cell ->
        cell.status == Status.EMPTY
    }.take(area).count()

    fun inEnclosedArea(point: Point, threshold: Int = 5): Boolean = enclosedArea(point, threshold) < threshold

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
