package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.VolumeDown
import androidx.compose.material.icons.automirrored.rounded.VolumeUp
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Bedtime
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.audio.ReadingAmbientTrack
import com.example.audio.ReadingSleepTimerOption
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.roundToInt
import kotlin.math.sin

@Composable
fun AmbientWaveBars(
    isPlaying: Boolean,
    color: Color,
    barCount: Int = 3,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "reading_music_wave")
    val phase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = (2f * PI).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1_500, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "reading_music_phase"
    )

    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(2.5.dp)
    ) {
        for (i in 0 until barCount) {
            val h = if (isPlaying) {
                4.5f + abs(sin(phase + i * 1.05f)) * 8.5f
            } else {
                4.5f + (if (i == 1) 3f else 0f)
            }
            Box(
                modifier = Modifier
                    .width(2.5.dp)
                    .height(h.dp)
                    .clip(CircleShape)
                    .background(color.copy(alpha = if (isPlaying) 0.95f else 0.55f))
            )
        }
    }
}

/**
 * Subtle frosted-glass ambient music pill displayed in the top overlay of [ShayariReaderScreen].
 * Keeps the bottom 100% clear for vertical poetry swiping while allowing one-tap play/pause
 * and instant soundscape configuration.
 */
@Composable
fun ReaderAmbientMusicPill(
    isPlaying: Boolean,
    activeTrack: ReadingAmbientTrack,
    accentColor: Color,
    textColor: Color,
    onTogglePlay: () -> Unit,
    onOpenConfig: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(50),
        color = Color.Black.copy(alpha = if (isPlaying) 0.42f else 0.30f),
        border = BorderStroke(
            width = 1.dp,
            color = accentColor.copy(alpha = if (isPlaying) 0.55f else 0.28f)
        ),
        modifier = modifier.testTag("reader_ambient_music_pill")
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(start = 4.dp, end = 6.dp)
        ) {
            IconButton(
                onClick = onTogglePlay,
                modifier = Modifier
                    .minimumInteractiveComponentSize()
                    .size(40.dp)
                    .testTag("reader_music_play_toggle")
            ) {
                Icon(
                    imageVector = if (isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                    contentDescription = stringResource(
                        if (isPlaying) R.string.reading_music_pause_cd else R.string.reading_music_play_cd
                    ),
                    tint = if (isPlaying) accentColor else textColor,
                    modifier = Modifier.size(20.dp)
                )
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier
                    .clip(RoundedCornerShape(50))
                    .clickable(onClick = onOpenConfig)
                    .defaultMinSize(minHeight = 40.dp)
                    .padding(horizontal = 8.dp, vertical = 6.dp)
                    .testTag("reader_music_open_config")
            ) {
                if (isPlaying) {
                    AmbientWaveBars(
                        isPlaying = true,
                        color = accentColor,
                        barCount = 3
                    )
                } else {
                    Icon(
                        imageVector = Icons.Rounded.MusicNote,
                        contentDescription = null,
                        tint = accentColor.copy(alpha = 0.85f),
                        modifier = Modifier.size(15.dp)
                    )
                }

                Text(
                    text = if (isPlaying) {
                        activeTrack.labelHi
                    } else {
                        stringResource(R.string.reading_music_pill_off)
                    },
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontWeight = if (isPlaying) FontWeight.SemiBold else FontWeight.Medium,
                        fontSize = 12.sp
                    ),
                    color = textColor.copy(alpha = if (isPlaying) 0.96f else 0.84f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.widthIn(max = 128.dp)
                )

                Icon(
                    imageVector = Icons.Rounded.Tune,
                    contentDescription = stringResource(R.string.reading_music_cd),
                    tint = textColor.copy(alpha = 0.68f),
                    modifier = Modifier.size(14.dp)
                )
            }
        }
    }
}

/**
 * Whisper-subtle floating ambient music bar for poetry browsing screens while music is active.
 */
