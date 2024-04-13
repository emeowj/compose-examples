package dev.xiaoming.compose.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedContentTransitionScope.SlideDirection.Companion.End
import androidx.compose.animation.AnimatedContentTransitionScope.SlideDirection.Companion.Start
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import dev.xiaoming.compose.example.speedcontrol.SpeedControl
import dev.xiaoming.compose.example.ui.theme.ComposeexamplesTheme

enum class ExampleItem(val title: String, val composable: @Composable () -> Unit) {
  SPEED_CONTROL(
    title = "Speed Control",
    composable = { SpeedControl() })
}

class MainActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()
    setContent {
      ComposeexamplesTheme {
        val navController = rememberNavController()
        NavHost(navController = navController, startDestination = "home") {
          composable(route = "home") {
            MainScreen(navController)
          }
          composable(
            route = "example/{item}",
            arguments = listOf(
              navArgument("item") {
                type = NavType.StringType
                nullable = false
              }
            ),
            enterTransition = { slideIntoContainer(towards = Start) },
            exitTransition = { slideOutOfContainer(towards = End) }
          ) { navBackStackEntry ->
            val item = navBackStackEntry.arguments?.getString("item")?.let {
              ExampleItem.valueOf(it)
            }
            ExampleScreen(item = item!!)
          }
        }
      }
    }
  }
}

@Composable
private fun MainScreen(navHostController: NavHostController) {
  Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
    LazyColumn(contentPadding = innerPadding) {
      items(items = ExampleItem.entries) { item ->
        ListItem(
          headlineContent = {
            Text(text = item.title, style = MaterialTheme.typography.titleMedium)
          },
          modifier = Modifier.clickable {
            navHostController.navigate(route = "example/$item")
          }
        )
      }
    }
  }
}

@Composable
private fun ExampleScreen(item: ExampleItem) {
  Scaffold(modifier = Modifier.fillMaxSize()) { contentPadding ->
    Box(modifier = Modifier.padding(contentPadding)) {
      item.composable()
    }
  }
}

@Preview(showBackground = true)
@Composable
private fun MainScreenPreview() {
  ComposeexamplesTheme {
    MainScreen(navHostController = rememberNavController())
  }
}