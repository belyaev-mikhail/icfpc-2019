package ru.spbstu.generator

import ru.spbstu.map.Point


class BusterGenerator(val walls: Set<Point>, val parameters: Parameters) {

    data class Busters(val robot: Point,
                       val manipulators: Set<Point>,
                       val fastWheels: Set<Point>,
                       val drills: Set<Point>,
                       val teleports: Set<Point>,
                       val cloningBoosters: Set<Point>,
                       val spawnPoints: Set<Point>)

    private val bustersLocations = HashSet<Point>()

    private fun generatePoints(number: Int): Set<Point> {
        val result = HashSet<Point>()
        for (v0 in 0 until parameters.mapSize) {
            for (v1 in 0 until parameters.mapSize) {
                val point = Point(v0, v1)
                if (walls.contains(point)) continue
                if (bustersLocations.contains(point)) continue
                bustersLocations.add(point)
                result.add(point)
                if (result.size == number) return result
            }
        }
        println("AAA")
        return result
    }

    fun generate() = Busters(
            manipulators = generatePoints(parameters.manipulatorsNumber),
            fastWheels = generatePoints(parameters.fastWheelsNumber),
            drills = generatePoints(parameters.drillsNumber),
            teleports = generatePoints(parameters.teleportsNumber),
            cloningBoosters = generatePoints(parameters.cloningBoostersNumber),
            spawnPoints = generatePoints(parameters.spawnPointsNumber),
            robot = generatePoints(1).first()
    )
}