@Composable
fun FloatingReadingMusicBar(
    visible: Boolean,
    isPlaying: Boolean,
    activeTrack: ReadingAmbientTrack,
    remainingTimerMinutes: Int?,
    onTogglePlay: () -> Unit,
    onOpenConfig: () -> Unit,
    onStopAndDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(
        visible = visible,
        enter = slideInVertically(
            initialOffsetY = { it / 2 },
            animationSpec = spring(stiffness = Spring.StiffnessMediumLow)
        ) + fadeIn(animationSpec = tween(220)),
        exit = slideOutVertically(
            targetOffsetY = { it / 2 },
            animationSpec = tween(180)
        ) + fadeOut(animationSpec = tween(160)),
        modifier = modifier
    ) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.94f),
            tonalElevation = 6.dp,
            shadowElevation = 6.dp,
            border = BorderStroke(
                1.dp,
                MaterialTheme.colorScheme.primary.copy(alpha = 0.28f)
            ),
            modifier = Modifier
                .widthIn(max = 440.dp)
                .fillMaxWidth()
                .testTag("floating_reading_music_bar")
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onOpenConfig)
                    .padding(start = 12.dp, end = 4.dp, top = 4.dp, bottom = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primaryContainer,
                    modifier = Modifier.size(34.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        AmbientWaveBars(
                            isPlaying = isPlaying,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            barCount = 3
                        )
                    }
                }

                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(1.dp)
                ) {
                    Text(
                        text = "${activeTrack.emoji} ${activeTrack.labelHi}",
                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = if (remainingTimerMinutes != null && remainingTimerMinutes > 0) {
                            "${activeTrack.subtitleHi} • ${stringResource(R.string.reading_music_timer_remaining, remainingTimerMinutes)}"
                        } else {
                            activeTrack.subtitleHi
                        },
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                IconButton(
                    onClick = onOpenConfig,
                    modifier = Modifier
                        .minimumInteractiveComponentSize()
                        .testTag("floating_music_open_config")
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Tune,
                        contentDescription = stringResource(R.string.reading_music_cd),
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                }

                IconButton(
                    onClick = onStopAndDismiss,
                    modifier = Modifier
                        .minimumInteractiveComponentSize()
                        .testTag("floating_music_pause_stop")
                ) {
                    Icon(
                        imageVector = if (isPlaying) Icons.Rounded.Pause else Icons.Rounded.Close,
                        contentDescription = stringResource(R.string.reading_music_stop_cd),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}

/**
 * Full configuration bottom sheet for the poetry reading background music player:
 * play/pause hero card, subtle volume slider, auto-mood toggle, 6 instrumental/ambient tracks,
 * and gentle sleep fade-out timer.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReadingMusicSheet(
    isEnabled: Boolean,
    selectedTrack: ReadingAmbientTrack,
    effectiveTrack: ReadingAmbientTrack,
    volume: Float,
    autoMoodEnabled: Boolean,
    selectedTimer: ReadingSleepTimerOption,
    remainingTimerMinutes: Int?,
    onToggleEnabled: (Boolean) -> Unit,
    onSelectTrack: (ReadingAmbientTrack) -> Unit,
    onVolumeChanged: (Float) -> Unit,
    onAutoMoodChanged: (Boolean) -> Unit,
    onSelectTimer: (ReadingSleepTimerOption) -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val volumePercent = (volume * 100f).roundToInt().coerceIn(5, 100)
    val volumeHint = when {
        volumePercent <= 45 -> stringResource(R.string.reading_music_volume_subtle_hint)
        volumePercent <= 75 -> stringResource(R.string.reading_music_volume_balanced_hint)
        else -> stringResource(R.string.reading_music_volume_rich_hint)
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 22.dp)
                .padding(bottom = 26.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.primaryContainer,
                    modifier = Modifier.size(46.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Rounded.MusicNote,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.reading_music_title),
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = stringResource(R.string.reading_music_subtitle),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Hero Now Playing Card
            Surface(
                shape = RoundedCornerShape(22.dp),
                border = BorderStroke(
                    width = 1.dp,
                    color = if (isEnabled) {
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.45f)
                    } else {
                        MaterialTheme.colorScheme.outlineVariant
                    }
                ),
                tonalElevation = 3.dp,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("reading_music_now_playing_card")
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            Brush.linearGradient(
                                listOf(
                                    MaterialTheme.colorScheme.primaryContainer.copy(
                                        alpha = if (isEnabled) 0.55f else 0.25f
                                    ),
                                    MaterialTheme.colorScheme.surfaceContainerHigh
                                )
                            )
                        )
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Surface(
                        shape = CircleShape,
                        color = if (isEnabled) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.surfaceContainerHighest
                        },
                        modifier = Modifier.size(52.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            if (isEnabled) {
                                AmbientWaveBars(
                                    isPlaying = true,
                                    color = MaterialTheme.colorScheme.onPrimary,
                                    barCount = 4
                                )
                            } else {
                                Text(
                                    text = effectiveTrack.emoji,
                                    fontSize = 22.sp
                                )
                            }
                        }
                    }

                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(3.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Surface(
                                shape = RoundedCornerShape(50),
                                color = if (isEnabled) {
                                    MaterialTheme.colorScheme.primary.copy(alpha = 0.16f)
                                } else {
                                    MaterialTheme.colorScheme.surfaceVariant
                                }
                            ) {
                                Text(
                                    text = stringResource(
                                        if (isEnabled) R.string.reading_music_playing_badge else R.string.reading_music_paused_badge
                                    ),
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                                    color = if (isEnabled) {
                                        MaterialTheme.colorScheme.primary
                                    } else {
                                        MaterialTheme.colorScheme.onSurfaceVariant
                                    },
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                )
                            }
                            if (remainingTimerMinutes != null && remainingTimerMinutes > 0 && isEnabled) {
                                Text(
                                    text = "• ${stringResource(R.string.reading_music_timer_remaining, remainingTimerMinutes)}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }

                        Text(
                            text = "${effectiveTrack.emoji} ${effectiveTrack.labelHi}",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = effectiveTrack.subtitleHi,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    Surface(
                        onClick = { onToggleEnabled(!isEnabled) },
                        shape = CircleShape,
                        color = if (isEnabled) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.primaryContainer
                        },
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .size(48.dp)
                            .testTag("reading_music_sheet_play_toggle")
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = if (isEnabled) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                                contentDescription = stringResource(
                                    if (isEnabled) R.string.reading_music_pause_cd else R.string.reading_music_play_cd
                                ),
                                tint = if (isEnabled) {
                                    MaterialTheme.colorScheme.onPrimary
                                } else {
                                    MaterialTheme.colorScheme.onPrimaryContainer
                                },
                                modifier = Modifier.size(26.dp)
                            )
                        }
                    }
                }
            }

            // Volume Control
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(R.string.reading_music_volume_label),
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Surface(
                        shape = RoundedCornerShape(50),
                        color = MaterialTheme.colorScheme.secondaryContainer
                    ) {
                        Text(
                            text = "$volumePercent% • $volumeHint",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                            color = MaterialTheme.colorScheme.onSecondaryContainer,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.VolumeDown,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(20.dp)
                    )
                    Slider(
                        value = volume.coerceIn(0.05f, 1.0f),
                        onValueChange = onVolumeChanged,
                        valueRange = 0.05f..1.0f,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("reading_music_volume_slider")
                    )
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.VolumeUp,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            // Auto-Mood Match Toggle
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = if (autoMoodEnabled) {
                    MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.55f)
                } else {
                    MaterialTheme.colorScheme.surfaceContainer
                },
                border = BorderStroke(
                    width = 1.dp,
                    color = if (autoMoodEnabled) {
                        MaterialTheme.colorScheme.tertiary.copy(alpha = 0.45f)
                    } else {
                        MaterialTheme.colorScheme.outlineVariant
                    }
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("reading_music_auto_mood_row")
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onAutoMoodChanged(!autoMoodEnabled) }
                        .padding(horizontal = 14.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.AutoAwesome,
                        contentDescription = null,
                        tint = if (autoMoodEnabled) {
                            MaterialTheme.colorScheme.tertiary
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        },
                        modifier = Modifier.size(22.dp)
                    )
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = stringResource(R.string.reading_music_auto_mood_title),
                            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = stringResource(R.string.reading_music_auto_mood_subtitle),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = autoMoodEnabled,
                        onCheckedChange = onAutoMoodChanged,
                        modifier = Modifier.testTag("reading_music_auto_mood_switch")
                    )
                }
            }

            // 6 Soft Instrumental & Ambient Soundscapes (2-column grid)
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = stringResource(R.string.reading_music_tracks_heading),
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                    color = MaterialTheme.colorScheme.onSurface
                )

                val trackPairs = ReadingAmbientTrack.entries.chunked(2)
                trackPairs.forEach { rowTracks ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        rowTracks.forEach { track ->
                            val isActiveTrack = if (autoMoodEnabled) {
                                effectiveTrack == track
                            } else {
                                selectedTrack == track
                            }
                            Surface(
                                onClick = { onSelectTrack(track) },
                                shape = RoundedCornerShape(18.dp),
                                color = if (isActiveTrack) {
                                    MaterialTheme.colorScheme.primaryContainer
                                } else {
                                    MaterialTheme.colorScheme.surfaceContainer
                                },
                                border = BorderStroke(
                                    width = if (isActiveTrack) 1.6.dp else 1.dp,
                                    color = if (isActiveTrack) {
                                        MaterialTheme.colorScheme.primary
                                    } else {
                                        MaterialTheme.colorScheme.outlineVariant
                                    }
                                ),
                                modifier = Modifier
                                    .weight(1f)
                                    .defaultMinSize(minHeight = 96.dp)
                                    .testTag("reading_music_track_${track.id}")
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp),
                                    verticalArrangement = Arrangement.spacedBy(5.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = track.emoji,
                                            fontSize = 20.sp
                                        )
                                        if (isActiveTrack) {
                                            Surface(
                                                shape = CircleShape,
                                                color = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.size(20.dp)
                                            ) {
                                                Box(contentAlignment = Alignment.Center) {
                                                    Icon(
                                                        imageVector = Icons.Rounded.Check,
                                                        contentDescription = null,
                                                        tint = MaterialTheme.colorScheme.onPrimary,
                                                        modifier = Modifier.size(13.dp)
                                                    )
                                                }
                                            }
                                        }
                                    }

                                    Text(
                                        text = track.labelHi,
                                        style = MaterialTheme.typography.labelLarge.copy(
                                            fontWeight = if (isActiveTrack) FontWeight.Bold else FontWeight.SemiBold
                                        ),
                                        color = if (isActiveTrack) {
                                            MaterialTheme.colorScheme.onPrimaryContainer
                                        } else {
                                            MaterialTheme.colorScheme.onSurface
                                        },
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )

                                    Text(
                                        text = track.moodHintHi,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = if (isActiveTrack) {
                                            MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.82f)
                                        } else {
                                            MaterialTheme.colorScheme.onSurfaceVariant
                                        },
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Sleep / Gentle Auto-Fade Timer
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Bedtime,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = stringResource(R.string.reading_music_timer_heading),
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    ReadingSleepTimerOption.entries.forEach { option ->
                        val isPicked = option == selectedTimer
                        Surface(
                            onClick = { onSelectTimer(option) },
                            shape = RoundedCornerShape(14.dp),
                            color = if (isPicked) {
                                MaterialTheme.colorScheme.primaryContainer
                            } else {
                                MaterialTheme.colorScheme.surfaceContainer
                            },
                            border = BorderStroke(
                                width = if (isPicked) 1.5.dp else 1.dp,
                                color = if (isPicked) {
                                    MaterialTheme.colorScheme.primary
                                } else {
                                    MaterialTheme.colorScheme.outlineVariant
                                }
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .defaultMinSize(minHeight = 48.dp)
                                .testTag("reading_music_timer_${option.minutes}")
                        ) {
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 10.dp)
                            ) {
                                Text(
                                    text = option.labelHi,
                                    style = MaterialTheme.typography.labelMedium.copy(
                                        fontWeight = if (isPicked) FontWeight.Bold else FontWeight.Medium
                                    ),
                                    color = if (isPicked) {
                                        MaterialTheme.colorScheme.onPrimaryContainer
                                    } else {
                                        MaterialTheme.colorScheme.onSurface
                                    },
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
