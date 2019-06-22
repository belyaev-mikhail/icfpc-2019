package ru.spbstu.player

import ru.spbstu.map.Cell
import ru.spbstu.map.Point
import ru.spbstu.map.Status
import ru.spbstu.map.neighbours
import ru.spbstu.sim.*
import ru.spbstu.util.DisjointSets
import ru.spbstu.util.withIdx
import ru.spbstu.wheels.*

fun Simulator.groups(): Int {
    val dj = DisjointSets<Point>()
    val counter = mutableMapOf<Any?, Int>()

    for ((p, c) in this.gameMap.cells) {
        dj.add(p)
        for (n in p.neighbours()) {
            dj.add(n)
            if (gameMap[n].status == c.status) dj.union(p, n)
        }
    }

    return this.gameMap.cells.keys.mapTo(mutableSetOf()) { dj.root(it) }.size
}

data class SimAndCommand(val sim: Simulator, val command: Command, val points: Set<Point>) {
    val empties: Double by lazy {
        sim.gameMap.cells.filter<Point, Cell> { it.key in points }.count {
            it.value.status == Status.EMPTY
        }.toDouble()
    }

    val groups: Double by lazy {
        +sim.groups().toDouble()
    }
    val neighbors by lazy {
        val commands = listOf(TURN_CW, TURN_CCW, MOVE_UP, MOVE_RIGHT, MOVE_LEFT, MOVE_DOWN)
        commands.mapNotNull { tryEx { sim.apply(it) }.getOrNull()?.run { SimAndCommand(this, it, points) } }
    }

    val hash by lazy { sim.gameMap.hashCode() }

    override fun equals(other: Any?): Boolean = other is SimAndCommand && hash == other.hash

    override fun hashCode(): Int = hash
}

fun wholeMapAStar(sim: Simulator, points: Set<Point>) = run {
    val msim = ref(sim)
    val vis = SimFrame(10) { msim.value }
    aStarSearch(
            SimAndCommand(sim, NOOP, points),
            heur = { it.empties },
            neighbours = { it.neighbors.asSequence() },
            goal = { msim.assign(it.sim); vis.repaint(); it.empties == 0.0 }
    )
}

fun persistentBot(simref: MutableRef<Simulator>, points: Set<Point>, idx: Int = 0) =
        sequence {
            while (true) {
                val sim by simref
                val mpp = wholeMapAStar(sim, points).orEmpty().reversed().drop(1)
                yieldAll(mpp.map { it.command })
            }
        }.withIdx(idx)
