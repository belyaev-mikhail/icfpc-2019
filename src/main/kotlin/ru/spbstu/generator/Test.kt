package ru.spbstu.generator

import ru.spbstu.map.Point
import java.awt.Color
import java.awt.Dimension
import java.awt.Graphics
import javax.swing.JFrame


class Test(val map: Map<Point, TunnelGenerator.Cell>, val parameters: Parameters) : JFrame() {
    private val SIZE = 10

    init {
        this.preferredSize = Dimension(parameters.mapSize * SIZE, parameters.mapSize * SIZE)
        this.pack()
        this.isVisible = true
        this.defaultCloseOperation = EXIT_ON_CLOSE
    }

    override fun paint(g: Graphics) {
        super.paint(g)

        g.color = Color.WHITE
        g.fillRect(0, 0, parameters.mapSize * SIZE, parameters.mapSize * SIZE)
        map.forEach { (it, cell) ->
            g.color = when (cell) {
                TunnelGenerator.Cell.WALL -> Color.BLACK
                TunnelGenerator.Cell.PATH -> Color.WHITE
            }
            g.fillRect(it.v0 * SIZE, it.v1 * SIZE, SIZE, SIZE)
        }
        parameters.wallsPoints.forEach {
            g.fillRect(it.v0 * SIZE, it.v1 * SIZE, SIZE, SIZE)
        }
        g.color = Color.RED
        parameters.pathsPoints.forEach {
            g.fillRect(it.v0 * SIZE, it.v1 * SIZE, SIZE, SIZE)
        }
        g.color = Color.BLUE
        parameters.wallsPoints.forEach {
            g.fillRect(it.v0 * SIZE, it.v1 * SIZE, SIZE, SIZE)
        }
    }
}