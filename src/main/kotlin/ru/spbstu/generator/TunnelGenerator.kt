package ru.spbstu.generator

import ru.spbstu.map.*
import ru.spbstu.player.aStarSearch


class  TunnelGenerator(private val parameters: Parameters) {
    enum class Cell { WALL, PATH }

    private val matrix = HashMap<Point, Cell>()

    private fun isWall(point: Point) = matrix[point] == Cell.WALL
    private fun isPath(point: Point) = matrix[point] == Cell.PATH

    private fun falseWall(point: Point) =
            (isWall(point.up().left()) && !isWall(point.up()) && !isWall(point.left())) ||
                    (isWall(point.up().right()) && !isWall(point.up()) && !isWall(point.right())) ||
                    (isWall(point.down().left()) && !isWall(point.down()) && !isWall(point.left())) ||
                    (isWall(point.down().right()) && !isWall(point.down()) && !isWall(point.right()))

    private fun generateWalls(point: Point) {
        val walls = matrix.asSequence()
                .filter { it.value == Cell.WALL }
                .filter { it.key != point }
                .map { it.key }
                .toSet()
        val points = aStarSearch(
                from = point,
                heur = { walls.map { wall -> wall.euclidDistance(it) }.min()!! },
                goal = { it in walls },
                neighbours = {
                    it.neighbours().asSequence().filterNot { isPath(it) || falseWall(it) }
                })!!
        points.forEach { matrix[it] = Cell.WALL }
    }

    private fun addWallIfCan(point: Point): Boolean {
        if (point.v0 < 0 || point.v1 < 0) return false
        if (point.v0 >= parameters.mapSize) return false
        if (point.v1 >= parameters.mapSize) return false
        if (isWall(point)) return false
        if (falseWall(point)) return false
        matrix[point] = Cell.WALL
        return true
    }

    data class Pattern(val walls: Set<Point>, val paths: Set<Point>) {
        companion object {
            // "000 000 0x1"
            fun parse(initial: Point, pattern: String): Pattern {
                val lines = pattern.split(" ")
                val length = lines.map { it.length }.max()!!
                if (length != lines.map { it.length }.min()) throw Exception(pattern)
                val walls = HashSet<Point>()
                val paths = HashSet<Point>()
                var relativeInitial: Point? = null
                for (v0 in lines.indices) {
                    for (v1 in 0 until length) {
                        val ch = lines[v0][v1]
                        val point = Point(v0, v1)
                        when (ch) {
                            '0' -> paths.add(point)
                            '1' -> walls.add(point)
                            'o' -> paths.add(point)
                            'x' -> walls.add(point)
                            '-' -> { //Skip
                            }
                            else -> throw Exception(pattern)
                        }
                        if (ch == 'o' || ch == 'x') {
                            relativeInitial = point
                        }
                    }
                }
                if (relativeInitial == null) throw Exception(pattern)
                return Pattern(
                        walls = walls.map { it - relativeInitial + initial }.toSet(),
                        paths = paths.map { it - relativeInitial + initial }.toSet()
                )
            }
        }
    }

    private fun availablePoint(point: Point): Boolean {
        if (point.v0 < -1 || point.v1 < -1) return false
        if (point.v0 > parameters.mapSize) return false
        if (point.v1 > parameters.mapSize) return false
        return true
    }

    private fun match(point: Point, rawPattern: String): Boolean {
        val pattern = Pattern.parse(point, rawPattern)
        if (pattern.paths.any { !availablePoint(it) }) return false
        if (pattern.walls.any { !availablePoint(it) }) return false
        if (pattern.paths.any { isWall(it) }) return false
        if (pattern.walls.any { !isWall(it) }) return false
        return true
    }

    private fun match(point: Point, vararg rawPattern: String): Int {
        return rawPattern.count { match(point, it) }
    }

    private fun appendCorners() {
        var availableCorners = parameters.verticesMin - countCorners()
        var pointsIt = emptyList<Point>().iterator()
        var previousAvailableCorners = availableCorners + 1
        while (availableCorners > 0) {
            if (!pointsIt.hasNext()) {
                if (previousAvailableCorners == availableCorners) {
                    println("Has not any possibilities to append, $availableCorners")
                    return
                }
                previousAvailableCorners = availableCorners
                pointsIt = matrix.filter { it.value == Cell.WALL }.map { it.key }.iterator()
            }
            val point = pointsIt.next()
            if (match(point, "000 000 1x1")) {
                if (addWallIfCan(point.down())) {
                    availableCorners -= 4
                }
            } else if (match(point, "1x1 000 000")) {
                if (addWallIfCan(point.up())) {
                    availableCorners -= 4
                }
            } else if (match(point, "100 x00 100")) {
                if (addWallIfCan(point.right())) {
                    availableCorners -= 4
                }
            } else if (match(point, "001 00x 001")) {
                if (addWallIfCan(point.left())) {
                    availableCorners -= 4
                }
            } else if (match(point, "000 000 0x1")) {
                if (addWallIfCan(point.down())) {
                    availableCorners -= 2
                }
            } else if (match(point, "000 000 1x0")) {
                if (addWallIfCan(point.down())) {
                    availableCorners -= 2
                }
            } else if (match(point, "1x0 000 000")) {
                if (addWallIfCan(point.up())) {
                    availableCorners -= 2
                }
            } else if (match(point, "0x1 000 000")) {
                if (addWallIfCan(point.up())) {
                    availableCorners -= 2
                }
            } else if (match(point, "001 00x 000")) {
                if (addWallIfCan(point.left())) {
                    availableCorners -= 2
                }
            } else if (match(point, "000 00x 001")) {
                if (addWallIfCan(point.left())) {
                    availableCorners -= 2
                }
            } else if (match(point, "100 x00 000")) {
                if (addWallIfCan(point.right())) {
                    availableCorners -= 2
                }
            } else if (match(point, "000 x00 100")) {
                if (addWallIfCan(point.right())) {
                    availableCorners -= 2
                }
            }
//            if (availableCorners + countCorners() != parameters.verticesMin) {
//                println("AAAAAAAA")
//                println(point)
//                println("$availableCorners + ${countCorners()} != ${parameters.verticesMin}")
//                throw Exception()
//            }
        }
    }

    private fun removeCorners() {

    }

    fun generate(): Set<Point> {
        parameters.pathsPoints.forEach { matrix[it] = Cell.PATH }
        for (i in -1..parameters.mapSize) {
            matrix[Point(i, -1)] = Cell.WALL
            matrix[Point(-1, i)] = Cell.WALL
            matrix[Point(i, parameters.mapSize)] = Cell.WALL
            matrix[Point(parameters.mapSize, i)] = Cell.WALL
        }
        parameters.wallsPoints.forEach {
            generateWalls(it)
        }
        appendCorners()
        removeCorners()
        println("${countCorners()} in ${parameters.verticesMin to parameters.verticesMax}")
        return matrix.filter { it.value == Cell.WALL }.keys
    }

    private fun countCorners() = matrix.asSequence()
            .filter { it.value == Cell.WALL }
            .map { it.key }
            .map { match(it, "00 0x", "00 x0", "x0 00", "0x 00", "01 1x", "10 x1", "x1 10", "1x 01") }
            .sum() - 4
}