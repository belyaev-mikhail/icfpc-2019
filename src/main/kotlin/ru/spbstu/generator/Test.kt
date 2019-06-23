package ru.spbstu.generator

import ru.spbstu.map.Point
import java.awt.Color
import java.awt.Dimension
import java.awt.Graphics
import javax.swing.JFrame


class Test(val walls: Set<Point>, val parameters: Parameters, val busters: BusterGenerator.Busters) : JFrame() {
    private val SIZE = 10
    private val SHIFT = 100

    init {
        val size = parameters.mapSize * SIZE + 2 * SHIFT
        this.preferredSize = Dimension(size, size)
        this.pack()
        this.isVisible = true
        this.defaultCloseOperation = EXIT_ON_CLOSE
    }

    override fun paint(g: Graphics) {
        super.paint(g)

        g.color = Color.WHITE
        g.fillRect(SHIFT, SHIFT, parameters.mapSize * SIZE, parameters.mapSize * SIZE)
        g.color = Color.BLACK
        walls.forEach {
            g.fillRect(SHIFT + it.v1 * SIZE, SHIFT + it.v0 * SIZE, SIZE, SIZE)
        }
        g.color = Color.RED
        parameters.pathsPoints.forEach {
            g.fillRect(SHIFT + it.v1 * SIZE, SHIFT + it.v0 * SIZE, SIZE, SIZE)
        }
        g.color = Color.BLUE
        parameters.wallsPoints.forEach {
            g.fillRect(SHIFT + it.v1 * SIZE, SHIFT + it.v0 * SIZE, SIZE, SIZE)
        }
        val busterPoints = busters.spawnPoints + busters.cloningBoosters + busters.drills + busters.fastWheels +
                busters.manipulators + busters.teleports + setOf(busters.robot)
        g.color = Color.GREEN
        busterPoints.forEach {
            g.fillRect(SHIFT + it.v1 * SIZE, SHIFT + it.v0 * SIZE, SIZE, SIZE)
        }
    }
}