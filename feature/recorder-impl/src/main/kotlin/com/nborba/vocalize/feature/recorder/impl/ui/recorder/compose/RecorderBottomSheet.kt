package com.nborba.vocalize.feature.recorder.impl.ui.recorder.compose

import android.widget.Toast
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.nborba.vocalize.core.common.util.DefaultEffectHandler
import com.nborba.vocalize.core.designsystem.component.VocalizeCircularButton
import com.nborba.vocalize.core.designsystem.component.VocalizeFilledIconButton
import com.nborba.vocalize.core.designsystem.icon.VocalizeIcons
import com.nborba.vocalize.core.designsystem.theme.spacing
import com.nborba.vocalize.core.designsystem.util.defaultPadding
import com.nborba.vocalize.core.permission.host.PermissionPromptHost
import com.nborba.vocalize.core.permission.host.rememberPermissionPromptHostState
import com.nborba.vocalize.feature.recorder.impl.ui.recorder.RecorderBottomSheetViewModel
import com.nborba.vocalize.feature.recorder.impl.ui.recorder.model.RecorderEffect
import com.nborba.vocalize.feature.recorder.impl.ui.recorder.model.RecorderState
import com.nborba.vocalize.feature.recorder.impl.ui.recorder.model.RecorderUiState
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun RecorderBottomSheet(
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: RecorderBottomSheetViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val permissionHostState = rememberPermissionPromptHostState()

    val sheetState =
        rememberModalBottomSheetState(
            skipPartiallyExpanded = true,
        )

    EffectHandler(
        effectFlow = viewModel.effects,
        onRequestPermission = { permissions ->
            scope.launch {
                val result = permissionHostState.requestPermissions(permissions)
                viewModel.onPermissionsRequestResult(result)
            }
        },
        onShowToast = { message ->
            Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
        },
        onDismissRequest = onDismissRequest,
        onEffectConsumed = viewModel::onEffectConsumed,
    )

    ModalBottomSheet(
        sheetState = sheetState,
        onDismissRequest = viewModel::onDismissRequest,
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
    ) {
        RecorderContent(
            uiState = uiState,
            onMainButtonClick = viewModel::onMainButtonClick,
            onStopButtonClick = viewModel::onStopButtonClick,
        )
    }

    PermissionPromptHost(hostState = permissionHostState)
}

@Composable
private fun EffectHandler(
    effectFlow: Flow<RecorderEffect?>,
    onRequestPermission: (List<String>) -> Unit,
    onShowToast: (String) -> Unit,
    onEffectConsumed: () -> Unit,
    onDismissRequest: () -> Unit,
) {
    DefaultEffectHandler(
        effectFlow = effectFlow,
        onEffect = { effect ->
            when (effect) {
                is RecorderEffect.RequestPermission -> onRequestPermission(effect.permissions)
                is RecorderEffect.ShowToast -> onShowToast(effect.message)
                is RecorderEffect.Dismiss -> {
                    effect.message?.let { onShowToast(it) }
                    onDismissRequest()
                }
            }
        },
        onConsumeEffect = onEffectConsumed,
    )
}

const val RECORDER_MAIN_BUTTON_TEST_TAG = "recorder_main_button"
const val RECORDER_STOP_BUTTON_TEST_TAG = "recorder_stop_button"

@Composable
internal fun RecorderContent(
    uiState: RecorderUiState,
    onMainButtonClick: () -> Unit,
    onStopButtonClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier =
            modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .defaultPadding()
                .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.small),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        if (uiState.state != RecorderState.Idle) {
            PulsingText(
                text = uiState.state.name,
                isPulsing = uiState.state == RecorderState.Recording,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            Text(
                text = uiState.formattedDuration,
                //    isPulsing = uiState.state == RecorderState.Recording,
                style = MaterialTheme.typography.displayMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )

            val barColor =
                if (uiState.state == RecorderState.Recording) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
                }

            AudioWaveform(
                waveform = uiState.audioWaveform,
                barColor = barColor,
                modifier = Modifier.padding(horizontal = MaterialTheme.spacing.medium),
            )
        }

        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .animateContentSize(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier.weight(1f),
                contentAlignment = Alignment.CenterEnd,
            ) {
                if (uiState.state != RecorderState.Idle) {
                    VocalizeFilledIconButton(
                        icon = VocalizeIcons.Stop,
                        onClick = onStopButtonClick,
                        modifier =
                            Modifier
                                .padding(end = MaterialTheme.spacing.small)
                                .testTag(RECORDER_STOP_BUTTON_TEST_TAG),
                    )
                }
            }

            VocalizeCircularButton(
                icon = uiState.mainButtonIcon,
                onClick = onMainButtonClick,
                modifier = Modifier.testTag(RECORDER_MAIN_BUTTON_TEST_TAG),
            )

            Box(modifier = Modifier.weight(1f))
        }
    }
}

