package dev.xiaoming.compose.example.speedcontrol

import androidx.compose.animation.core.DecayAnimationSpec
import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.tween
import androidx.compose.animation.rememberSplineBasedDecay
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.gestures.AnchoredDraggableState
import androidx.compose.foundation.gestures.DraggableAnchors
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.anchoredDraggable
import androidx.compose.foundation.gestures.animateTo
import androidx.compose.foundation.gestures.snapTo
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.progressSemantics
import androidx.compose.material3.Button
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.setProgress
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import dev.xiaoming.compose.example.ExamplePreview
import dev.xiaoming.compose.example.ui.theme.Padding
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.launch
import kotlin.math.absoluteValue
import kotlin.math.roundToInt

@OptIn(ExperimentalFoundationApi::class)
@Stable
private class SpeedControlState(
  val initialValue: Int,
  val density: Density,
  decayAnimationSpec: DecayAnimationSpec<Float>,
  snapAnimationSpec: FiniteAnimationSpec<Float>
) {
  var speed by mutableIntStateOf(initialValue)
  val valueRange = 8..30
  val lineGap = 48.dp
  val lineGapPx = with(density) { lineGap.toPx() }
  val lineWidth = 6.dp
  val lineWidthPx = with(density) { lineWidth.toPx() }

  val draggableState =
    AnchoredDraggableState(
      initialValue = initialValue,
      positionalThreshold = { totalDistance -> totalDistance * 0.1f },
      velocityThreshold = { with(density) { 100.dp.toPx() } },
      decayAnimationSpec = decayAnimationSpec,
      snapAnimationSpec = snapAnimationSpec,
    )


  val currentValue by derivedStateOf {
    with(draggableState) {
      currentValue + (targetValue - currentValue) * progress(
        targetValue,
        currentValue
      )
    }
  }

  fun updateAnchors(size: IntSize) {
    draggableState.updateAnchors(
      DraggableAnchors {
        valueRange.forEach { value ->
          val offset =
            size.width / 2f - lineGapPx * (value - valueRange.first)
          value at offset
        }
      }
    )
  }


  suspend fun animateTo(value: Int) {
    draggableState.animateTo(value)
    speed = value
  }

  companion object {
    fun Saver(
      snapAnimationSpec: FiniteAnimationSpec<Float>,
      decayAnimationSpec: DecayAnimationSpec<Float>
    ): Saver<SpeedControlState, *> = listSaver(
      save = {
        listOf(
          it.speed.toFloat(),
          it.density.density,
          it.density.fontScale
        )
      },
      restore = {
        val initialValue = it[0].toInt()
        SpeedControlState(
          initialValue = initialValue,
          density = Density(it[1], it[2]),
          snapAnimationSpec = snapAnimationSpec,
          decayAnimationSpec = decayAnimationSpec
        )
      }
    )
  }
}

@Composable
private fun rememberSpeedControlState(
  initialValue: Int,
  density: Density,
  snapAnimationSpec: FiniteAnimationSpec<Float> = tween(),
  decayAnimationSpec: DecayAnimationSpec<Float> = rememberSplineBasedDecay(),
): SpeedControlState {
  return rememberSaveable(
    saver = SpeedControlState.Saver(
      snapAnimationSpec = snapAnimationSpec,
      decayAnimationSpec = decayAnimationSpec
    )
  ) {
    SpeedControlState(
      initialValue = initialValue,
      density = density,
      snapAnimationSpec = snapAnimationSpec,
      decayAnimationSpec = decayAnimationSpec
    )
  }
}

