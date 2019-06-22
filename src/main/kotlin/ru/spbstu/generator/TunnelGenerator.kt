package ru.spbstu.generator

import ru.spbstu.generator.TunnelGenerator.FragmentType.*
import ru.spbstu.map.*
import ru.spbstu.player.aStarSearch


class  TunnelGenerator(private val parameters: Parameters) {
    enum class Cell { WALL, PATH }

    enum class FragmentType {
        INNER_CORNER_UL, INNER_CORNER_UR, INNER_CORNER_DL, INNER_CORNER_DR,
        OUTER_CORNER_UL, OUTER_CORNER_UR, OUTER_CORNER_DL, OUTER_CORNER_DR,
        LONG_CORNER_000000011, LONG_CORNER_000000110, LONG_CORNER_011000000, LONG_CORNER_110000000,
        LONG_CORNER_001001000, LONG_CORNER_000001001, LONG_CORNER_100100000, LONG_CORNER_000100100,
        PLACE_U, PLACE_L, PLACE_D, PLACE_R
    }

    private val CORNERS = setOf(INNER_CORNER_UL, INNER_CORNER_UR, INNER_CORNER_DL, INNER_CORNER_DR,
            OUTER_CORNER_UL, OUTER_CORNER_UR, OUTER_CORNER_DL, OUTER_CORNER_DR)

    private fun getFragmentTypes(point: Point): Set<FragmentType> {
        return sequence {
            if (isWall(point) && !isWall(point.up()) && !isWall(point.left()) && !isWall(point.up().left()))
                yield(OUTER_CORNER_UL)
            if (isWall(point) && !isWall(point.up()) && !isWall(point.right()) && !isWall(point.up().right()))
                yield(OUTER_CORNER_UR)
            if (isWall(point) && !isWall(point.down()) && !isWall(point.left()) && !isWall(point.down().left()))
                yield(OUTER_CORNER_DL)
            if (isWall(point) && !isWall(point.down()) && !isWall(point.right()) && !isWall(point.down().right()))
                yield(OUTER_CORNER_DR)
            if (isWall(point) && isWall(point.up()) && isWall(point.left()) && !isWall(point.up().left()))
                yield(INNER_CORNER_UL)
            if (isWall(point) && isWall(point.up()) && isWall(point.right()) && !isWall(point.up().right()))
                yield(INNER_CORNER_UR)
            if (isWall(point) && isWall(point.down()) && isWall(point.left()) && !isWall(point.down().left()))
                yield(INNER_CORNER_DL)
            if (isWall(point) && isWall(point.down()) && isWall(point.right()) && !isWall(point.down().right()))
                yield(INNER_CORNER_DR)
            if (isWall(point) && isWall(point.left()) && isWall(point.right()) &&
                    !isWall(point.up()) && !isWall(point.up().left()) && !isWall(point.up().right()) &&
                    !isWall(point.up().up()) && !isWall(point.up().up().left()) && !isWall(point.up().up().right()))
                yield(PLACE_U)
            if (isWall(point) && isWall(point.left()) && isWall(point.right()) &&
                    !isWall(point.down()) && !isWall(point.down().left()) && !isWall(point.down().right()) &&
                    !isWall(point.down().down()) && !isWall(point.down().down().left()) && !isWall(point.down().down().right()))
                yield(PLACE_D)
            if (isWall(point) && isWall(point.up()) && isWall(point.down()) &&
                    !isWall(point.left()) && !isWall(point.left().up()) && !isWall(point.left().down()) &&
                    !isWall(point.left().left()) && !isWall(point.left().left().up()) && !isWall(point.left().left().down()))
                yield(PLACE_L)
            if (isWall(point) && isWall(point.up()) && isWall(point.down()) &&
                    !isWall(point.right()) && !isWall(point.right().up()) && !isWall(point.right().down()) &&
                    !isWall(point.right().right()) && !isWall(point.right().right().up()) && !isWall(point.right().right().down()))
                yield(PLACE_R)

            if (isWall(point) && !isWall(point.left()) && isWall(point.right()) &&
                    !isWall(point.up()) && !isWall(point.up().left()) && !isWall(point.up().right()) &&
                    !isWall(point.up().up()) && !isWall(point.up().up().left()) && !isWall(point.up().up().right()))
                yield(LONG_CORNER_000000011)
            if (isWall(point) && isWall(point.left()) && !isWall(point.right()) &&
                    !isWall(point.up()) && !isWall(point.up().left()) && !isWall(point.up().right()) &&
                    !isWall(point.up().up()) && !isWall(point.up().up().left()) && !isWall(point.up().up().right()))
                yield(LONG_CORNER_000000110)
            if (isWall(point) && !isWall(point.left()) && isWall(point.right()) &&
                    !isWall(point.down()) && !isWall(point.down().left()) && !isWall(point.down().right()) &&
                    !isWall(point.down().down()) && !isWall(point.down().down().left()) && !isWall(point.down().down().right()))
                yield(LONG_CORNER_011000000)
            if (isWall(point) && isWall(point.left()) && !isWall(point.right()) &&
                    !isWall(point.down()) && !isWall(point.down().left()) && !isWall(point.down().right()) &&
                    !isWall(point.down().down()) && !isWall(point.down().down().left()) && !isWall(point.down().down().right()))
                yield(LONG_CORNER_110000000)

            if (isWall(point) && isWall(point.up()) && !isWall(point.down()) &&
                    !isWall(point.left()) && !isWall(point.left().up()) && !isWall(point.left().down()) &&
                    !isWall(point.left().left()) && !isWall(point.left().left().up()) && !isWall(point.left().down()))
                yield(LONG_CORNER_001001000)
            if (isWall(point) && !isWall(point.up()) && isWall(point.down()) &&
                    !isWall(point.left()) && !isWall(point.left().up()) && !isWall(point.left().down()) &&
                    !isWall(point.left().left()) && !isWall(point.left().left().up()) && !isWall(point.left().down()))
                yield(LONG_CORNER_000001001)
            if (isWall(point) && isWall(point.up()) && !isWall(point.down()) &&
                    !isWall(point.right()) && !isWall(point.right().up()) && !isWall(point.right().down()) &&
                    !isWall(point.right().right()) && !isWall(point.right().right().up()) && !isWall(point.right().down()))
                yield(LONG_CORNER_100100000)
            if (isWall(point) && !isWall(point.up()) && isWall(point.down()) &&
                    !isWall(point.right()) && !isWall(point.right().up()) && !isWall(point.right().down()) &&
                    !isWall(point.right().right()) && !isWall(point.right().right().up()) && !isWall(point.right().down()))
                yield(LONG_CORNER_000100100)
        }.toSet()
    }

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

