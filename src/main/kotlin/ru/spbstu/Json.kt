package ru.spbstu

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.KotlinModule
import com.fasterxml.jackson.module.kotlin.readValue
import org.intellij.lang.annotations.Language
import ru.spbstu.ktuples.jackson.KTuplesModule

@PublishedApi
internal val globalMapper = ObjectMapper().registerModule(KotlinModule()).registerModule(KTuplesModule())

inline fun <reified T> fromJson(@Language("JSON") json: String): T =
        globalMapper.readValue<T>(json)

fun <T> toJson(value: T): String =
        globalMapper.writeValueAsString(value)
