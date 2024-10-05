package dev.xiaoming.compose.example.dial

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.VectorConverter
import androidx.compose.animation.core.spring
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitDragOrCancellation
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.AddLocation
import androidx.compose.material.icons.rounded.AddLocationAlt
import androidx.compose.material.icons.rounded.ArrowDownward
import androidx.compose.material.icons.rounded.ArrowUpward
import androidx.compose.material.icons.rounded.Link
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
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
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun <T> DialControl(
    options: List<T>,
    onSelected: (T) -> Unit,
    modifier: Modifier = Modifier,
    config: DialConfig = DialConfig(),
    content: @Composable (T) -> Unit,
) {
    require(options.isNotEmpty()) {
        "Options cannot be empty"
    }
    val coroutineScope = rememberCoroutineScope()
    val density = LocalDensity.current
    val isPreview = LocalInspectionMode.current
    var visible by remember { mutableStateOf(isPreview) }
    var offset by remember { mutableStateOf(Offset.Zero) }
    val controlOffset = remember {
        Animatable(initialValue = Offset.Zero, Offset.VectorConverter)
    }
    val selectedOption by remember(density, config) {
        derivedStateOf {
            with(density) {
                calculateSelectedOption(
                    options = options,
                    config = config,
                    offset = controlOffset.value
                )
            }
        }
    }

    val hapticFeedback = LocalHapticFeedback.current
    LaunchedEffect(Unit) {
        snapshotFlow { selectedOption }
            .distinctUntilChanged()
            .collect {
                hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
            }
    }
    Box(
        modifier = modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                awaitEachGesture {
                    val down = awaitFirstDown()
                    offset = Offset(down.position.x, down.position.y)
                    visible = true
                    var change = awaitDragOrCancellation(pointerId = down.id)
                    while (change != null && change.pressed) {
                        change = awaitDragOrCancellation(change.id)?.also {
                            if (it.pressed) {
                                val origin = controlOffset.value
                                val target = origin + it.positionChange()
                                val radius = config.size.toPx() / 2
                                val distance = target.getDistance()
                                val clamped =
                                    if (distance > radius) target * radius / distance else target
                                coroutineScope.launch {
                                    controlOffset.snapTo(clamped)
                                }
                            }
                        }
                    }
                    visible = false
                    selectedOption?.let(onSelected)
                    coroutineScope.launch {
                        controlOffset.animateTo(Offset.Zero)
                    }
                }
            },
        contentAlignment = Alignment.TopCenter
    ) {
        AnimatedVisibility(
            visible = visible,
            enter = scaleIn(),
            exit = scaleOut(),
        ) {
            CircleDial(
                options = options,
                selectedOption = selectedOption,
                selectOption = onSelected,
                optionContent = content,
                config = config,
                modifier = Modifier.padding(24.dp)
            ) {
                Box(
                    modifier = Modifier
                        .graphicsLayer {
                            controlOffset.value.let {
                                translationX = it.x
                                translationY = it.y
                            }
                        }
                        .size(24.dp)
                        .background(
                            color = MaterialTheme.colorScheme.onSurface,
                            shape = CircleShape
                        )

                )
            }
        }
    }
}

data class DialConfig(
    val size: Dp = 240.dp,
    val cutoffFraction: Float = 0.4f,
    val unselectedScale: Float = 0.8f
)

private fun calculateStartAngle(index: Int, size: Int): Float {
    val sweep = 360f / size
    return 0f + sweep * index - 90f - sweep / 2
}

private fun <T> Density.calculateSelectedOption(
    options: List<T>,
    config: DialConfig,
    offset: Offset
): T? {
    val sizePx = config.size.toPx()
    val radius = sizePx / 2
    val distance = offset.getDistance()
    return if (distance < radius * config.cutoffFraction) {
        null
    } else {
        val degree = (180f / Math.PI) * atan2(y = offset.y, x = offset.x)
        val sweep = 360f / options.size
        val index = options.indices.firstOrNull { index ->
            val startAngle = calculateStartAngle(index, options.size)
            val endAngle = startAngle + sweep
            degree >= startAngle && degree < endAngle
        } ?: options.lastIndex
        options[index]
    }
}

