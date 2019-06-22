package ru.spbstu.generator

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.option
import java.io.File

object Main : CliktCommand() {
    val input: String by option().default("sample/puzzle.cond")
    val output: String by option().default("sample/task.desc")

    override fun run() {
        val rawParameters = File(input).readText()
        val parameters = Parameters.read(rawParameters)
        val map = TunnelGenerator(parameters).generate()
        val walls = map.filter { it.value == TunnelGenerator.Cell.WALL }.map { it.key }.toSet()
        val bonuses = BusterGenerator(walls, parameters)
        println(bonuses)
        Test(map, parameters).repaint()
    }
}

fun main(args: Array<String>) = Main.main(args)
