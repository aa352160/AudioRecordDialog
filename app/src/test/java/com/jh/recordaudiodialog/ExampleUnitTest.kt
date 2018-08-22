package com.jh.recordaudiodialog

import android.text.TextUtils.split
import org.junit.Test

import org.junit.Assert.*

/**
 * Example local unit test, which will execute on the development machine (host).
 *
 * See [testing documentation](http://d.android.com/tools/testing).
 */
class ExampleUnitTest {
    @Test
    fun addition_isCorrect() {
        assertEquals(4, 2 + 2)
        parseHighLightTag("https://redring.jubill.com/ask-again/{gi/{gid}-{cid}")
    }

    fun parseHighLightTag(text: String) = text.split("/").last().split("-")
}