@Composable
private fun <T> CircleDial(
    options: List<T>,
    selectedOption: T?,
    selectOption: (T) -> Unit,
    optionContent: @Composable (T) -> Unit,
    config: DialConfig,
    modifier: Modifier = Modifier,
    background: Color = MaterialTheme.colorScheme.surfaceContainerHigh,
    cutoffColor: Color = MaterialTheme.colorScheme.surfaceContainerLowest,
    indicator: @Composable () -> Unit,
) {
    val scales = remember {
        options.associateWith {
            Animatable(
                initialValue = config.unselectedScale,
                Float.VectorConverter
            )
        }.toMap()
    }

    LaunchedEffect(selectedOption) {
        options.forEach { option ->
            launch {
                scales[option]?.animateTo(
                    if (option == selectedOption) 1f else config.unselectedScale,
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioMediumBouncy,
                        stiffness = Spring.StiffnessMedium
                    )
                )
            }
        }
    }

    Box(modifier = modifier.size(config.size), contentAlignment = Alignment.Center) {
        val sweep = 360f / options.size
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            val regionColor = MaterialTheme.colorScheme.primary
            Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    .background(color = background, shape = CircleShape)
            ) {
                options.forEachIndexed { index, option ->
                    val scale = scales[option]!!.value
                    val startAngle = calculateStartAngle(index = index, size = options.size)
                    scale(scale) {
                        drawArc(
                            color = regionColor,
                            startAngle = startAngle,
                            sweepAngle = sweep,
                            alpha = (scale - 0.8f).coerceAtLeast(0f),
                            useCenter = true,
                        )
                    }
                }

                val radius = size.minDimension / 2
                options.indices.forEach { index ->
                    val startAngle = calculateStartAngle(index = index, size = options.size)
                    val radian = startAngle * Math.PI / 180
                    val x = center.x + radius * cos(radian)
                    val y = center.y + radius * sin(radian)
                    drawLine(
                        color = cutoffColor,
                        start = center,
                        end = Offset(x.toFloat(), y.toFloat()),
                        strokeWidth = 4.dp.toPx(),
                        cap = StrokeCap.Round
                    )
                }
            }

            Canvas(modifier = Modifier.fillMaxSize(fraction = config.cutoffFraction)) {
                drawCircle(color = cutoffColor)
            }

            indicator()
        }

        options.forEachIndexed { index, option ->

            key(option) {
                Box(
                    modifier = Modifier
                        .clickable { selectOption(option) }
                        .graphicsLayer {
                            val scale = scales[option]!!.value / config.unselectedScale
                            val startAngle = calculateStartAngle(index = index, size = options.size)
                            val radians = (startAngle + sweep / 2) * Math.PI / 180
                            val radius =
                                (config.size.toPx() / 2) * (config.cutoffFraction + (1f - config.cutoffFraction) / 2)
                            translationX = (radius * cos(radians)).toFloat()
                            translationY = (radius * sin(radians)).toFloat()
                            scaleX = scale
                            scaleY = scale
                        }
                        .size(24.dp)
                ) {
                    optionContent(option)
                }
            }
        }
    }
}

enum class DialRegion(val icon: ImageVector) {
    TOP(icon = Icons.Rounded.ArrowUpward),
    TOP_RIGHT(icon = Icons.Rounded.Add),
    BOTTOM_RIGHT(icon = Icons.Rounded.Link),
    BOTTOM(icon = Icons.Rounded.ArrowDownward),
    BOTTOM_LEFT(icon = Icons.Rounded.AddLocation),
    TOP_LEFT(icon = Icons.Rounded.AddLocationAlt);
}

@Composable
fun DialControlExample(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    DialControl(
        options = DialRegion.entries,
        onSelected = {
            Toast.makeText(context, it.name, Toast.LENGTH_SHORT).show()
        },
        modifier = modifier
            .fillMaxSize()
            .background(color = MaterialTheme.colorScheme.surfaceContainerLowest),
    ) { region ->
        Icon(imageVector = region.icon, contentDescription = null)
    }
}

@Preview
@Composable
fun DialControlPreview() {
    ExamplePreview {
        DialControlExample()
    }
}