@Composable
private fun AudioWaveform(
    waveform: List<Float>,
    modifier: Modifier = Modifier,
    barColor: Color = MaterialTheme.colorScheme.primary,
) {
    Canvas(
        modifier =
            modifier
                .fillMaxWidth()
                .height(64.dp),
    ) {
        if (waveform.isEmpty()) return@Canvas

        val barWidthPx = 4.dp.toPx()
        val gapPx = 3.dp.toPx()
        val maxBarHeightPx = size.height
        val minBarHeightPx = 4.dp.toPx()

        val totalBarWidth = barWidthPx + gapPx
        val maxBarsCount = (size.width / totalBarWidth).toInt()
        val samplesToDraw = waveform.takeLast(maxBarsCount)

        val startX = (size.width - (samplesToDraw.size * totalBarWidth)) / 2f

        samplesToDraw.forEachIndexed { index, amplitude ->
            val barHeightPx = (amplitude * maxBarHeightPx).coerceAtLeast(minBarHeightPx)
            val x = startX + (index * totalBarWidth) + (barWidthPx / 2f)
            val top = (size.height - barHeightPx) / 2f
            val bottom = top + barHeightPx

            drawLine(
                color = barColor,
                start = Offset(x, top),
                end = Offset(x, bottom),
                strokeWidth = barWidthPx,
                cap = StrokeCap.Round,
            )
        }
    }
}

@Composable
private fun PulsingText(
    text: String,
    style: TextStyle,
    color: Color,
    isPulsing: Boolean,
    modifier: Modifier = Modifier,
) {
    val alpha by if (isPulsing) {
        val infiniteTransition = rememberInfiniteTransition("PulsingTextTransition")
        infiniteTransition.animateFloat(
            initialValue = 1.0f,
            targetValue = 0.3f,
            animationSpec =
                infiniteRepeatable(
                    animation = tween(durationMillis = 1500, easing = LinearEasing),
                    repeatMode = RepeatMode.Reverse,
                ),
            label = "TimerAlphaAnimation",
        )
    } else {
        remember { mutableFloatStateOf(1.0f) }
    }

    Text(
        text = text,
        style = style,
        color = color,
        modifier = modifier.graphicsLayer { this.alpha = alpha },
    )
}

@Preview
@Composable
private fun RecorderIdleContentPreview() {
    RecorderContent(
        uiState = RecorderUiState(),
        onMainButtonClick = {},
        onStopButtonClick = {},
    )
}

@Preview
@Composable
private fun RecorderRecordingContentPreview() {
    RecorderContent(
        uiState =
            RecorderUiState(
                state = RecorderState.Recording,
                durationMillis = 125000L,
                audioWaveform = listOf(0.1f, 0.4f, 0.8f, 0.3f, 0.6f, 0.9f, 0.2f),
            ),
        onMainButtonClick = {},
        onStopButtonClick = {},
    )
}

@Preview
@Composable
private fun RecorderPausedContentPreview() {
    RecorderContent(
        uiState =
            RecorderUiState(
                state = RecorderState.Paused,
                durationMillis = 125000L,
                audioWaveform = listOf(0.1f, 0.4f, 0.8f, 0.3f, 0.6f, 0.9f, 0.2f),
            ),
        onMainButtonClick = {},
        onStopButtonClick = {},
    )
}
