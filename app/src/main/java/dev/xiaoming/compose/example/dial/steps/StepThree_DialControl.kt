package dev.xiaoming.compose.example.dial.steps

import androidx.annotation.FloatRange
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
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitDragOrCancellation
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Audiotrack
import androidx.compose.material.icons.filled.Crop
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Tag
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.center
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.round
import dev.xiaoming.compose.example.ExamplePreview
import dev.xiaoming.compose.example.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.zip
import kotlinx.coroutines.launch
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin

object StepThree {
    @Stable
    class DialControlState<T>(
        val options: List<T>,
        val onOptionSelected: (T) -> Unit,
        val config: DialConfig,
        private val density: Density,
        private val coroutineScope: CoroutineScope,
    ) {

        var visible by mutableStateOf(false)
            private set
        var offset by mutableStateOf(Offset.Zero)
            private set

        val indicatorOffset = Animatable(
            initialValue = Offset.Zero,
            typeConverter = Offset.VectorConverter
        )

        val selectedOption: T? by derivedStateOf {
            val sizePx = with(density) { config.dialSize.toPx() }
            val radius = sizePx / 2
            val currentOffset = indicatorOffset.value
            val distance = currentOffset.getDistance()
            if (distance < radius * config.cutOffFraction) {
                null
            } else {
                val degree = (180f / Math.PI) * atan2(y = currentOffset.y, x = currentOffset.x)
                val startAngle = calculateStartAngle(options.size)
                val sweep = 360f / options.size
                val index = options.indices.firstOrNull { index ->
                    val start = startAngle + sweep * index
                    val endAngle = start + sweep
                    degree >= startAngle && degree < endAngle
                } ?: options.lastIndex
                options[index]
            }
        }

        val sectionScales = options.associateWith {
            Animatable(
                initialValue = 0f,
                typeConverter = Float.VectorConverter
            )
        }

        init {
            coroutineScope.launch {
                snapshotFlow { selectedOption }
                    .collect { selected ->
                        sectionScales.forEach { (option, scale) ->
                            launch {
                                scale.animateTo(
                                    targetValue = if (option == selected) 1f else 0f,
                                    animationSpec = spring(dampingRatio = Spring.DampingRatioLowBouncy)
                                )
                            }
                        }
                    }
            }
        }

        fun onDown(position: Offset) {
            visible = true
            offset = position
        }

        fun onDrag(dragAmount: Offset) {
            coroutineScope.launch {
                indicatorOffset.snapTo(indicatorOffset.value + dragAmount)
            }
        }

        fun onRelease() {
            visible = false
            selectedOption?.let(onOptionSelected)
            coroutineScope.launch {
                indicatorOffset.animateTo(Offset.Zero)
            }
        }
    }

    private fun Modifier.dialControlBox(state: DialControlState<*>) = pointerInput(state) {
        awaitEachGesture {
            val down = awaitFirstDown()
            state.onDown(position = down.position)
            var change = awaitDragOrCancellation(pointerId = down.id)
            while (change != null && change.pressed) {
                state.onDrag(dragAmount = change.positionChange())
                change = awaitDragOrCancellation(pointerId = change.id)
            }
            state.onRelease()
        }
    }

    @Composable
    fun <T> rememberDialState(
        options: List<T>,
        onSelected: (T) -> Unit,
        config: DialConfig = DialConfig()
    ): DialControlState<T> {
        val density = LocalDensity.current
        val coroutineScope = rememberCoroutineScope()
        return remember(options, onSelected, config, density) {
            DialControlState(
                options = options,
                onOptionSelected = onSelected,
                config = config,
                density = density,
                coroutineScope = coroutineScope
            )
        }
    }

    @Composable
    fun <T> DialControlBox(
        options: List<T>,
        optionContent: @Composable (T, Boolean) -> Unit,
        onSelected: (T) -> Unit,
        config: DialConfig = DialConfig(),
        state: DialControlState<T> = rememberDialState(
            options = options,
            onSelected = onSelected,
            config = config
        ),
        modifier: Modifier = Modifier,
        content: @Composable BoxScope.() -> Unit,
    ) {
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

        Box(modifier = modifier.dialControlBox(state)) {
            content()
            AnimatedVisibility(
                visible = state.visible,
                enter = scaleIn() + fadeIn(),
                exit = scaleOut() + fadeOut(),
                modifier = Modifier.graphicsLayer {
                    (state.offset - size.center).let {
                        translationX = it.x
                        translationY = it.y
                    }
                }
            ) {
                val dialColor: Color = MaterialTheme.colorScheme.surfaceContainer
                DialControl(
                    options = options,
                    optionContent = optionContent,
                    selectedOption = state.selectedOption,
                    sectionScale = {
                        state.sectionScales[it]?.value ?: 0f
                    },
                    config = config,
                    dialColor = dialColor
                ) {
                    Box(
                        modifier = Modifier
                            .offset {
                                state.indicatorOffset.value.round()
                            }
                            .size(config.indicatorSize)
                            .background(color = dialColor, shape = CircleShape)
                    )
                }
            }
        }
    }

