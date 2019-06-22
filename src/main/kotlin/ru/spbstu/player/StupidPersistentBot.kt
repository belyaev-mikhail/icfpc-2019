package ru.spbstu.player
import ru.spbstu.map.*
import ru.spbstu.sim.*
import ru.spbstu.util.DisjointSets
import ru.spbstu.wheels.*

fun Simulator.groups(): Int {
    val dj = DisjointSets<Point>()
    val counter = mutableMapOf<Any?, Int>()

    for((p, c) in this.gameMap.cells) {
        dj.add(p)
        for(n in p.neighbours()) {
            dj.add(n)
            if(gameMap[n].status == c.status) dj.union(p, n)
        }
    }

    return this.gameMap.cells.keys.mapTo(mutableSetOf()) { dj.root(it) }.size
}

data class SimAndCommand(val sim: Simulator, val command: Command) {
    val empties: Double by lazy {
        sim.gameMap.cells.count<Point, Cell> {
            it.value.status == Status.EMPTY
        }.toDouble()
    }

    val groups: Double by lazy {
        +sim.groups().toDouble()
    }
    val neighbors by lazy {
        val commands = listOf(TURN_CW, TURN_CCW, MOVE_UP, MOVE_RIGHT, MOVE_LEFT, MOVE_DOWN)
        commands.mapNotNull { tryEx { sim.apply(it) }.getOrNull()?.run { SimAndCommand(this, it) } }
    }

    val hash by lazy { sim.gameMap.hashCode() }

    override fun equals(other: Any?): Boolean = other is SimAndCommand && hash == other.hash

    override fun hashCode(): Int = hash
}

fun wholeMapAStar(sim: Simulator) = run {
    val msim = ref(sim)
    val vis = SimFrame(10) { msim.value }
    aStarSearch(
            SimAndCommand(sim, NOOP),
            heur = { it.empties },
            neighbours = { it.neighbors.asSequence() },
            goal = { msim.assign(it.sim); vis.repaint(); it.empties == 0.0 }
    )
}

fun persistentBot(simref: MutableRef<Simulator>, points: Set<Point> /*todo*/) =
        sequence {
            while(true) {
                val sim by simref
                val mpp = wholeMapAStar(sim).orEmpty().reversed().drop(1)
                yieldAll(mpp.map { it.command })
            }
        }