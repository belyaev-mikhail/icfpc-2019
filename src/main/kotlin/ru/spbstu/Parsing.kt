package ru.spbstu


enum class BoosterType {
    B, F, L, X
}

data class Map(val points: List<Point>)

data class Booster(val position: Point, val type: BoosterType)

data class Point(val x: Int, val y: Int)

data class Obstacle(val points: Map)

data class Task(val map: Map, val initial: Point, val obstacles: List<Obstacle>, val boosters: List<Booster>)

fun parsePoint(point: String): Point {
    val coordinates = point.replace('(', ' ').replace(')', ' ').split(',').map { it.trim() }.map { Integer.valueOf(it) }
    return Point(coordinates.first(), coordinates.last())
}

fun parseMap(map: String): Map {
    return Map(map.split("),(").map { parsePoint(it) })
}

fun parseObstacles(obstacles: String): List<Obstacle> {
    if(obstacles.trim().isEmpty()) return emptyList()
    return obstacles.split(';').map { parseMap(it) }.map { Obstacle(it) }
}


fun parseBoosters(boosters: String): List<Booster> {
    if(boosters.trim().isEmpty()) return emptyList()
    return boosters.split(';').map { it.trim() }.map { Booster(parsePoint(it.drop(1)), BoosterType.valueOf("${it[0]}")) }
}

fun parseFile(data: String): Task {
    val (map, point, obstacles, boosters) = data.split('#')
    return Task(
            parseMap(map),
            parsePoint(point),
            parseObstacles(obstacles),
            parseBoosters(boosters)
    )
}
