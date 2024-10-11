package dev.xiaoming.compose.example.dial.steps

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AddLocation
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Audiotrack
import androidx.compose.material.icons.filled.Crop
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Tag
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import dev.xiaoming.compose.example.ExamplePreview
import dev.xiaoming.compose.example.R
import kotlin.math.cos
import kotlin.math.sin

object StepOne {
    @Composable
    fun <T> DialControl(
        options: List<T>,
        optionContent: @Composable (T) -> Unit,
        modifier: Modifier = Modifier,
        dialSize: Dp = 240.dp,
        dialColor: Color = MaterialTheme.colorScheme.surfaceContainer,
    ) {
        val cutOffFraction = 0.4f
        val dialCount = options.size
        val sweep = 360f / dialCount
        // We want the first option's middle point to line up at -90degrees.
        val startDegree = -90f - sweep / 2
        Box(modifier = modifier.size(dialSize), contentAlignment = Alignment.Center) {
            Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        compositingStrategy = CompositingStrategy.Offscreen
                    }
            ) {
                drawCircle(color = dialColor)
                scale(cutOffFraction) {
                    drawCircle(color = Color.Black, blendMode = BlendMode.Clear)
                }
                var i = 0
                while (i < dialCount) {
                    rotate(startDegree + sweep * i) {
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

            }


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
                    optionContent(option)
                }
            }

            Box(modifier = Modifier.size(32.dp).background(color = dialColor, shape = CircleShape))
        }
    }
}

@Composable
@Preview
private fun StepOnePreview() {
    ExamplePreview {
        Box(modifier = Modifier.wrapContentSize()) {
            Image(
                painter = painterResource(R.drawable.image_unsplash),
                contentDescription = null,
                modifier = Modifier.matchParentSize(),
                contentScale = ContentScale.Crop
            )
            val icons = listOf(
                Icons.Filled.Add,
                Icons.Filled.Crop,
                Icons.Filled.Tag,
                Icons.Filled.Palette,
                Icons.Filled.Audiotrack,
                Icons.Filled.Flag
            )
            StepOne.DialControl(
                options = icons,
                optionContent = {
                    IconButton(onClick = {}) {
                        Icon(imageVector = it, contentDescription = null)
                    }
                },
                dialSize = 400.dp,
                modifier = Modifier
            )
        }
    }
}