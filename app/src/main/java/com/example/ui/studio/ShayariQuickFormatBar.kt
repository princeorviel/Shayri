package com.example.ui.studio

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.util.HinglishTransliterator

/**
 * Lightweight, non-intrusive helper strip above the phone's own keyboard
 * offering quick shayari formatting keys (New Line, Purnviram, Em-dash) and mood word sparks.
 */
@Composable
fun ShayariQuickFormatBar(
    onInsertSnippet: (snippet: String, padSpace: Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.55f))
    Surface(
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        modifier = modifier
            .fillMaxWidth()
            .testTag("studio_quick_format_bar")
    ) {
        LazyRow(
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            item(key = "newline_key") {
                QuickFormatChip(
                    label = stringResource(R.string.studio_newline_key),
                    testTag = "studio_newline_key",
                    onClick = { onInsertSnippet("\n", false) }
                )
            }

            item(key = "purnviram_key") {
                QuickFormatChip(
                    label = "।",
                    testTag = "studio_purnviram_key",
                    onClick = { onInsertSnippet("। ", false) }
                )
            }

            item(key = "emdash_key") {
                QuickFormatChip(
                    label = "—",
                    testTag = "studio_emdash_key",
                    onClick = { onInsertSnippet(" — ", false) }
                )
            }

            items(HinglishTransliterator.quickMoodWords) { moodWord ->
                QuickFormatChip(
                    label = moodWord,
                    testTag = "mood_word_chip_$moodWord",
                    onClick = { onInsertSnippet(moodWord, true) }
                )
            }
        }
    }
}

@Composable
private fun QuickFormatChip(
    label: String,
    testTag: String,
    onClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = Modifier
            .defaultMinSize(minWidth = 48.dp, minHeight = 48.dp)
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .testTag(testTag)
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp)
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Medium),
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}
