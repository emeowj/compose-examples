package dev.xiaoming.compose.example.swipeable

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import dev.xiaoming.compose.example.ExamplePreview
import dev.xiaoming.compose.example.ui.theme.Padding


@Composable
fun Swipeable(modifier: Modifier = Modifier) {
  val mailbox = remember {
    Mailbox(
      data = listOf(
        Email(id = "1", subject = "Movie tonight?", sender = "Jake"),
        Email(id = "2", subject = "An update on the dog", sender = "Leah"),
        Email(id = "3", subject = "Did you find the keys?", sender = "Bob"),
        Email(
          id = "4",
          subject = "The latest news on Jetpack Compose",
          sender = "Android Developer"
        ),
      )
    )
  }
  val inbox by remember {
    derivedStateOf {
      mailbox.emails.filterNot { it.archived }
    }
  }
  LazyColumn(modifier = modifier) {
    item {
      HorizontalDivider(color = LocalContentColor.current.copy(alpha = 0.2f))
    }
    items(items = inbox, key = { it.id }) { email ->
      SwipeableEmailItem(email = email, emailUpdater = mailbox)
      HorizontalDivider(color = LocalContentColor.current.copy(alpha = 0.2f))
    }

    item {
      Button(
        onClick = { mailbox.reset() },
        modifier = Modifier
          .fillMaxWidth()
          .padding(horizontal = Padding.large, vertical = Padding.large)
      ) {
        Text(text = "Rest Mailbox")
      }
    }
  }
}

@Composable
private fun SwipeableEmailItem(
  email: Email,
  emailUpdater: EmailUpdater,
  modifier: Modifier = Modifier,
  initialSwipeToDismissValue: SwipeToDismissBoxValue = SwipeToDismissBoxValue.Settled,
) {
  val updatedEmail by rememberUpdatedState(newValue = email)
  var archived by remember { mutableStateOf(false) }
  val swipeToDismissBoxState =
    rememberSwipeToDismissBoxState(
      initialValue = initialSwipeToDismissValue,
      confirmValueChange = { direction ->
        when (direction) {
          SwipeToDismissBoxValue.StartToEnd ->
            emailUpdater.updateEmail(id = updatedEmail.id) { copy(starred = !starred) }

          SwipeToDismissBoxValue.EndToStart -> archived = true
          SwipeToDismissBoxValue.Settled -> {}
        }
        false
      }
    )
  if (archived) {
    LaunchedEffect(Unit) {
      // Mark the email as archived when the dismiss action has completed
      swipeToDismissBoxState.dismiss(direction = SwipeToDismissBoxValue.EndToStart)
      emailUpdater.updateEmail(id = updatedEmail.id) { copy(archived = true) }
    }
  }

  SwipeToDismissBox(
    state = swipeToDismissBoxState,
    backgroundContent = {
      SwipeActionBackground(
        email = email,
        archived = archived,
        dismissDirection = swipeToDismissBoxState.dismissDirection,
      ) {
        (swipeToDismissBoxState.progress * 3).coerceIn(0f, 1f)
      }
    },
    modifier = modifier,
  ) {
    EmailItem(email = email, emailUpdater = emailUpdater)
  }
}

