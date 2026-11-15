package com.genvision.nova.core

import com.genvision.nova.core.data.LoadResult
import com.genvision.nova.core.data.LoadedData
import com.genvision.nova.core.data.MapLoader
import kotlin.test.assertIs

/** Loads the two real files from the brief for tests. */
object TestData {
    private fun resource(name: String) =
        TestData::class.java.classLoader.getResource(name)!!.readText()

    val loaded: LoadedData by lazy {
        val r = MapLoader.load(resource("map.json"), resource("markers.json"))
        assertIs<LoadResult.Success>(r)
        r.data
    }
}
