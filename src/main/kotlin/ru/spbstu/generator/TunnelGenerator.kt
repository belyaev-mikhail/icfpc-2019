package ru.spbstu.generator

import ru.spbstu.map.*
import ru.spbstu.player.aStarSearch


class TunnelGenerator(private val parameters: Parameters) {
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

    private fun appendCorners(corners: Int) {
        println(corners)
    }

    private fun removeCorners(corners: Int) {

    }

    fun generate(): HashMap<Point, Cell> {
        parameters.pathsPoints.forEach { matrix[it] = Cell.PATH }
        for (i in -1..parameters.mapSize) {
            matrix[Point(i, 0)] = Cell.WALL
            matrix[Point(0, i)] = Cell.WALL
            matrix[Point(i, parameters.mapSize)] = Cell.WALL
            matrix[Point(parameters.mapSize, i)] = Cell.WALL
        }
        parameters.wallsPoints.forEach {
            generateWalls(it)
        }
        val corners = countCorners()
        if (corners > parameters.verticesMax) {
            removeCorners(corners - parameters.verticesMax)
        } else if (corners < parameters.verticesMin) {
            appendCorners(parameters.verticesMin - corners)
        }
        println(parameters.verticesMin to parameters.verticesMax to countCorners())
        return matrix
    }

    private fun countCorners() = matrix.filter { it.value == Cell.WALL }.map { it.key }.count { isCorner(it) }

    private fun isOuterCorner(point: Point) = isWall(point) &&
            (!isWall(point.up()) && !isWall(point.left()) && !isWall(point.up().left()) ||
                    !isWall(point.up()) && !isWall(point.right()) && !isWall(point.up().right()) ||
                    !isWall(point.down()) && !isWall(point.left()) && !isWall(point.down().left()) ||
                    !isWall(point.down()) && !isWall(point.right()) && !isWall(point.down().right()))

    private fun isInnerCorner(point: Point) = isWall(point) &&
            (isWall(point.up()) && isWall(point.left()) && !isWall(point.up().left()) ||
                    isWall(point.up()) && isWall(point.right()) && !isWall(point.up().right()) ||
                    isWall(point.down()) && isWall(point.left()) && !isWall(point.down().left()) ||
                    isWall(point.down()) && isWall(point.right()) && !isWall(point.down().right()))

    private fun isCorner(point: Point) = isOuterCorner(point) || isInnerCorner(point)
}