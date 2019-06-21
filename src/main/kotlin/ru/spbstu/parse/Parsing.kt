package ru.spbstu.parse

import ru.spbstu.map.*

data class Task(val name: String, val map: Shape, val initial: Point, val obstacles: List<Shape>, val boosters: List<Booster>)

fun parsePoint(point: String): Point {
    val coordinates = point.replace('(', ' ').replace(')', ' ').split(',').map { it.trim() }.map { Integer.valueOf(it) }
    return Point(coordinates.first(), coordinates.last())
}

fun parseMap(map: String): Shape {
    return map.split("),(").map { parsePoint(it) }
}

fun parseObstacles(obstacles: String): List<Obstacle> {
    if (obstacles.trim().isEmpty()) return emptyList()
    return obstacles.split(';').map { parseMap(it) }.map { it }
}

fun parseBoosters(boosters: String): List<Booster> {
    if (boosters.trim().isEmpty()) return emptyList()
    return boosters.split(';').map { it.trim() }.map { Booster(parsePoint(it.drop(1)), BoosterType.from("${it[0]}")) }
}

fun parseFile(name: String, data: String): Task {
    val (map, point, obstacles, boosters) = data.split('#')
    return Task(
            name,
            parseMap(map),
            parsePoint(point),
            parseObstacles(obstacles),
            parseBoosters(boosters)
    )
}
