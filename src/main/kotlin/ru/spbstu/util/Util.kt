package ru.spbstu.util

fun <K> MutableMap<K, Int>.inc(key: K) {
    val value = this[key] ?: 0

    this[key] = value + 1
}
