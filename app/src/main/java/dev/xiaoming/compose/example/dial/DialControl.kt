package dev.xiaoming.compose.example.dial

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.VectorConverter
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitDragOrCancellation
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.awaitTouchSlopOrCancellation
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.FormatSize
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import dev.xiaoming.compose.example.ExamplePreview
import dev.xiaoming.compose.example.dial.DialControlState.Companion.calculateStartAngle
import dev.xiaoming.compose.example.ui.theme.Padding
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.zip
import kotlinx.coroutines.launch
import kotlin.math.cos
import kotlin.math.sin

data class DialControlColors(
    val dialColor: Color,
    val indicatorColor: Color,
    val selectionColor: Color,
)

object DialControlDefaults {
    @Composable
    @ReadOnlyComposable
    fun colors(
        dialColor: Color = MaterialTheme.colorScheme.surfaceContainerHigh,
        indicatorColor: Color = MaterialTheme.colorScheme.primary,
        selectionColor: Color = MaterialTheme.colorScheme.primary,
    ): DialControlColors =
        DialControlColors(
            dialColor = dialColor,
            indicatorColor = indicatorColor,
            selectionColor = selectionColor
        )

    @Composable
    fun Indicator(colors: DialControlColors, state: DialControlState<*>) {
        Box(
            modifier = Modifier
                .graphicsLayer {
                    state.indicatorOffset.value.let {
                        translationX = it.x
                        translationY = it.y
                    }
                }
                .size(state.config.indicatorSize)
                .background(color = colors.indicatorColor, shape = CircleShape)
        )
    }
}

data class DialConfig(
    val size: Dp = 240.dp,
    val indicatorSize: Dp = 24.dp,
    val persistent: Boolean = false,
    val cutoffFraction: Float = 0.4f,
    val enableHaptics: Boolean = true,
    val dialAlignment: Alignment = if (persistent) Alignment.Center else Alignment.TopStart,
)

@Composable
fun <T> rememberDialControlState(
    options: List<T>,
    onSelected: (T) -> Unit,
    density: Density = LocalDensity.current,
    config: DialConfig = DialConfig(),
    coroutineScope: CoroutineScope = rememberCoroutineScope(),
): DialControlState<T> {
    return remember(density, config, coroutineScope, onSelected) {
        DialControlState(
            initialOptions = options,
            onSelected = onSelected,
            config = config,
            density = density,
            coroutineScope = coroutineScope
        )
    }
}

