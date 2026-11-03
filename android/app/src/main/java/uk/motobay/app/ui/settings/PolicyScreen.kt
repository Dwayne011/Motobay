package uk.motobay.app.ui.settings

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp

/** Which bundled policy page to show. */
enum class Policy(val asset: String, val title: String, val url: String) {
    Privacy("policies/privacy.md", "Privacy policy", uk.motobay.app.data.Legal.PRIVACY_POLICY_URL),
    DeleteAccount("policies/delete-account.md", "Delete your account", uk.motobay.app.data.Legal.DELETE_ACCOUNT_URL),
}

/**
 * Shows a policy from the app's own copy of the docs folder's Markdown, so it works offline and
 * before the website is up. The same file is what GitHub Pages serves.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PolicyScreen(policy: Policy, onBack: () -> Unit) {
    val context = LocalContext.current
    val blocks = remember(policy) {
        parseMarkdown(context.assets.open(policy.asset).bufferedReader().use { it.readText() })
    }
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(policy.title) },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back") } },
                actions = {
                    IconButton(onClick = { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(policy.url))) }) {
                        Icon(Icons.AutoMirrored.Filled.OpenInNew, contentDescription = "Open on the website")
                    }
                },
            )
        },
    ) { padding ->
        Column(Modifier.padding(padding).verticalScroll(rememberScrollState()).padding(horizontal = 16.dp).padding(bottom = 32.dp)) {
            blocks.forEach { b -> MarkdownBlock(b) }
        }
    }
}

/** The handful of Markdown shapes the policy files use. */
sealed interface Block {
    data class Heading(val level: Int, val text: String) : Block
    data class Paragraph(val text: String) : Block
    data class Bullets(val items: List<String>) : Block
    data class Table(val header: List<String>, val rows: List<List<String>>) : Block
    data object Rule : Block
}

/** Enough Markdown for the policy pages: headings, paragraphs, bullets, tables, bold and links. */
fun parseMarkdown(src: String): List<Block> {
    var lines = src.lines()
    if (lines.firstOrNull()?.trim() == "---") lines = lines.drop(1).dropWhile { it.trim() != "---" }.drop(1) // YAML front matter
    val out = mutableListOf<Block>()
    val para = StringBuilder()
    val bullets = mutableListOf<String>()
    val table = mutableListOf<List<String>>()
    fun flush() {
        if (para.isNotBlank()) out += Block.Paragraph(para.toString().trim()); para.clear()
        if (bullets.isNotEmpty()) out += Block.Bullets(bullets.toList()); bullets.clear()
        if (table.size >= 2) out += Block.Table(table[0], table.drop(1)); table.clear()
    }
    for (raw in lines) {
        val line = raw.trimEnd()
        when {
            line.startsWith("#") -> { flush(); val level = line.takeWhile { it == '#' }.length; out += Block.Heading(level, line.drop(level).trim()) }
            line.startsWith("|") -> {
                if (para.isNotBlank() || bullets.isNotEmpty()) flush()
                val cells = line.trim().trim('|').split("|").map { it.trim() }
                if (cells.all { it.matches(Regex("-{3,}")) }) continue // the |---|---| separator
                table += cells
            }
            line.startsWith("- ") -> { if (para.isNotBlank() || table.isNotEmpty()) flush(); bullets += line.drop(2).trim() }
            line.isBlank() -> flush()
            line.matches(Regex("^-{3,}$")) -> { flush(); out += Block.Rule }
            bullets.isNotEmpty() && line.startsWith("  ") -> bullets[bullets.lastIndex] = bullets.last() + " " + line.trim()
            else -> { if (table.isNotEmpty() || bullets.isNotEmpty()) flush(); para.append(line.trim()).append(' ') }
        }
    }
    flush()
    return out
}

/** **bold** and [text](url) inline; everything else literal. */
fun inline(text: String): AnnotatedString = buildAnnotatedString {
    val linkless = text.replace(Regex("\\[([^]]+)]\\(([^)]+)\\)"), "$1")
        .replace("`", "") // code spans: just show the text
    var i = 0
    val bold = Regex("\\*\\*(.+?)\\*\\*")
    for (m in bold.findAll(linkless)) {
        append(linkless.substring(i, m.range.first))
        withStyle(SpanStyle(fontWeight = FontWeight.SemiBold)) { append(m.groupValues[1]) }
        i = m.range.last + 1
    }
    append(linkless.substring(i))
}

@Composable
private fun MarkdownBlock(block: Block) {
    val t = MaterialTheme.typography
    val muted = MaterialTheme.colorScheme.onSurfaceVariant
    when (block) {
        is Block.Heading -> Text(
            inline(block.text),
            style = when (block.level) { 1 -> t.headlineSmall; 2 -> t.titleLarge; else -> t.titleMedium },
            color = if (block.level == 2) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(top = if (block.level == 1) 16.dp else 24.dp, bottom = 8.dp),
        )
        is Block.Paragraph -> Text(inline(block.text), style = t.bodyMedium, modifier = Modifier.padding(bottom = 12.dp))
        is Block.Bullets -> Column(Modifier.padding(bottom = 12.dp)) {
            block.items.forEach { item ->
                Row(Modifier.padding(bottom = 4.dp)) {
                    Text("•", style = t.bodyMedium, modifier = Modifier.padding(end = 8.dp))
                    Text(inline(item), style = t.bodyMedium)
                }
            }
        }
        // Phone-width tables read better as one card per row, header as labels.
        is Block.Table -> Column(Modifier.padding(bottom = 12.dp)) {
            block.rows.forEach { row ->
                OutlinedCard(Modifier.fillMaxWidth().padding(bottom = 8.dp)) {
                    Column(Modifier.padding(12.dp)) {
                        row.forEachIndexed { i, cell ->
                            if (cell.isBlank()) return@forEachIndexed
                            if (i == 0) {
                                Text(inline(cell), style = t.titleSmall)
                            } else {
                                Text(block.header.getOrNull(i).orEmpty(), style = t.labelSmall, color = muted, modifier = Modifier.padding(top = 8.dp))
                                Text(inline(cell), style = t.bodyMedium)
                            }
                        }
                    }
                }
            }
        }
        Block.Rule -> HorizontalDivider(Modifier.padding(vertical = 12.dp))
    }
}
