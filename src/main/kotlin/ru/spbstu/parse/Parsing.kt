package ru.spbstu.parse

import ru.spbstu.map.*
import ru.spbstu.sim.*

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

fun parseAnswer(data: String): List<Command> {
    val splData = data.split(Regex("[() ]"))
    val result = mutableListOf<Command>()
    splData.mapIndexed { index, s ->
        s.map {
            when (it) {
                'R' -> RESET
                'W' -> MOVE_UP
                'S' -> MOVE_DOWN
                'A' -> MOVE_LEFT
                'D' -> MOVE_RIGHT
                'Z' -> NOOP
                'E' -> TURN_CW
                'Q' -> TURN_CCW
                'B' -> ATTACH_MANUPULATOR(parsePoint(splData[index + 1]))
                'F' -> USE_FAST_WHEELS
                'L' -> USE_DRILL
                'T' -> SHIFT_TO(parsePoint(splData[index + 1]))
                else -> null
            }
        }.filterNotNull().forEach { result.add(it) }
    }
    return result
}

