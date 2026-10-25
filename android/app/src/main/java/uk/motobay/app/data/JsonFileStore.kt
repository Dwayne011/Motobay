package uk.motobay.app.data

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.serialization.KSerializer
import kotlinx.serialization.json.Json
import java.io.File

/**
 * A value kept in memory as a StateFlow and saved to a JSON file on every
 * change. Small and dependency-free; fine for settings and the mock backend.
 */
class JsonFileStore<T>(
    private val file: File,
    private val serializer: KSerializer<T>,
    default: T,
    private val scope: CoroutineScope,
) {
    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }
    // One writer at a time, always writing the latest value.
    private val writer = Dispatchers.IO.limitedParallelism(1)
    private val _state = MutableStateFlow(read() ?: default)
    val state: StateFlow<T> = _state.asStateFlow()

    fun update(transform: (T) -> T) {
        _state.update(transform)
        scope.launch(writer) {
            val tmp = File(file.parentFile, "${file.name}.tmp")
            tmp.writeText(json.encodeToString(serializer, _state.value))
            tmp.renameTo(file)
        }
    }

    private fun read(): T? = runCatching { json.decodeFromString(serializer, file.readText()) }.getOrNull()
}
