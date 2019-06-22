package ru.spbstu.player

import org.graphstream.algorithm.Kruskal
import org.graphstream.graph.DepthFirstIterator
import org.graphstream.graph.Edge
import org.graphstream.graph.Graph
import org.graphstream.graph.Node
import org.graphstream.graph.implementations.SingleGraph
import ru.spbstu.ktuples.compareTo
import ru.spbstu.ktuples.sorted
import ru.spbstu.map.*
import ru.spbstu.player.evenSmarterPriorityAstarBot
import ru.spbstu.sim.*
import ru.spbstu.util.withIdx
import ru.spbstu.wheels.*
import java.awt.Color
import java.awt.Graphics
import java.awt.Graphics2D
import java.util.*
import javax.swing.JFrame
import javax.swing.JPanel
import kotlin.math.abs
import kotlin.random.Random

object SmartAsFuckBot {
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

    fun toPanel(cellSize: Int, gameMap: GameMap, blobs: List<Blob>, robotCoor: Point): JPanel = with(gameMap) {
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

                g.paint = Color.RED
                drawPoint(robotCoor)
            }
        }
    }

    fun display(cellSize: Int, map: GameMap, blobs: List<Blob>, robotCoor: Point): JFrame {
        val frame = JFrame()
        frame.add(toPanel(cellSize, map, blobs, robotCoor))
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

    fun run(simref: MutableRef<Simulator>, points: Set<Point>, idx: Int = 0): Sequence<Pair<Int, Command>> {
        return sequence {
            val sim by simref

            val currentRobot = { sim.currentRobots[idx] }

            val initialBlobs = findBlobs(sim.gameMap)
            val blobs = optimizeBlobs(initialBlobs)
            display(10, simref.value.gameMap, blobs, simref.value.currentRobots[0].pos)
            val graph = findGraph(blobs)
            val kruskal = Kruskal()
            kruskal.init(graph)
            kruskal.compute()

            val initialBlobIdx = blobs.indexOfFirst { currentRobot().pos in it }
            val rootNode = graph.getNode<Node>("$initialBlobIdx")

            val orderedBlobs = getBlobsOrdered(rootNode, graph.getNodeSet<Node>().toList(), kruskal.getTreeEdges<Edge>().toList())

            for (blob in orderedBlobs) {
                println("BLOB")
                yieldAll(zipAstarBot(simref, blob.points, idx))
            }
        }
    }


    fun Point.getNearestUpNeighbor(points: Set<Point>): Point? =
            points.map { it to it.v1 - this.v1 }
                    .filter { it.second > 0 }
                    .sortedBy { it.second }
                    .map { it.first }
                    .minBy { abs(it.v0 - this.v0) }


    var k = 0
    private fun zipAstarBot(simref: MutableRef<Simulator>, points: Set<Point>, idx: Int = 0) =
            sequence {
                val pointQueue = TreeSet<Point> { o1, o2 -> if (o1.v0.compareTo(o2.v0) == 0) o1.v1.compareTo(o2.v1) else o1.v0.compareTo(o2.v0) }
                while (true) {
                    val sim by simref

                    //yieldAll(applyBoosters(sim, idx))

                    val currentRobot = { sim.currentRobots[idx] }

                    val cells = sim.gameMap.cells.filter { it.key in points }
                    val obstacles = sim.gameMap.cells.filter { it.value.status == Status.WALL || it.value.status == Status.SUPERWALL }


//                    val closestBooster = checkNearestBooster(sim, currentRobot()) {
//                        it.booster == BoosterType.MANIPULATOR_EXTENSION
//                    }

//                    if (closestBooster != null) {
//                        val local = astarWithoutTurnsWalk(sim, closestBooster, idx)
//                        yieldAll(local)
//                    }

                    //yieldAll(applyBoosters(sim, idx))

//                    val target = cells
//                            .filter { it.value.status == Status.EMPTY }
//                            .minBy {
//                                currentRobot().pos.euclidDistance(it.key)
//                            }
                    val allFreeCells =
                            cells.filter { it.value.status == Status.EMPTY }
                                    .map { it.key }
                    val curCoor = simref.value.currentRobots[0].pos


//                    val freeAndWrapCellsFromSameRow =
//                            cells.filter { it.value.status == Status.EMPTY || it.value.status == Status.WRAP }
//                                    .map { it.key }
//                                    .filter { it.v1 == curCoor.v1 }

                    val freeCellsFromSameRow =
                            allFreeCells
                                    .filter { it.v1 == curCoor.v1 }
                                    .sortedBy { it.v0 }

                    val obstaclesFromRow =
                            obstacles.filter { it.value.status == Status.WALL || it.value.status == Status.SUPERWALL }
                                    .map { it.key }
                                    .filter { it.v1 == curCoor.v1 }
                                    .sortedBy { it.v0 }


                    //Update pointQueue
                    println("BEFORE = ${pointQueue}")
                    pointQueue.removeAll { it !in allFreeCells }
                    println("AFTER = ${pointQueue}")

                    //Check for availability of nodes from queue
                    if (pointQueue.isNotEmpty()) {
                        val nodeToGo = pointQueue.first()
                        println("FINDING ROUTE TO ${nodeToGo} FROM ${sim.currentRobots[idx].pos}")
                        if (nodeToGo.v1 < sim.currentRobots[idx].pos.v1) {
                            val route = astarWithoutTurnsAndUpsWalk(sim, nodeToGo, idx)
                            println("ROUTE = $route")
                            if (route.isNotEmpty()) {
                                yieldAll(route)
                                continue
                            }
                        }
                    }

//                    val nodeToGo =
//                            pointQueue.filter { it.v0 == pointQueue.first().v0 }
//                                    .find { it1 -> freeAndWrapCellsFromSameRow.any { it.v0 == it1.v0 } }
//                    println("node = $nodeToGo")


                    println("WALLS = ${cells.filter { it.value.status == Status.WALL || it.value.status == Status.SUPERWALL }.toList()}")
                    println("Free cells = ${freeCellsFromSameRow.toList()}")
                    println("curRobot coor = ${simref.value.currentRobots[0].pos}")
                    println("obstaclesFromRow = ${obstaclesFromRow.toList()}")
                    val firstObstacleFromLeft = obstaclesFromRow.findLast { it.v0 < curCoor.v0 }?.v0 ?: Int.MIN_VALUE
                    val firstObstacleFromRight = obstaclesFromRow.find { it.v0 > curCoor.v0 }?.v0 ?: Int.MAX_VALUE
                    println("OBS1 = $firstObstacleFromLeft")
                    println("OBS2 = $firstObstacleFromRight")
                    val avialibleCells = freeCellsFromSameRow
                            .filter { it.v0 in (firstObstacleFromLeft + 1)..(firstObstacleFromRight - 1) }
                            .sortedBy { it.v0 }
                    val freeCells = freeCellsFromSameRow.filter { it !in avialibleCells }
                    freeCells.forEach { pointQueue.add(it); println("ADDING $it to queue") }
                    println("AV = ${avialibleCells.toList()}")
                    println("freeCells = ${freeCells.toList()}")
                    var fullRow = (avialibleCells + curCoor).sortedBy { it.v0 }
                    var indexOfRobot = fullRow.indexOf(curCoor)
                    println("IND = $indexOfRobot")
                    println("FULL ROW = ${fullRow.toList()}")
                    val commands = mutableListOf<Command>()
                    var newCoord = curCoor
                    //Go to left
                    fullRow.filterIndexed { index, _ -> index < indexOfRobot }.find { it.v0 < curCoor.v0 }?.let {
                        repeat(curCoor.v0 - it.v0) { commands.add(MOVE_LEFT); newCoord = Point(newCoord.v0 - 1, newCoord.v1) }
                    }
                    println("newCoord = $newCoord")
                    fullRow = (avialibleCells + newCoord).sortedBy { it.v0 }
                    indexOfRobot = fullRow.indexOf(newCoord)
                    println("index = $indexOfRobot")
                    //Go to right
                    fullRow.filterIndexed { index, _ -> index > indexOfRobot }.findLast { it.v0 > curCoor.v0 }?.let {
                        repeat(it.v0 - newCoord.v0) { commands.add(MOVE_RIGHT); newCoord = Point(newCoord.v0 + 1, newCoord.v1) }
                    }
                    println("END COOR = ${newCoord}")
                    println("FINDING Neighbor FROM ${Point(newCoord.v0, newCoord.v1)} in ${allFreeCells.toSet()}")
                    val n = Point(newCoord.v0, newCoord.v1).getNearestUpNeighbor(allFreeCells.toSet()) ?: break
                    println("NEIGHBOR = $n")
                    yieldAll(commands)
                    commands.clear()

                    //GO TO NEAREST Neighbor
                    val pathToNeigh = astarWithoutTurnsWalk(sim, n, idx)
                    yieldAll(pathToNeigh)
                    ++k

                    println("CUR QUEUE = ${pointQueue}")
//                    if (k == 20)
//                        System.exit(0)
                    println("\n\n")
                    //println("allFreeCells = ${allFreeCells.toList()}")
//                    println("target = $target")
//                    System.exit(0)
//
//                    target ?: break
//
//                    val local = visibleAstarWalk(sim, target.key, idx)
//                    yieldAll(local)
                }
            }.withIdx(idx)
}