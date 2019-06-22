package ru.spbstu.util

import kotlinx.coroutines.Deferred

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

suspend fun <T> List<Deferred<T>>.awaitAll() = kotlinx.coroutines.awaitAll(*this.toTypedArray())
