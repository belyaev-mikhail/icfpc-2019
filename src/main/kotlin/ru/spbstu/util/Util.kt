package ru.spbstu.util

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