    data class DialConfig(
        val dialSize: Dp = 240.dp,
        val indicatorSize: Dp = 32.dp,
        @FloatRange(from = 0.0, to = 1.0) val cutOffFraction: Float = 0.4f,
    )

    @Composable
    private fun <T> DialControl(
        options: List<T>,
        optionContent: @Composable (T, Boolean) -> Unit,
        selectedOption: T?,
        config: DialConfig,
        sectionScale: (T) -> Float,
        modifier: Modifier = Modifier,
        dialColor: Color = MaterialTheme.colorScheme.surfaceContainer,
        selectedColor: Color = MaterialTheme.colorScheme.primary,
        indicator: @Composable () -> Unit,
    ) {
        Box(modifier = modifier.size(config.dialSize), contentAlignment = Alignment.Center) {
            DialBackground(
                color = dialColor,
                selectedColor = selectedColor,
                cutOffFraction = config.cutOffFraction,
                sectionCount = options.size,
                sectionScale = { index ->
                    sectionScale(options[index])
                }
            )

            DialContent(
                options = options,
                optionContent = optionContent,
                selectedOption = selectedOption,
                cutOffFraction = config.cutOffFraction,
                dialSize = config.dialSize
            )

            indicator()
        }
    }

    @Composable
    private fun <T> DialContent(
        options: List<T>,
        optionContent: @Composable (T, Boolean) -> Unit,
        selectedOption: T?,
        cutOffFraction: Float,
        dialSize: Dp
    ) {
        val startDegree = calculateStartAngle(options.size)
        val sweep = 360f / options.size
        options.forEachIndexed { index, option ->
            Box(
                modifier = Modifier.graphicsLayer {
                    val angle = startDegree + sweep * index
                    val radians = (angle + sweep / 2) * Math.PI / 180
                    val radius =
                        (dialSize.toPx() / 2) * (cutOffFraction + (1f - cutOffFraction) / 2)
                    translationX = (radius * cos(radians)).toFloat()
                    translationY = (radius * sin(radians)).toFloat()
                }
            ) {
                optionContent(option, option == selectedOption)
            }
        }
    }

    @Composable
    private fun DialBackground(
        color: Color,
        selectedColor: Color,
        cutOffFraction: Float,
        sectionCount: Int,
        modifier: Modifier = Modifier,
        sectionScale: (Int) -> Float,
    ) {
        Canvas(
            modifier = modifier
                .fillMaxSize()
                .graphicsLayer {
                    compositingStrategy = CompositingStrategy.Offscreen
                }
        ) {
            drawCircle(color = color)
            val startDegree = calculateStartAngle(sectionCount)
            val sweep = 360f / sectionCount
            var i = 0
            while (i < sectionCount) {
                rotate(startDegree + sweep * i) {
                    scale(sectionScale(i)) {
                        drawArc(
                            color = selectedColor,
                            startAngle = 0f,
                            sweepAngle = sweep,
                            useCenter = true
                        )
                    }

                    drawLine(
                        color = Color.Black,
                        start = center,
                        end = Offset(x = size.width, y = size.height / 2),
                        strokeWidth = 6.dp.toPx(),
                        blendMode = BlendMode.Clear
                    )
                }
                i++
            }

            scale(cutOffFraction) {
                drawCircle(color = Color.Black, blendMode = BlendMode.Clear)
            }
        }
    }

    private fun calculateStartAngle(sectionCount: Int): Float {
        val sweep = 360f / sectionCount
        return -90f - sweep / 2
    }
}


@Composable
@Preview
private fun StepThreePreview() {
    ExamplePreview {
        var selectedOption by remember { mutableStateOf<String?>(null) }

        val icons = listOf(
            Icons.Filled.Add,
            Icons.Filled.Crop,
            Icons.Filled.Tag,
            Icons.Filled.Palette,
            Icons.Filled.Audiotrack,
            Icons.Filled.Flag
        )
        StepThree.DialControlBox(
            options = icons,
            optionContent = { icon, selected ->
                IconButton(onClick = {}) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = if (selected) {
                            MaterialTheme.colorScheme.onPrimary
                        } else {
                            LocalContentColor.current
                        }
                    )
                }
            },
            onSelected = {
                selectedOption = it.name
            },
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
        ) {
            Image(
                painter = painterResource(R.drawable.image_unsplash),
                contentDescription = null,
                modifier = Modifier.matchParentSize(),
                contentScale = ContentScale.Crop
            )

            Text(
                text = "$selectedOption",
                style = MaterialTheme.typography.displaySmall,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .offset(y = 24.dp)
            )
        }
    }
}