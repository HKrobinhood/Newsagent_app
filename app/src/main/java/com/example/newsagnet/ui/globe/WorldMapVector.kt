package com.example.newsagnet.ui.globe

import android.content.Context
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object WorldMapRepository {
    @Volatile
    private var cachedMap: WorldMapVector? = null

    suspend fun loadWorldMap(context: Context): WorldMapVector = withContext(Dispatchers.IO) {
        cachedMap?.let { return@withContext it }

        try {
            val jsonString = context.assets.open("world_map_vector.json").bufferedReader().use {
                it.readText()
            }
            val raw = Gson().fromJson(jsonString, RawWorldMapDto::class.java)

            val parsedLand = raw.land.map { ring ->
                val flat = FloatArray(ring.size * 2)
                var idx = 0
                for (pt in ring) {
                    flat[idx++] = pt[0]
                    flat[idx++] = pt[1]
                }
                flat
            }

            val parsedBorders = raw.borders.map { line ->
                val flat = FloatArray(line.size * 2)
                var idx = 0
                for (pt in line) {
                    flat[idx++] = pt[0]
                    flat[idx++] = pt[1]
                }
                flat
            }

            val vectorMap = WorldMapVector(parsedLand, parsedBorders)
            cachedMap = vectorMap
            vectorMap
        } catch (_: Exception) {
            WorldMapVector(emptyList(), emptyList())
        }
    }
}

private data class RawWorldMapDto(
    val land: List<List<List<Float>>>,
    val borders: List<List<List<Float>>>
)
