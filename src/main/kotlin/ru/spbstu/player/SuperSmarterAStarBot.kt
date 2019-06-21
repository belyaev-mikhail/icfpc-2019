package ru.spbstu.player

import org.graphstream.graph.Edge
import org.graphstream.graph.Graph
import org.graphstream.graph.Node
import org.graphstream.graph.implementations.AbstractEdge
import org.graphstream.graph.implementations.DefaultGraph
import org.graphstream.graph.implementations.SingleGraph
import org.graphstream.graph.implementations.SingleNode
import org.graphstream.algorithm.Kruskal
import ru.spbstu.map.*
import ru.spbstu.sim.Simulator
import ru.spbstu.wheels.isNotEmpty
import ru.spbstu.wheels.queue
import java.awt.Color
import java.awt.Graphics
import java.awt.Graphics2D
import javax.swing.JFrame
import javax.swing.JPanel
import kotlin.random.Random

const val BLOB_SIZE = 20

data class Blob(val initial: Point, val points: Set<Point>)

data class Area(val corner: Point)

operator fun Area.contains(point: Point) =
        corner.v0 <= point.v0 &&
                corner.v1 <= point.v1 &&
                corner.v0 + BLOB_SIZE > point.v0 &&
                corner.v1 + BLOB_SIZE > point.v1

operator fun Blob.contains(point: Point) = point in points

fun Blob.isNeighbor(other: Blob): Boolean {
    for (point in points) {
        if (point.up() in other) return true
        if (point.down() in other) return true
        if (point.left() in other) return true
        if (point.right() in other) return true
    }
    return false
}


fun Blob.center(): Point = points.reduce { acc, it -> acc + it } / points.size


fun floodFill(point: Point, area: Area, map: GameMap): Blob {
    val result = mutableSetOf<Point>()

    val queue = queue<Point>()
    queue.put(point)

    while (queue.isNotEmpty()) {
        val current = queue.take()
        if (current in result) continue
        if (current !in area) continue
        if (map[current].status.isWall) continue
        result.add(current)

        queue.put(current.up())
        queue.put(current.down())
        queue.put(current.left())
        queue.put(current.right())
    }

    return Blob(point, result)
}

fun findBlobs(area: Area, map: GameMap): List<Blob> {
    val blobs = ArrayList<Blob>()
    for (v0 in area.corner.v0 until area.corner.v0 + BLOB_SIZE) {
        for (v1 in area.corner.v1 until area.corner.v1 + BLOB_SIZE) {
            val point = Point(v0, v1)
            if (map[point].status.isWall) continue
            if (blobs.any { point in it }) continue
            blobs.add(floodFill(point, area, map))
        }
    }
    return blobs
}

fun findBlobs(map: GameMap): List<Blob> {
    val blobs = arrayListOf<Blob>()
    for (y in IntProgression.fromClosedRange(map.minY, map.maxY, BLOB_SIZE)) {
        for (x in IntProgression.fromClosedRange(map.minX, map.maxX, BLOB_SIZE)) {
            val area = Area(Point(x, y))
            blobs.addAll(findBlobs(area, map))
        }
    }
    return blobs
}

fun findGraph(blobs: List<Blob>): Graph {
    val graph = SingleGraph("hui")
    repeat(blobs.size) {
        val blob = blobs[it]
        val center = blob.center()
        val node = graph.addNode<Node>("$it")
        node.addAttribute("blob", blob)
        node.addAttribute("xy", center.v0, center.v1)
    }
    for (i in blobs.indices) {
        for (j in (i + 1)..blobs.lastIndex) {
            if (!blobs[i].isNeighbor(blobs[j])) continue
            graph.addEdge<Edge>("$i,$j", "$i", "$j", false)
        }
    }
    return graph
}


fun toPanel(cellSize: Int, gameMap: GameMap, blobs: List<Blob>): JPanel = with(gameMap) {
    return object : JPanel() {
        init {
            this.preferredSize = java.awt.Dimension((maxX - minX + 2) * cellSize, (maxY - minY + 2) * cellSize)
            this.minimumSize = this.preferredSize
            this.maximumSize = this.preferredSize
        }

        override fun paint(g: Graphics?) {
            super.paint(g)
            g as Graphics2D

            g.background = java.awt.Color.BLACK

            fun drawPoint(point: Point) = kotlin.with(point) {
                g.fillRect((v0 + 1) * cellSize, (maxY - v1) * cellSize, cellSize, cellSize)
                g.paint = java.awt.Color.DARK_GRAY
                g.drawRect((v0 + 1) * cellSize, (maxY - v1) * cellSize, cellSize, cellSize)
            }

            for (blob in blobs) {
                val c = Color(Random.nextInt(256), Random.nextInt(256), Random.nextInt(256), Random.nextInt(256))
                for (p in blob.points) {
                    g.paint = c
                    drawPoint(p)
                }
            }


        }
    }
}

fun display(cellSize: Int, map: GameMap, blobs: List<Blob>): JFrame {
    val frame = JFrame()
    frame.add(toPanel(cellSize, map, blobs))
    frame.pack()
    frame.isVisible = true
    return frame

    // frame.defaultCloseOperation = WindowConstants.EXIT_ON_CLOSE
}


fun superSmarterAstarBot(sim: Simulator) =
        sequence {
            val blobs = findBlobs(sim.gameMap)
            val graph = findGraph(blobs)
            val kruskal = Kruskal()
            kruskal.init(graph)
            kruskal.compute()

            val edges = kruskal.getTreeEdges<Edge>()

            val initialBlobIdx = blobs.indexOfFirst { sim.initialRobot.pos in it }
            val rootNode = graph.getNode<Node>("$initialBlobIdx")



            display(10, sim.gameMap, blobs)

            return@sequence

            while (true) {
                val target = sim
                        .gameMap
                        .cells
                        .filter { it.value.status == Status.EMPTY }
                        .minBy {
                            sim.currentRobot.pos.euclidDistance(it.key) -
                                    0.1 * it.key.neighbours().count { sim.gameMap[it].status == Status.WRAP }
                        }

                target ?: break

                val local = astarWalk(sim, target.key)
                yieldAll(local)
            }
        }
