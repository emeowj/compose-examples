@file:OptIn(ExperimentalMaterial3Api::class)

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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import dev.xiaoming.compose.example.dial.DialControlExample
import dev.xiaoming.compose.example.glance.GlanceWidget
import dev.xiaoming.compose.example.speedcontrol.SpeedControl
import dev.xiaoming.compose.example.swipeable.Swipeable
import dev.xiaoming.compose.example.ui.theme.ComposeexamplesTheme
import dev.xiaoming.compose.example.ui.theme.Padding

enum class ExampleItem(val title: String, val composable: @Composable () -> Unit) {
    SPEED_CONTROL(
        title = "Speed Control",
        composable = {
            SpeedControl(modifier = Modifier.padding(vertical = Padding.large))
        }
    ),
    SWIPEABLE(
        title = "Swipeable",
        composable = {
            Swipeable()
        }
    ),
    GLANCE(
        title = "Glance Widget",
        composable = {
            GlanceWidget()
        }
    ),
    DIAL(
        title = "Dial",
        composable = {
            DialControlExample()
        }
    )
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
                        ExampleScreen(item = item!!) {
                            navController.navigateUp()
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MainScreen(navHostController: NavHostController) {
    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(text = "Compose Examples", style = MaterialTheme.typography.titleMedium)
                }
            )
        }
    ) { innerPadding ->
        LazyColumn(contentPadding = innerPadding) {
            item {
                HorizontalDivider()
            }
            items(items = ExampleItem.entries) { item ->
                ListItem(
                    headlineContent = {
                        Text(text = item.title, style = MaterialTheme.typography.titleMedium)
                    },
                    modifier = Modifier.clickable {
                        navHostController.navigate(route = "example/$item")
                    }
                )
                HorizontalDivider()
            }
        }
    }
}

@Composable
private fun ExampleScreen(item: ExampleItem, navBack: () -> Unit) {
    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(item.title, style = MaterialTheme.typography.titleMedium)
                },
                navigationIcon = {
                    IconButton(onClick = navBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                            contentDescription = "Nav Back"
                        )
                    }
                }
            )
        }
    ) { contentPadding ->
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

@Preview(showBackground = true)
@Composable
private fun ExampleScreenPreview() {
    ComposeexamplesTheme {
        ExampleScreen(ExampleItem.SPEED_CONTROL) { }
    }
}
