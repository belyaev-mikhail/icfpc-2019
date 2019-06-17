package ru.spbstu

import ru.spbstu.ktuples.Tuple
import ru.spbstu.ktuples.Tuple3
import kotlin.test.Test
import kotlin.test.assertEquals

class JacksonTest {
    data class TestData(val i: Int, val s: List<String?>, val t: Tuple3<Double, Double, String?>)

    @Test
    fun sanityCheck() {

        val td: List<TestData> = fromJson(
                """
                    [
                      {
                        "i": 2,
                        "s": [null, "Hello"],
                        "t": [3.14, 2.15, null]
                      },
                      {
                        "i": 12,
                        "s": [],
                        "t": [3.14, 2.15, "M"]
                      }
                    ]
                """
        )

        assertEquals(
                listOf(
                        TestData(2, listOf(null, "Hello"), Tuple(3.14, 2.15, null)),
                        TestData(12, listOf(), Tuple(3.14, 2.15, "M"))
                ),
                td
        )

    }
}