@Composable
fun <T> DialControl(
    state: DialControlState<T>,
    dialContent: @Composable (T) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    colors: DialControlColors = DialControlDefaults.colors(),
    indicator: @Composable (DialControlState<T>) -> Unit = {
        DialControlDefaults.Indicator(colors, state)
    },
    content: @Composable BoxScope.(DialControlState<T>) -> Unit,
) {
    if (state.config.enableHaptics) {
        val hapticFeedback = LocalHapticFeedback.current
        LaunchedEffect(state) {
            val selection = snapshotFlow { state.selectedOption }
            selection
                .zip(selection.drop(1)) { previous, current ->
                    if (previous != current && current != null) {
                        HapticFeedbackType.LongPress
                    } else {
                        null
                    }
                }
                .filterNotNull()
                .collect {
                    hapticFeedback.performHapticFeedback(it)
                }
        }
    }

    val gestureModifier = if (enabled) {
        Modifier
            .pointerInput(state) {
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    state.onDown(position = down.position)
                    var change = awaitTouchSlopOrCancellation(pointerId = down.id) { change, _ ->
                        change.consume()
                    }
                    while (change != null && change.pressed) {
                        change = awaitDragOrCancellation(change.id)?.also {
                            if (it.pressed) {
                                state.onDrag(dragAmount = it.positionChange())
                            }
                        }
                    }
                    state.onRelease()
                }
            }
    } else {
        Modifier
    }
    Box(
        modifier = modifier
            .fillMaxSize()
            .then(gestureModifier)
    ) {
        content(state)

        val containerModifier = if (state.config.persistent) {
            Modifier
        } else {
            Modifier
                .graphicsLayer {
                    translationX = state.containerOffset.x - size.width / 2
                    translationY = state.containerOffset.y - size.height
                }
        }
        AnimatedVisibility(
            visible = state.visible || state.config.persistent,
            enter = scaleIn() + fadeIn(),
            exit = scaleOut() + fadeOut(),
            modifier = containerModifier
                .padding(Padding.large)
                .padding(state.config.indicatorSize)
                .size(state.config.size)
                .align(state.config.dialAlignment)
        ) {
            CircleDial(
                state = state,
                optionContent = dialContent,
                colors = colors,
                indicator = { indicator(state) },
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}

@Composable
private fun <T> CircleDial(
    state: DialControlState<T>,
    optionContent: @Composable (T) -> Unit,
    colors: DialControlColors,
    modifier: Modifier = Modifier,
    indicator: @Composable () -> Unit,
) {
    val scales = remember(state.options) {
        state.options.associateWith { Animatable(initialValue = 0f, Float.VectorConverter) }.toMap()
    }

    LaunchedEffect(state.selectedOption, state.options) {
        state.options.forEach { option ->
            launch {
                scales[option]?.animateTo(
                    if (option == state.selectedOption) 1f else 0f,
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioLowBouncy,
                        stiffness = Spring.StiffnessMediumLow
                    )
                )
            }
        }
    }

    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        val sweep = 360f / state.options.size
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Canvas(
                modifier = Modifier
                    .graphicsLayer {
                        compositingStrategy = CompositingStrategy.Offscreen
                    }
                    .fillMaxSize()
                    .background(color = colors.dialColor, shape = CircleShape)
            ) {
                state.options.forEachIndexed { index, option ->
                    val scale = scales[option]!!.value
                    val startAngle = calculateStartAngle(index = index, count = state.options.size)
                    scale(scale) {
                        drawArc(
                            color = colors.selectionColor,
                            startAngle = startAngle,
                            sweepAngle = sweep,
                            alpha = scale,
                            useCenter = true,
                        )
                    }
                }

                val radius = size.minDimension / 2
                state.options.indices.forEach { index ->
                    val startAngle = calculateStartAngle(index = index, count = state.options.size)
                    val radian = startAngle * Math.PI / 180
                    val x = center.x + radius * cos(radian)
                    val y = center.y + radius * sin(radian)
                    drawLine(
                        color = Color.Black,
                        start = center,
                        end = Offset(x.toFloat(), y.toFloat()),
                        strokeWidth = 4.dp.toPx(),
                        cap = StrokeCap.Round,
                        blendMode = BlendMode.DstOut
                    )
                }

                scale(scale = state.config.cutoffFraction) {
                    drawCircle(color = Color.Black, blendMode = BlendMode.DstOut)
                }
            }

            indicator()
        }

        state.options.forEachIndexed { index, option ->
            key(option) {
                Box(modifier = Modifier
                    .graphicsLayer {
                        val scale = 1f + (scales[option]!!.value).coerceAtMost(0.2f)
                        val startAngle =
                            calculateStartAngle(index = index, count = state.options.size)
                        val radians = (startAngle + sweep / 2) * Math.PI / 180
                        val radius =
                            (state.config.size.toPx() / 2) * (state.config.cutoffFraction + (1f - state.config.cutoffFraction) / 2)
                        translationX = (radius * cos(radians)).toFloat()
                        translationY = (radius * sin(radians)).toFloat()
                        scaleX = scale
                        scaleY = scale
                    }

                ) {
                    optionContent(option)
                }
            }
        }
    }
}

enum class DialRegion(val icon: ImageVector) {
    TOP(icon = Icons.Filled.ArrowUpward),
    TOP_RIGHT(icon = Icons.Filled.Add),
    BOTTOM_RIGHT(icon = Icons.Filled.FormatSize),
    BOTTOM(icon = Icons.Filled.ArrowDownward),
    BOTTOM_LEFT(icon = Icons.Filled.Code),
    TOP_LEFT(icon = Icons.Filled.Palette);
}

@Composable
fun DialControlExample(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        val state = rememberDialControlState(
            options = DialRegion.entries,
            onSelected = {
                Toast.makeText(context, it.name, Toast.LENGTH_SHORT).show()
            },
            config = DialConfig(persistent = LocalInspectionMode.current, size = 240.dp)
        )
        state.updateOptions(options = DialRegion.entries, enabledOptions = DialRegion.entries)
        DialControl(
            state = state,
            modifier = Modifier.size(400.dp),
            dialContent = { region ->
                val selected = region == state.selectedOption
                IconButton(
                    onClick = {},
                    enabled = state.isOptionEnabled(region)
                ) {
                    Icon(
                        imageVector = region.icon,
                        contentDescription = null,
                        tint = if (selected) {
                            MaterialTheme.colorScheme.onPrimary
                        } else {
                            LocalContentColor.current
                        }
                    )
                }
            }
        ) {
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .background(color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.2f))
                    .padding(Padding.large)
                    .align(Alignment.TopCenter)
            ) {
                Text(text = "selected: ${state.selectedOption}")
            }
        }
    }
}

@Preview
@Composable
fun DialControlPreview() {
    ExamplePreview {
        DialControlExample()
    }
}