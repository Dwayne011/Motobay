package uk.motobay.app.ui.saved

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import uk.motobay.app.ui.LocalApp
import uk.motobay.app.ui.components.BayListItem
import uk.motobay.app.util.distanceMetres
import uk.motobay.app.util.formatDistance

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SavedScreen(onOpenBay: (String) -> Unit) {
    val app = LocalApp.current
    val favourites by app.user.favourites.collectAsStateWithLifecycle()
    val position by app.location.position.collectAsStateWithLifecycle()
    val items = favourites.values
        .map { bay -> bay to position?.let { distanceMetres(it, bay) } }
        .sortedWith(compareBy({ it.second ?: 0.0 }, { it.first.title }))
    val scroll = TopAppBarDefaults.pinnedScrollBehavior()

    Scaffold(
        modifier = Modifier.nestedScroll(scroll.nestedScrollConnection),
        topBar = { TopAppBar(title = { Text("Saved") }, scrollBehavior = scroll) },
    ) { padding ->
        if (items.isEmpty()) {
            Column(
                Modifier.padding(padding).fillMaxSize().padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterVertically),
            ) {
                Icon(Icons.Outlined.StarBorder, contentDescription = null, Modifier.size(48.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                Text("No saved bays yet", style = MaterialTheme.typography.titleMedium)
                Text(
                    "Save the bays you use most so you can get directions in one tap.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )
            }
        } else {
            LazyColumn(contentPadding = padding) {
                items(items, key = { it.first.id }) { (bay, d) ->
                    BayListItem(bay, d?.let(::formatDistance)) { onOpenBay(bay.id) }
                }
            }
        }
    }
}
