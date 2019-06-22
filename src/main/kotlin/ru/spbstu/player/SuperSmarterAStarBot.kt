package ru.spbstu.player

import org.graphstream.algorithm.Kruskal
import org.graphstream.graph.DepthFirstIterator
import org.graphstream.graph.Edge
import org.graphstream.graph.Graph
import org.graphstream.graph.Node
import org.graphstream.graph.implementations.SingleGraph
import org.jgrapht.alg.tour.ChristofidesThreeHalvesApproxMetricTSP
import ru.spbstu.map.*
import ru.spbstu.sim.Command
import ru.spbstu.sim.Simulator
import ru.spbstu.wheels.MutableRef
import ru.spbstu.wheels.getValue
import ru.spbstu.wheels.isNotEmpty
import ru.spbstu.wheels.queue
import java.awt.Color
import java.awt.Graphics
import java.awt.Graphics2D
import javax.swing.JFrame
import javax.swing.JPanel
import kotlin.random.Random
import kotlin.reflect.KCallable
import kotlin.reflect.KFunction

typealias BotType = (MutableRef<Simulator>, Set<Point>, Int) -> Sequence<Pair<Int, Command>>

object SuperSmarterAStarBot: BotType {
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
                graph.addEdge<Edge>("$i,$j", "$i", "$j", false).addAttribute("weight", 1)
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
    }

    fun optimizeBlobs(blobs: List<Blob>): List<Blob> {
        val maxSize = BLOB_SIZE * BLOB_SIZE
        val result = blobs.toMutableList()
        while (true) {
            val candidate = result.find { it.points.size < 0.2 * maxSize }
                    ?: break
            val mergeWith = result.find { it != candidate && it.isNeighbor(candidate) }
                    ?: break
            result.remove(candidate)
            result.remove(mergeWith)
            result.add(Blob(mergeWith.initial, mergeWith.points + candidate.points))
        }
        return result
    }

    fun getBlobsOrdered(initial: Node, allNodes: List<Node>, edges: List<Edge>): List<Blob> {
        val graph = SingleGraph("minimal")
        val blobs = allNodes.map { it.id to it.getAttribute<Blob>("blob") }.toMap()
        blobs.keys.forEach {
            val center = blobs[it]!!.center()
            graph.addNode<Node>(it).addAttribute("xy", center.v0, center.v1)
        }
        edges.forEach {
            val from = it.getSourceNode<Node>()
            val to = it.getTargetNode<Node>()
            graph.addEdge<Edge>("${from.id},${to.id}", from.id, to.id, false)
        }
        val nodes = arrayListOf<Node>()
        val iter = DepthFirstIterator<Node>(graph.getNode(initial.id))
        iter.forEach { nodes.add(it) }
        return nodes.map { blobs[it.id]!! }
    }

    override fun invoke(simref: MutableRef<Simulator>, points: Set<Point>, idx: Int): Sequence<Pair<Int, Command>> {
        return sequence {
            val sim by simref

            val currentRobot = { sim.currentRobots[idx] }

            val initialBlobs = findBlobs(sim.gameMap)
            val blobs = optimizeBlobs(initialBlobs)
            val graph = findGraph(blobs)

            var chris = Christofides.path(graph)
            if(chris.size == 1) { // Don't ask
                chris = chris + chris
            }
//            val kruskal = Kruskal()
//            kruskal.init(graph)
//            kruskal.compute()
//
            val initialBlobIdx = blobs.indexOfFirst { currentRobot().pos in it }
            val rootNode = graph.getNode<Node>("$initialBlobIdx")
//
//            val orderedBlobs = getBlobsOrdered(rootNode, graph.getNodeSet<Node>().toList(), kruskal.getTreeEdges<Edge>().toList())

            val rootIndex = chris.indexOf(rootNode)
            val orderedBlobs = (chris.subList(rootIndex, chris.lastIndex) + chris.subList(0, rootIndex)).map {
                it.getAttribute<Blob>("blob")
            }
            println(orderedBlobs)

            for (blob in orderedBlobs) {
                yieldAll(evenSmarterPriorityAstarBot(simref, blob.points, idx))
            }
        }
    }

    val name: String = "SuperSmarterAStarBot"
    override fun toString(): String = name
}
