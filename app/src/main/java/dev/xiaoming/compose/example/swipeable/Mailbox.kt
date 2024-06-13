package dev.xiaoming.compose.example.swipeable

import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

data class Email(
  val id: String,
  val subject: String,
  val sender: String,
  val read: Boolean = false,
  val starred: Boolean = false,
  val archived: Boolean = false,
)

fun interface EmailUpdater {
  fun updateEmail(id: String, block: Email.() -> Email)
}

@Stable
class Mailbox(private val data: List<Email>) : EmailUpdater {
  private val state = mutableStateOf(data)

  var emails by state
    private set

  override fun updateEmail(id: String, block: Email.() -> Email) {
    emails = emails.map {
      if (it.id == id) it.block() else it
    }
  }

  fun reset() {
    emails = data
  }
}
