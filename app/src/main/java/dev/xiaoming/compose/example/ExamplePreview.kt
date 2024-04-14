package dev.xiaoming.compose.example

import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import dev.xiaoming.compose.example.ui.theme.ComposeexamplesTheme

@Composable
fun ExamplePreview(content: @Composable () -> Unit) {
  ComposeexamplesTheme {
    Surface {
      content()
    }
  }
}