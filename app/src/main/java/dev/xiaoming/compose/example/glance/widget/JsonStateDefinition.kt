package dev.xiaoming.compose.example.glance.widget

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.core.DataStoreFactory
import androidx.datastore.core.Serializer
import androidx.datastore.dataStoreFile
import androidx.glance.state.GlanceStateDefinition
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.KSerializer
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.encodeToStream
import java.io.File
import java.io.InputStream
import java.io.OutputStream

class JsonStateDefinition<T>(val initialValue: T, val serializer: KSerializer<T>) :
    GlanceStateDefinition<T> {

    @OptIn(ExperimentalSerializationApi::class)
    override suspend fun getDataStore(context: Context, fileKey: String): DataStore<T> =
        DataStoreFactory.create(
            serializer = object : Serializer<T> {
                override val defaultValue: T = initialValue

                override suspend fun readFrom(input: InputStream): T {
                    val string = input.bufferedReader().use { it.readText() }
                    return Json.decodeFromString(deserializer = serializer, string = string)
                }

                override suspend fun writeTo(t: T, output: OutputStream) {
                    Json.encodeToStream(serializer, t, output)
                }
            },
            produceFile = {
                getLocation(context, fileKey)
            },
        )

    override fun getLocation(context: Context, fileKey: String): File =
        context.dataStoreFile(fileKey)
}