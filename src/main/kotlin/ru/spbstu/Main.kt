package ru.spbstu

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.options.prompt
import com.github.ajalt.clikt.parameters.types.int
import org.graphstream.algorithm.AStar
import org.graphstream.graph.Edge
import org.graphstream.graph.Node
import org.graphstream.graph.implementations.SingleGraph

class Hello : CliktCommand() {
    val count: Int by option(help="Number of greetings").int().default(1)
    val name: String? by option(help="The person to greet")

    override fun run() {
        for (i in 1..count) {
            echo("Hello $name!")
        }

        val graph = SingleGraph("Hello")
        graph.addNode<Node>("A").addAttribute("xy", 0, 0)
        graph.addNode<Node>("B").addAttribute("xy", 1, 5)
        graph.addNode<Node>("C").addAttribute("xy", 10, 10)
        graph.addNode<Node>("D").addAttribute("xy", 2, 2)

        graph.addEdge<Edge>("AB", "A", "B", true)
        graph.addEdge<Edge>("BC", "B", "C", true)
        graph.addEdge<Edge>("AD", "A", "D", true)
        graph.addEdge<Edge>("DC", "D", "C", true)

        val astar = AStar(graph, "A", "C")
        astar.setCosts(AStar.DistanceCosts())
        astar.compute()

        for(node in astar.shortestPath.nodePath) {
            node.setAttribute("ui.color", "red")
        }

        println(astar.shortestPath)

        graph.display(false)


    }
}

fun main(args: Array<String>) = Hello().main(args)
