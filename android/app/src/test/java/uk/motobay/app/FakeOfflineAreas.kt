package uk.motobay.app

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import uk.motobay.app.data.BBox
import uk.motobay.app.data.OfflineArea
import uk.motobay.app.data.OfflineAreas

/** Saves "areas" in memory, finishing at once. */
class FakeOfflineAreas : OfflineAreas {
    override val areas = MutableStateFlow<List<OfflineArea>>(emptyList())
    override val progress = MutableStateFlow<Float?>(null)
    val downloads = mutableListOf<Pair<String, BBox>>()
    override fun refresh() {}
    override suspend fun download(name: String, box: BBox, styleUrl: String) {
        downloads += name to box
        areas.update { it + OfflineArea(it.size + 1L, name, 1_700_000_000_000, 3_200_000, true) }
    }
    override suspend fun delete(id: Long) = areas.update { l -> l.filterNot { it.id == id } }
}
