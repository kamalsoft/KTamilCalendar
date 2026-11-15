package com.kamalsoft.tamilcalendar.tv.web

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate

class JsonTest {
    @Test
    fun writesNestedValuesAndEscapesStrings() {
        val json = toJson(mapOf("a" to "x\"y\n", "b" to listOf(1, true, null), "c" to LocalDate.of(2026, 11, 15)))
        assertEquals("""{"a":"x\"y\n","b":[1,true,null],"c":"2026-11-15"}""", json)
    }
}
