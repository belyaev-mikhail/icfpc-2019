package ru.spbstu

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.KotlinModule
import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.types.int
import org.graphstream.algorithm.AStar
import org.graphstream.graph.Edge
import org.graphstream.graph.Node
import org.graphstream.graph.implementations.SingleGraph
import org.organicdesign.fp.collections.PersistentHashMap
import ru.spbstu.ktuples.jackson.KTuplesModule
import ru.spbstu.map.GameMap
import ru.spbstu.parse.parseFile
import java.io.File

object Main : CliktCommand() {
    val map: String by option(help = "Map to run on").default("all")
    val gui: Boolean by option().flag(default = false)

//    val count: Int by option(help = "Number of greetings").int().default(1)
//    val name: String? by option(help = "The person to greet")

    override fun run() {
        val pshm = PersistentHashMap.empty<String, Int>()
        println(pshm.assoc("A", 1).assoc("B", 2).assoc("C", 4))

        if (map != "all") {
            val data = File("docs/part-1-initial/$map.desc").let { parseFile(it.name, it.readText()) }
            val map = GameMap(data)
            println(data.name)
            println(map.toASCII())
            if(gui) map.display()
        } else {
            val data = File("docs/part-1-initial").walkTopDown().filter { it.extension == "desc" }.map {
                parseFile(it.name, it.readText())
            }.toList()

            for (datum in data.take(50)) {
                val map = GameMap(datum)

                println(datum.name)
                println(map.toASCII())
            }
        }


    }
}

fun main(args: Array<String>) = Main.main(args)
