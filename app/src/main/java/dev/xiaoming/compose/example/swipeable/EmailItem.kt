package dev.xiaoming.compose.example.swipeable

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight

@Composable
fun EmailItem(
  email: Email,
  emailUpdater: EmailUpdater,
  modifier: Modifier = Modifier,
) {
  ListItem(
    headlineContent = {
      Text(
        text = email.subject,
        style = MaterialTheme.typography.titleMedium,
        color = LocalContentColor.current.let {
          if (email.read) it.copy(alpha = 0.6f) else it
        }
      )
    },
    supportingContent = {
      Text(
        text = email.sender,
        style = MaterialTheme.typography.labelMedium,
        fontWeight = FontWeight.Medium,
        color = LocalContentColor.current.copy(alpha = 0.8f)
      )
    },
    leadingContent = {
      IconButton(
        onClick = {
          emailUpdater.updateEmail(id = email.id) { copy(starred = !starred) }
        }
      ) {
        Icon(
          imageVector = if (email.starred) Icons.Filled.Star else Icons.Filled.StarBorder,
          contentDescription = if (email.starred) "Un-star Email" else "Star Email"
        )
      }
    },
    modifier = modifier,
  )
}