package com.aistudio.meditracker.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.aistudio.meditracker.R

internal enum class UpdateUi {
    CHECKING,
    ASKING,
    NEED_PERMISSION,
    AWAITING_PERMISSION,
    DOWNLOADING,
    UP_TO_DATE,
    FAILED
}

@Composable
internal fun UpdateDialog(
    state: UpdateUi,
    release: UpdateCenter.Release?,
    progress: Float,
    onUpdate: () -> Unit,
    onLater: () -> Unit,
    onConfirmPermission: () -> Unit,
    onRetry: () -> Unit,
    onBrowser: () -> Unit,
    onReleases: () -> Unit,
    onDismiss: () -> Unit
) {
    val primary = MaterialTheme.colorScheme.primary
    val onPrimary = MaterialTheme.colorScheme.onPrimary
    // Never let the card grow past ~78% of the screen: on small phones a
    // long changelog would otherwise push the buttons off-screen.
    val maxCardHeight = LocalConfiguration.current.screenHeightDp.dp * 0.78f

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.62f))

            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) { onDismiss() }
            .systemBarsPadding(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = maxCardHeight)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) {   }
                    .clip(RoundedCornerShape(28.dp))
                    .background(
                        Brush.verticalGradient(
                            listOf(
                                Color(0xFF1C232E),
                                Color(0xFF151B24)
                            )
                        )
                    )
                    .padding(24.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(46.dp)
                            .clip(CircleShape)
                            .background(
                                Brush.linearGradient(listOf(primary, MaterialTheme.colorScheme.tertiary))
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        val headerIcon = when (state) {
                            UpdateUi.UP_TO_DATE -> Icons.Filled.Verified
                            UpdateUi.FAILED -> Icons.Filled.ErrorOutline
                            else -> Icons.Filled.SystemUpdate
                        }
                        Icon(
                            imageVector = headerIcon,
                            contentDescription = null,
                            tint = onPrimary,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        val title = when (state) {
                            UpdateUi.UP_TO_DATE -> stringResource(R.string.upd_uptodate)
                            UpdateUi.FAILED -> stringResource(R.string.upd_failed)
                            else -> stringResource(R.string.upd_title)
                        }
                        Text(
                            text = title,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color.White,
                            maxLines = 3,
                            overflow = TextOverflow.Ellipsis
                        )
                        if (release != null && state != UpdateUi.UP_TO_DATE && state != UpdateUi.FAILED) {
                            Text(
                                text = "Pill Tracker ${release.versionName ?: release.tag}",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = primary,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(10.dp))
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.size(34.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Close,
                            contentDescription = stringResource(R.string.upd_close),
                            tint = Color.White.copy(alpha = 0.65f),
                            modifier = Modifier.size(19.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                AnimatedContent(
                    targetState = state,
                    transitionSpec = {
                        (fadeIn(tween(240)) + slideInVertically(tween(240)) { it / 6 }) togetherWith
                            fadeOut(tween(160))
                    },
                    label = "updateBody"
                ) { s ->
                    // The whole body scrolls when it outgrows the card —
                    // long changelogs never get clipped, on any screen.
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .verticalScroll(rememberScrollState())
                    ) {
                        when (s) {
                            UpdateUi.CHECKING -> CheckingBody()
                            UpdateUi.ASKING -> AskingBody(release, onUpdate, onLater)
                            UpdateUi.NEED_PERMISSION -> PermissionBody(
                                onConfirmPermission, onBrowser
                            )
                            UpdateUi.AWAITING_PERMISSION -> AwaitingBody(
                                onConfirmPermission, onBrowser
                            )
                            UpdateUi.DOWNLOADING -> DownloadingBody(
                                progress, onDismiss, onReleases
                            )
                            UpdateUi.UP_TO_DATE -> UpToDateBody(onDismiss)
                            UpdateUi.FAILED -> FailedBody(onRetry, onReleases, onDismiss)
                        }
                    }
                }
            }
        }
    }
}
