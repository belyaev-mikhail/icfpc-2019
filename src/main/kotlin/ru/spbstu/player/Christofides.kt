package ru.spbstu.player

import org.graphstream.algorithm.Kruskal
import org.graphstream.graph.Edge
import org.graphstream.graph.Graph
import org.graphstream.graph.Node
import org.graphstream.graph.implementations.SingleGraph
import org.jgrapht.alg.tour.ChristofidesThreeHalvesApproxMetricTSP
import org.jgrapht.graph.DefaultUndirectedWeightedGraph
import org.jgrapht.graph.SimpleGraph
import ru.spbstu.map.sqr
import kotlin.math.sqrt

fun Node.distance(that: Node): Double {
    val (x, y) = getArray("xy")
    x as Number
    y as Number
    val (tx, ty) = that.getArray("xy")
    tx as Number
    ty as Number
    return sqrt(sqr(tx.toDouble() - x.toDouble()) + sqr(ty.toDouble() - y.toDouble()))
}

fun Graph.toJGraphT(): org.jgrapht.Graph<Node, Edge> {
    val res = DefaultUndirectedWeightedGraph<Node, Edge>(Edge::class.java)
    getNodeIterator<Node>().forEach {
        res.addVertex(it)
    }
    getEdgeIterator<Edge>().forEach {
        val lhv = it.getSourceNode<Node>()
        val rhv = it.getTargetNode<Node>()
        res.addEdge(lhv, rhv, it)
        res.setEdgeWeight(lhv, rhv, lhv.distance(rhv))
    }
    /* make graph complete */
    for(lhv in getNodeIterator<Node>()) {
        for(rhv in getNodeIterator<Node>()) {
            if(lhv !== rhv && !res.containsEdge(lhv, rhv)) {
                res.addEdge(lhv, rhv, edgeFactory().newInstance("id", lhv, rhv, false))
                res.setEdgeWeight(lhv, rhv, lhv.distance(rhv))
            }
        }
    }
    return res
}

object Christofides {

    fun path(graph: Graph): List<Node> {
        val chris = ChristofidesThreeHalvesApproxMetricTSP<Node, Edge>()

        val tour = chris.getTour(graph.toJGraphT())

        return tour.vertexList
    }

}