@Composable
fun SpeedControl(modifier: Modifier = Modifier) {
  val coroutineScope = rememberCoroutineScope()
  val state =
    rememberSpeedControlState(initialValue = 10, density = LocalDensity.current)
  Column(
    modifier = modifier,
    horizontalAlignment = Alignment.CenterHorizontally,
    verticalArrangement = Arrangement.spacedBy(Padding.large),
  ) {
    SpeedDial(state = state)
    val presets = listOf(8, 10, 16, 20, 30)
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.SpaceAround
    ) {
      presets.forEach { value ->
        Button(
          onClick = {
            coroutineScope.launch {
              state.animateTo(value = value)
            }
          },
        ) {
          Text(
            text = formatSpeed(value),
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.semantics {
              contentDescription = "Set speed to ${value / 10f}"
            }
          )
        }
      }
    }
  }
}

@OptIn(ExperimentalFoundationApi::class, FlowPreview::class)
@Composable
private fun SpeedDial(state: SpeedControlState, modifier: Modifier = Modifier) {
  val coroutineScope = rememberCoroutineScope()
  val color = LocalContentColor.current
  val speed = state.speed
  val draggableState = state.draggableState
  val hapticFeedback = LocalHapticFeedback.current
  LaunchedEffect(draggableState) {
    val valueFlow =
      snapshotFlow { draggableState.anchors.closestAnchor(draggableState.offset) }
        .filterNotNull()

    launch {
      valueFlow.collect {
        state.speed = it
      }
    }

    launch {
      valueFlow.debounce(timeoutMillis = 100)
        .collect {
          hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
        }
    }
  }
  Column(
    modifier = modifier
      .progressSemantics(
        value = speed.toFloat(),
        valueRange = 8f..30f,
        steps = 23
      )
      .semantics {
        setProgress { targetValue ->
          val newValue = targetValue
            .roundToInt()
            .coerceIn(state.valueRange)
          if (newValue == speed) {
            false
          } else {
            coroutineScope.launch {
              draggableState.snapTo(newValue)
            }
            true
          }
        }
      }
      .clearAndSetSemantics {
        stateDescription = "speed ${speed / 10f}"
      },
    horizontalAlignment = Alignment.CenterHorizontally
  ) {
    Text(
      text = formatSpeed(speed),
      style = MaterialTheme.typography.displaySmall,
      fontWeight = FontWeight.Bold
    )
    Triangle()
    Canvas(
      modifier = Modifier
        .padding(vertical = Padding.large)
        .fillMaxWidth()
        .height(96.dp)
        .onSizeChanged { size -> state.updateAnchors(size = size) }
        .anchoredDraggable(
          state = draggableState,
          orientation = Orientation.Horizontal,
          reverseDirection = false,
        )
    ) {
      var start = 0f
      for (v in state.valueRange) {
        val distance = (v - state.currentValue).absoluteValue.coerceAtMost(4f)
        val scale = (1f - (distance / 4f)).coerceAtLeast(0.4f)
        val height = size.height * scale
        val startOffset =
          Offset(
            x = start + draggableState.requireOffset(),
            y = (size.height - height) / 2
          )
        drawLine(
          color = color.copy(alpha = scale),
          start = startOffset,
          end = startOffset.copy(y = startOffset.y + height),
          strokeWidth = state.lineWidthPx,
          cap = StrokeCap.Round,
        )
        start += state.lineGapPx
      }
    }
  }
}

private fun formatSpeed(value: Int): String =
  String.format(locale = null, format = "%.1f", value / 10f)

@Composable
private fun Triangle(
  modifier: Modifier = Modifier,
  color: Color = LocalContentColor.current
) {
  Canvas(
    modifier = modifier
      .width(20.dp)
      .aspectRatio(ratio = 1.1547f)
  ) {
    val path = Path().apply {
      moveTo(x = center.x, y = size.height)
      lineTo(x = size.width, y = 0f)
      lineTo(x = 0f, y = 0f)
      lineTo(x = center.x, y = size.height)
      close()
    }
    drawPath(path = path, color = color, style = Fill)
  }
}

@Preview(showBackground = true)
@Composable
private fun SpeedControlPreview() {
  ExamplePreview {
    SpeedControl(
      modifier = Modifier
        .fillMaxWidth()
        .padding(Padding.medium)
    )
  }
}