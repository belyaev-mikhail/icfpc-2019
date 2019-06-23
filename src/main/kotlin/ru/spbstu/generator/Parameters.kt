package ru.spbstu.generator

import ru.spbstu.map.Point

data class Parameters(
        val blockNum: Int,
        val epochNum: Int,
        val mapSize: Int,
        val verticesMin: Int,
        val verticesMax: Int,
        val manipulatorsNumber: Int,
        val fastWheelsNumber: Int,
        val drillsNumber: Int,
        val teleportsNumber: Int,
        val cloningBoostersNumber: Int,
        val spawnPointsNumber: Int,
        val pathsPoints: Set<Point>,
        val wallsPoints: Set<Point>) {

    companion object {
        fun read(text: String): Parameters {
            val (numbers, paths, walls) = text.split("#")
            val intNumbers = numbers.split(",").map { it.toInt() }
            val pathsPoints = paths.removeSurrounding("(", ")").split("),(")
                    .map { it.split(",").map { it.toInt() } }
                    .map { (v0, v1) -> Point(v0, v1) }
            val wallsPoints = walls.removeSurrounding("(", ")").split("),(")
                    .map { it.split(",").map { it.toInt() } }
                    .map { (v0, v1) -> Point(v0, v1) }
            return Parameters(
                    blockNum = intNumbers[0],
                    epochNum = intNumbers[1],
                    mapSize = intNumbers[2],
                    verticesMin = intNumbers[3],
                    verticesMax = intNumbers[4],
                    manipulatorsNumber = intNumbers[5],
                    fastWheelsNumber = intNumbers[6],
                    drillsNumber = intNumbers[7],
                    teleportsNumber = intNumbers[8],
                    cloningBoostersNumber = intNumbers[9],
                    spawnPointsNumber = intNumbers[10],
                    pathsPoints = pathsPoints.toSet(),
                    wallsPoints = wallsPoints.toSet()
            )
        }
    }
}