    private fun appendCorners() {
        var availableCorners = parameters.verticesMin - countCorners() + 4
        var pointsIt = emptyList<Point>().iterator()
        var previousAvailableCorners = availableCorners + 1
        while (availableCorners >= 0) {
            if (!pointsIt.hasNext()) {
                if (previousAvailableCorners == availableCorners) {
                    println("Has not any possibilities to append, $availableCorners")
                    return
                }
                previousAvailableCorners = availableCorners
                pointsIt = matrix.filter { it.value == Cell.WALL }.map { it.key }.iterator()
            }
            val point = pointsIt.next()
            val fragmentsType = getFragmentTypes(point)
            if (PLACE_U in fragmentsType) {
                if (addWallIfCan(point.up())) {
                    availableCorners -= 4
                }
            } else if (PLACE_D in fragmentsType) {
                if (addWallIfCan(point.down())) {
                    availableCorners -= 4
                }
            } else if (PLACE_L in fragmentsType) {
                if (addWallIfCan(point.left())) {
                    availableCorners -= 4
                }
            } else if (PLACE_R in fragmentsType) {
                if (addWallIfCan(point.right())) {
                    availableCorners -= 4
                }
            } else if (LONG_CORNER_000000011 in fragmentsType) {
                if (addWallIfCan(point.up())) {
                    availableCorners -= 2
                }
            } else if (LONG_CORNER_000000110 in fragmentsType) {
                if (addWallIfCan(point.up())) {
                    availableCorners -= 2
                }
            } else if (LONG_CORNER_110000000 in fragmentsType) {
                if (addWallIfCan(point.down())) {
                    availableCorners -= 2
                }
            } else if (LONG_CORNER_011000000 in fragmentsType) {
                if (addWallIfCan(point.down())) {
                    availableCorners -= 2
                }
            } else if (LONG_CORNER_001001000 in fragmentsType) {
                if (addWallIfCan(point.left())) {
                    availableCorners -= 2
                }
            } else if (LONG_CORNER_000001001 in fragmentsType) {
                if (addWallIfCan(point.left())) {
                    availableCorners -= 2
                }
            } else if (LONG_CORNER_100100000 in fragmentsType) {
                if (addWallIfCan(point.right())) {
                    availableCorners -= 2
                }
            } else if (LONG_CORNER_000100100 in fragmentsType) {
                if (addWallIfCan(point.right())) {
                    availableCorners -= 2
                }
            }
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
            .map { getFragmentTypes(it) }
            .map { it.count { type -> type in CORNERS } }
            .sum()
}