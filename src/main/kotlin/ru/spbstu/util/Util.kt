package ru.spbstu.util

import ru.spbstu.map.Point
import ru.spbstu.sim.Command
import ru.spbstu.sim.Simulator
import ru.spbstu.sim.TICK
import ru.spbstu.wheels.MutableRef

fun <K> MutableMap<K, Int>.inc(key: K) {
    val value = this[key] ?: 0

    this[key] = value + 1
}

fun <K> MutableMap<K, Int>.dec(key: K) {
    val value = this[key] ?: 0

    val newValue = value - 1

    when {
        newValue > 0 -> this[key] = value
        newValue == 0 -> this.remove(key)
        else -> throw ArithmeticException("Cannot dec $key for $this")
    }
}

fun <K> Map<K, Int>.inc(key: K): Map<K, Int> {
    val value = this[key] ?: 0

    return this + (key to value + 1)
}

fun <K> Map<K, Int>.dec(key: K): Map<K, Int> {
    val value = this[key] ?: 0

    val newValue = value - 1

    return when {
        newValue > 0 -> this + (key to value)
        newValue == 0 -> this - key
        else -> throw ArithmeticException("Cannot dec $key for $this")
    }
}

fun <T> Sequence<T>.withIdx(idx: Int) = map { idx to it }

fun <T> Sequence<Pair<Int, T>>.toSolution(): String {
    val groups = groupBy { it.first }
    return groups
            .keys
            .sorted()
            .map { groups[it]?.map { it.second }?.joinToString("") }
            .joinToString("#")
}

fun ((MutableRef<Simulator>, Set<Point>, Int) -> Sequence<Pair<Int, Command>>).withAutoTick() =
        { sim: MutableRef<Simulator>, points: Set<Point>, idx: Int ->
            this(sim, points, idx).flatMap { sequenceOf(it, 0 to TICK) }
        }
