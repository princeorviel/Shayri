package com.example.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.RecordVoiceOver
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ai.ShayarVoicePersona
import com.example.audio.ReelBgmTrack
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.sin

sealed interface ShayarVoicePlaybackState {
    data object Idle : ShayarVoicePlaybackState
    data object Loading : ShayarVoicePlaybackState
    data class Speaking(val persona: ShayarVoicePersona) : ShayarVoicePlaybackState
    data class QuotaFallback(val persona: ShayarVoicePersona, val detail: String) : ShayarVoicePlaybackState
    data class Error(val message: String) : ShayarVoicePlaybackState
}

@Composable
fun ReelVoiceStatusPill(
    voiceState: ShayarVoicePlaybackState,
    bgmTrack: ReelBgmTrack,
    isAudioEnabled: Boolean,
    onOpenSoundMixer: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (!isAudioEnabled) return

    val infiniteTransition = rememberInfiniteTransition(label = "voice_wave")
    val phase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = (2f * PI).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1_100, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "wave_phase"
    )

    val pillBg = when (voiceState) {
        is ShayarVoicePlaybackState.Error -> Color(0xFF5C1E24).copy(alpha = 0.82f)
        is ShayarVoicePlaybackState.QuotaFallback -> Color(0xFF2D2212).copy(alpha = 0.78f)
        else -> Color.Black.copy(alpha = 0.48f)
    }
    val borderColor = when (voiceState) {
        is ShayarVoicePlaybackState.Speaking -> Color(0xFFFFD580).copy(alpha = 0.65f)
        is ShayarVoicePlaybackState.QuotaFallback -> Color(0xFFFFB74D).copy(alpha = 0.72f)
        is ShayarVoicePlaybackState.Loading -> Color(0xFFFF8FA3).copy(alpha = 0.55f)
        is ShayarVoicePlaybackState.Error -> Color(0xFFFF8A80).copy(alpha = 0.7f)
        ShayarVoicePlaybackState.Idle -> Color.White.copy(alpha = 0.28f)
    }

    Surface(
        onClick = onOpenSoundMixer,
        shape = RoundedCornerShape(50),
        color = pillBg,
        border = BorderStroke(0.9.dp, borderColor),
        modifier = modifier.testTag("reel_voice_status_pill")
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(7.dp)
        ) {
            when (voiceState) {
                is ShayarVoicePlaybackState.Loading -> {
                    CircularProgressIndicator(
                        strokeWidth = 1.8.dp,
                        color = Color(0xFFFFD580),
                        modifier = Modifier.size(13.dp)
                    )
                    Text(
                        text = "🎙️ शायर आवाज़ तैयार हो रही है…",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 11.sp
                        ),
                        color = Color(0xFFFFE4A8),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                is ShayarVoicePlaybackState.Speaking -> {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        for (bar in 0 until 4) {
                            val h = 5f + abs(sin(phase + bar * 0.9f)) * 9f
                            Box(
                                modifier = Modifier
                                    .width(2.5.dp)
                                    .height(h.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFFFFD580))
                            )
                        }
                    }
                    Text(
                        text = "${voiceState.persona.emoji} ${voiceState.persona.labelHi} • ${bgmTrack.emoji} ${bgmTrack.labelHi}",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 11.sp
                        ),
                        color = Color.White,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                is ShayarVoicePlaybackState.QuotaFallback -> {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        for (bar in 0 until 4) {
                            val h = 5f + abs(sin(phase + bar * 0.9f)) * 8f
                            Box(
                                modifier = Modifier
                                    .width(2.5.dp)
                                    .height(h.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFFFFB74D))
                            )
                        }
                    }
                    Column(verticalArrangement = Arrangement.spacedBy(1.dp)) {
                        Text(
                            text = "${voiceState.persona.emoji} ${voiceState.persona.labelHi} • ${bgmTrack.emoji} ${bgmTrack.labelHi}",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 11.sp
                            ),
                            color = Color.White,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        if (voiceState.detail.isNotBlank()) {
                            Text(
                                text = voiceState.detail,
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Normal,
                                    fontSize = 9.5.sp
                                ),
                                color = Color(0xFFFFCC80),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
                is ShayarVoicePlaybackState.Error -> {
                    Text(
                        text = "⚠️ ${voiceState.message}",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 11.sp
                        ),
                        color = Color(0xFFFFCDD2),
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                ShayarVoicePlaybackState.Idle -> {
                    Icon(
                        imageVector = Icons.Rounded.MusicNote,
                        contentDescription = null,
                        tint = Color(0xFFFFD580),
                        modifier = Modifier.size(13.dp)
                    )
                    Text(
                        text = "${bgmTrack.emoji} ${bgmTrack.labelHi} • आवाज़ बदलें",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Medium,
                            fontSize = 11.sp
                        ),
                        color = Color.White.copy(alpha = 0.92f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReelSoundMixerSheet(
    selectedPersona: ShayarVoicePersona?,
    effectivePersona: ShayarVoicePersona,
    selectedBgmTrack: ReelBgmTrack?,
    effectiveBgmTrack: ReelBgmTrack,
    onSelectPersona: (ShayarVoicePersona?) -> Unit,
    onSelectBgmTrack: (ReelBgmTrack?) -> Unit,
    onDismiss: () -> Unit,
    voiceState: ShayarVoicePlaybackState = ShayarVoicePlaybackState.Idle,
    onRetryVoice: (() -> Unit)? = null,
    allowLocalTtsFallback: Boolean = true,
    onToggleLocalTtsFallback: ((Boolean) -> Unit)? = null
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Color(0xFF14141B),
        contentColor = Color.White
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.AutoAwesome,
                        contentDescription = null,
                        tint = Color(0xFFFFD580),
                        modifier = Modifier.size(22.dp)
                    )
                    Text(
                        text = "इस Reel की आवाज़ और म्यूज़िक",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = Color.White
                    )
                }
                Text(
                    text = "बदलाव सिर्फ़ इसी शायरी पर लागू होगा — बाकी Reels की आवाज़ व म्यूज़िक अलग रहेंगे",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.68f)
                )
            }

            // Section 1: Human-like Shayar Voice Persona
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.RecordVoiceOver,
                        contentDescription = null,
                        tint = Color(0xFFFF8FA3),
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = "शायर व लड़की शायर की आवाज़ें (8 अलग-अलग अंदाज़)",
                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
                        color = Color.White.copy(alpha = 0.92f)
                    )
                }

                // Auto-match chip
                Surface(
                    onClick = { onSelectPersona(null) },
                    shape = RoundedCornerShape(14.dp),
                    color = if (selectedPersona == null) Color(0xFFFF4D6D).copy(alpha = 0.22f) else Color.White.copy(alpha = 0.06f),
                    border = BorderStroke(
                        1.dp,
                        if (selectedPersona == null) Color(0xFFFF4D6D) else Color.White.copy(alpha = 0.14f)
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("mixer_voice_auto")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "✨ ऑटो-मैच (हर शायरी पर अलग शायर / लड़की शायर)",
                                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                                color = Color.White
                            )
                            Text(
                                text = "अभी सक्रिय: ${effectivePersona.emoji} ${effectivePersona.labelHi} (${effectivePersona.genderBadgeHi})",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.White.copy(alpha = 0.72f)
                            )
                        }
                        if (selectedPersona == null) {
                            Icon(
                                imageVector = Icons.Rounded.CheckCircle,
                                contentDescription = null,
                                tint = Color(0xFFFF4D6D),
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }

                LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    items(ShayarVoicePersona.entries, key = { it.id }) { persona ->
                        val isPicked = selectedPersona == persona
                        val badgeColor = if (persona.isFemale) Color(0xFFFF8FA3) else Color(0xFF90CAF9)
                        Surface(
                            onClick = { onSelectPersona(persona) },
                            shape = RoundedCornerShape(16.dp),
                            color = if (isPicked) Color(0xFFFF4D6D).copy(alpha = 0.22f) else Color.White.copy(alpha = 0.07f),
                            border = BorderStroke(
                                width = if (isPicked) 1.5.dp else 1.dp,
                                color = if (isPicked) Color(0xFFFF4D6D) else Color.White.copy(alpha = 0.14f)
                            ),
                            modifier = Modifier
                                .width(164.dp)
                                .testTag("mixer_voice_${persona.id}")
                        ) {
                            Column(
                                modifier = Modifier.padding(12.dp),
                                verticalArrangement = Arrangement.spacedBy(5.dp)
                            ) {
                                Surface(
                                    shape = RoundedCornerShape(50),
                                    color = badgeColor.copy(alpha = 0.16f)
                                ) {
                                    Text(
                                        text = persona.genderBadgeHi,
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold
                                        ),
                                        color = badgeColor,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                    )
                                }
                                Text(
                                    text = "${persona.emoji} ${persona.labelHi}",
                                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                                    color = Color.White,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = persona.subtitleHi,
                                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                                    color = Color.White.copy(alpha = 0.70f),
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }
            }

            // Section 2: TikTok-style Viral Background Music Tracks
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.MusicNote,
                        contentDescription = null,
                        tint = Color(0xFFFFD580),
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = "अलग-अलग राग व स्टाइल बैकग्राउंड म्यूज़िक",
                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
                        color = Color.White.copy(alpha = 0.92f)
                    )
                }

                Surface(
                    onClick = { onSelectBgmTrack(null) },
                    shape = RoundedCornerShape(14.dp),
                    color = if (selectedBgmTrack == null) Color(0xFFFFD580).copy(alpha = 0.18f) else Color.White.copy(alpha = 0.06f),
                    border = BorderStroke(
                        1.dp,
                        if (selectedBgmTrack == null) Color(0xFFFFD580) else Color.White.copy(alpha = 0.14f)
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("mixer_bgm_auto")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "✨ ऑटो म्यूज़िक (हर शायरी पर नया राग/धुन)",
                                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                                color = Color.White
                            )
                            Text(
                                text = "अभी चल रहा: ${effectiveBgmTrack.emoji} ${effectiveBgmTrack.labelHi}",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.White.copy(alpha = 0.72f)
                            )
                        }
                        if (selectedBgmTrack == null) {
                            Icon(
                                imageVector = Icons.Rounded.CheckCircle,
                                contentDescription = null,
                                tint = Color(0xFFFFD580),
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }

                LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    items(ReelBgmTrack.entries, key = { it.id }) { track ->
                        val isPicked = selectedBgmTrack == track
                        Surface(
                            onClick = { onSelectBgmTrack(track) },
                            shape = RoundedCornerShape(16.dp),
                            color = if (isPicked) Color(0xFFFFD580).copy(alpha = 0.18f) else Color.White.copy(alpha = 0.07f),
                            border = BorderStroke(
                                width = if (isPicked) 1.5.dp else 1.dp,
                                color = if (isPicked) Color(0xFFFFD580) else Color.White.copy(alpha = 0.14f)
                            ),
                            modifier = Modifier
                                .width(152.dp)
                                .testTag("mixer_bgm_${track.id}")
                        ) {
                            Column(
                                modifier = Modifier.padding(12.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text(
                                    text = "${track.emoji} ${track.labelHi}",
                                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                                    color = Color.White,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = track.subtitleHi,
                                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                                    color = Color.White.copy(alpha = 0.68f),
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }
            }

            // Section 3: Voice Engine Status & Local Phone TTS Toggle
            when (voiceState) {
                is ShayarVoicePlaybackState.Speaking -> {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = Color(0xFF1A3324).copy(alpha = 0.72f),
                        border = BorderStroke(0.8.dp, Color(0xFF69F0AE).copy(alpha = 0.45f)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("mixer_gemini_active_row")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 9.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.AutoAwesome,
                                contentDescription = null,
                                tint = Color(0xFF69F0AE),
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = "इमोशनल ह्यूमन शायर आवाज़ सक्रिय ✨",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold
                                ),
                                color = Color(0xFFB9F6CA)
                            )
                        }
                    }
                }
                is ShayarVoicePlaybackState.QuotaFallback,
                is ShayarVoicePlaybackState.Error -> {
                    val statusDetail = when (voiceState) {
                        is ShayarVoicePlaybackState.QuotaFallback -> voiceState.detail
                        is ShayarVoicePlaybackState.Error -> voiceState.message
                        else -> ""
                    }
                    if (statusDetail.isNotBlank()) {
                        Surface(
                            onClick = { onRetryVoice?.invoke() },
                            shape = RoundedCornerShape(12.dp),
                            color = Color.White.copy(alpha = 0.06f),
                            border = BorderStroke(0.8.dp, Color(0xFFFFB74D).copy(alpha = 0.45f)),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("mixer_ai_voice_retry_row")
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 9.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = statusDetail,
                                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.5.sp),
                                    color = Color(0xFFFFCC80),
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.weight(1f)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "रिट्राई",
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                    color = Color(0xFFFFD580)
                                )
                            }
                        }
                    }
                }
                else -> Unit
            }

            if (onToggleLocalTtsFallback != null) {
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = Color.White.copy(alpha = 0.05f),
                    border = BorderStroke(0.8.dp, Color.White.copy(alpha = 0.12f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("mixer_local_tts_toggle_row")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            Text(
                                text = "ऑफ़लाइन फ़ोन आवाज़ (फ़ॉलबैक)",
                                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
                                color = Color.White
                            )
                            Text(
                                text = if (allowLocalTtsFallback) {
                                    "इंटरनेट न होने पर ही फ़ोन की लोकल आवाज़ बजेगी"
                                } else {
                                    "बंद: सिर्फ़ असली इमोशनल आवाज़ या बैकग्राउंड म्यूज़िक"
                                },
                                style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                                color = Color.White.copy(alpha = 0.68f)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Switch(
                            checked = allowLocalTtsFallback,
                            onCheckedChange = onToggleLocalTtsFallback,
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = Color(0xFFFF4D6D),
                                uncheckedThumbColor = Color.White.copy(alpha = 0.8f),
                                uncheckedTrackColor = Color.White.copy(alpha = 0.18f)
                            ),
                            modifier = Modifier.testTag("mixer_local_tts_switch")
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))
        }
    }
}
