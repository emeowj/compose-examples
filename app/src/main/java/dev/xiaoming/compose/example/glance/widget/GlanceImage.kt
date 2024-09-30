package dev.xiaoming.compose.example.glance.widget

import android.os.Build
import android.util.Log
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.core.content.res.ResourcesCompat
import androidx.core.graphics.drawable.toBitmapOrNull
import androidx.glance.GlanceModifier
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.LocalContext
import coil.imageLoader
import coil.request.ErrorResult
import coil.request.ImageRequest
import coil.request.SuccessResult
import coil.transform.RoundedCornersTransformation
import dev.xiaoming.compose.example.R

@Composable
fun GlanceImage(
    src: String,
    modifier: GlanceModifier = GlanceModifier,
    cornerRadius: Dp = 0.dp,
    contentDescription: String? = null
) {
    val context = LocalContext.current
    val isPreview = LocalInspectionMode.current
    var image by remember(src) {
        val placeholder = if (isPreview) {
            ResourcesCompat.getDrawable(
                context.resources,
                R.drawable.image_placeholder,
                null
            )?.toBitmapOrNull()
        } else {
            null
        }
        mutableStateOf(placeholder)
    }

    // Skip loading the image if we're in a preview, otherwise the preview rendering will
    // block forever waiting for the image to load.
    if (!isPreview) {
        LaunchedEffect(src, cornerRadius) {
            val request = ImageRequest.Builder(context)
                .data(src)
                .apply {
                    if (cornerRadius > 0.dp && Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
                        transformations(
                            RoundedCornersTransformation(radius = with(Density(context)) { cornerRadius.toPx() })
                        )
                    }
                }
                .build()
            when (val result = context.imageLoader.execute(request)) {
                is SuccessResult -> {
                    image = result.drawable.toBitmapOrNull()
                }

                is ErrorResult -> {
                    Log.e("GlanceImage", "Error loading image $src", result.throwable)
                }
            }
        }
    }

    RoundedCornerBox(modifier = modifier, cornerRadius = cornerRadius) {
        image?.let {
            Image(provider = ImageProvider(it), contentDescription = contentDescription)
        }
    }
}