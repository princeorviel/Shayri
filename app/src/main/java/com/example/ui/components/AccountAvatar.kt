package com.example.ui.components

import android.net.Uri
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.rounded.AccountCircle
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.SubcomposeAsyncImage

@Composable
fun AccountAvatar(
    photoUrl: Uri?,
    displayName: String?,
    isSignedIn: Boolean,
    isSelected: Boolean = false,
    size: Dp = 26.dp,
    contentDescription: String? = null,
    modifier: Modifier = Modifier,
    tintOverride: Color? = null
) {
    if (!isSignedIn && photoUrl == null) {
        Icon(
            imageVector = if (isSelected) Icons.Rounded.Person else Icons.Outlined.Person,
            contentDescription = contentDescription,
            tint = tintOverride ?: if (isSelected) {
                MaterialTheme.colorScheme.primary
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant
            },
            modifier = modifier.size(size)
        )
        return
    }

    val initial = displayName?.trim()?.firstOrNull()?.uppercaseChar()?.toString()
    val borderColor = tintOverride ?: if (isSelected) {
        MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.colorScheme.outlineVariant
    }

    Surface(
        shape = CircleShape,
        color = MaterialTheme.colorScheme.primaryContainer,
        border = BorderStroke(if (isSelected) 1.8.dp else 1.dp, borderColor),
        modifier = modifier.size(size)
    ) {
        if (photoUrl != null) {
            SubcomposeAsyncImage(
                model = photoUrl,
                contentDescription = contentDescription,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxSize()
                    .clip(CircleShape),
                loading = {
                    AvatarFallback(
                        initial = initial,
                        size = size,
                        contentDescription = contentDescription
                    )
                },
                error = {
                    AvatarFallback(
                        initial = initial,
                        size = size,
                        contentDescription = contentDescription
                    )
                }
            )
        } else {
            AvatarFallback(
                initial = initial,
                size = size,
                contentDescription = contentDescription
            )
        }
    }
}

@Composable
private fun AvatarFallback(
    initial: String?,
    size: Dp,
    contentDescription: String?
) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        if (!initial.isNullOrBlank()) {
            val fontSize = (size.value * 0.45f).coerceIn(11f, 24f).sp
            Text(
                text = initial,
                style = MaterialTheme.typography.labelLarge.copy(
                    fontSize = fontSize,
                    fontWeight = FontWeight.Bold
                ),
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
        } else {
            Icon(
                imageVector = Icons.Rounded.AccountCircle,
                contentDescription = contentDescription,
                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier.fillMaxSize(0.85f)
            )
        }
    }
}