@Composable
private fun SwipeActionBackground(
  email: Email,
  archived: Boolean,
  dismissDirection: SwipeToDismissBoxValue,
  modifier: Modifier = Modifier,
  progress: () -> Float,
) {
  val reverseLayout = LocalLayoutDirection.current == LayoutDirection.Rtl
  // Needed to fix a material3 bug for RTL layout:
  // https://issuetracker.google.com/issues/347047623
  val direction = when (dismissDirection) {
    SwipeToDismissBoxValue.StartToEnd -> if (reverseLayout) SwipeToDismissBoxValue.EndToStart else SwipeToDismissBoxValue.StartToEnd
    SwipeToDismissBoxValue.EndToStart -> if (reverseLayout) SwipeToDismissBoxValue.StartToEnd else SwipeToDismissBoxValue.EndToStart
    SwipeToDismissBoxValue.Settled -> SwipeToDismissBoxValue.Settled
  }
  val contentAlignment = when (direction) {
    SwipeToDismissBoxValue.StartToEnd -> Alignment.CenterStart
    SwipeToDismissBoxValue.EndToStart -> Alignment.CenterEnd
    SwipeToDismissBoxValue.Settled -> Alignment.Center
  }
  val color = when (direction) {
    SwipeToDismissBoxValue.StartToEnd -> MaterialTheme.colorScheme.primaryContainer
    SwipeToDismissBoxValue.EndToStart -> MaterialTheme.colorScheme.errorContainer
    SwipeToDismissBoxValue.Settled -> MaterialTheme.colorScheme.surface
  }
  val scrim = MaterialTheme.colorScheme.scrim
  Surface(color = color, modifier = modifier) {
    Box(
      modifier = Modifier
        .fillMaxSize()
        .drawWithContent {
          drawContent()
          val scrimAlpha = (1f - progress()).coerceAtMost(0.5f)
          drawRect(color = scrim.copy(alpha = scrimAlpha))
        },
      contentAlignment = contentAlignment
    ) {
      when (direction) {
        SwipeToDismissBoxValue.StartToEnd -> StarEmailAction(
          starred = email.starred,
          reverseOffset = reverseLayout,
          progress = progress,
        )

        SwipeToDismissBoxValue.EndToStart -> ArchiveEmailAction(
          archived = archived,
          reverseOffset = reverseLayout,
          progress = progress,
        )

        SwipeToDismissBoxValue.Settled -> {}
      }
    }
  }
}

@Composable
private fun StarEmailAction(
  starred: Boolean,
  reverseOffset: Boolean,
  modifier: Modifier = Modifier,
  progress: () -> Float
) {
  Text(
    text = if (starred) "Un-star Email" else "Star Email",
    color = MaterialTheme.colorScheme.onPrimaryContainer,
    style = MaterialTheme.typography.labelLarge,
    modifier = modifier
      .graphicsLayer {
        val fraction = progress()
        alpha = fraction
        translationX =
          size.width * 0.2f * (1f - fraction) * (if (reverseOffset) -1 else 1)
      }
      .padding(horizontal = Padding.medium),
  )
}

@Composable
private fun ArchiveEmailAction(
  archived: Boolean,
  reverseOffset: Boolean,
  modifier: Modifier = Modifier,
  progress: () -> Float
) {
  AnimatedVisibility(visible = !archived, enter = fadeIn(), exit = fadeOut()) {
    Text(
      text = "Archive Email",
      color = MaterialTheme.colorScheme.onPrimaryContainer,
      style = MaterialTheme.typography.labelLarge,
      modifier = modifier
        .graphicsLayer {
          val fraction = progress()
          alpha = fraction
          translationX =
            size.width * 0.2f * (1f - fraction) * (if (reverseOffset) 1 else -1)
        }
        .padding(horizontal = Padding.medium),
    )
  }
}

@Composable
@Preview
@Preview(locale = "ar")
private fun BasicSwipeablePreview() {
  ExamplePreview {
    Swipeable()
  }
}

@Composable
@Preview
@Preview(locale = "ar")
private fun SwipeActionBackgroundPreview() {
  ExamplePreview {
    Column {
      val email = Email(
        id = "1",
        subject = "Hello, compose!",
        sender = "A Dev"
      )
      listOf(
        SwipeToDismissBoxValue.StartToEnd,
        SwipeToDismissBoxValue.EndToStart
      ).forEach { value ->
        listOf(0f, 0.5f, 1f).forEach { progress ->
          SwipeActionBackground(
            email = email,
            archived = false,
            dismissDirection = value,
            progress = { progress },
            modifier = Modifier.height(64.dp)
          )
        }
      }
    }
  }
}

