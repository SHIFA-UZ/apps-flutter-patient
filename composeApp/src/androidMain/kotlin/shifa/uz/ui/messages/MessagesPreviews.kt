package shifa.uz.ui.messages

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import kotlinx.datetime.Instant
import shifa.uz.data.model.ChatContact
import shifa.uz.data.model.ChatMessage
import shifa.uz.data.model.ChatUser
import shifa.uz.data.model.MessageContent
import shifa.uz.ui.widgets.PreviewTheme

private val now = Instant.parse("2026-09-28T09:00:00Z")

private val chats =
    listOf(
        ChatContact(
            "1",
            "11",
            "Dr. Aziz Karimov",
            lastMessage = "See you on Monday",
            lastAt = "2026-09-28T08:40:00Z",
            unreadCount = 2,
        ),
        ChatContact("2", "12", "Dr. Malika Yusupova", lastMessage = "photo", lastAt = "2026-09-28T07:15:00Z"),
        ChatContact(
            "3",
            "13",
            "Dr. Bobur Rahimov",
            lastMessage = "Your results look fine.",
            lastAt = "2026-09-27T18:10:00Z",
        ),
        ChatContact("4", "14", "Dr. Nodira Alieva", lastAt = "2026-09-20T10:00:00Z"),
    )

private val messages =
    listOf(
        ChatMessage(
            "1",
            rawType = "system",
            content = MessageContent("Chat started"),
            created = "2026-09-28T08:00:00Z",
        ),
        ChatMessage(
            "2",
            text = "Hello doctor, my blood test results are ready.",
            created = "2026-09-28T08:01:00Z",
            isMine = true,
        ),
        ChatMessage(
            "3",
            attachmentUrl = "https://f/results.pdf",
            attachmentName = "results.pdf",
            created = "2026-09-28T08:02:00Z",
            isMine = true,
        ),
        ChatMessage("4", text = "Thanks, I'll look at them today.", created = "2026-09-28T08:10:00Z"),
        ChatMessage(
            "5",
            rawType = "voice",
            content = MessageContent(fileUrl = "https://f/voice.m4a", duration = 42.0),
            created = "2026-09-28T08:20:00Z",
        ),
        ChatMessage("6", text = "Your results look fine. See you on Monday", created = "2026-09-28T08:40:00Z"),
    )

@Preview(widthDp = 393, heightDp = 852)
@Composable
internal fun MessagesListPreview() =
    PreviewTheme {
        MessagesContent(MessagesUiState(chats), notificationCount = 3, now = now)
    }

@Preview(widthDp = 393, heightDp = 852)
@Composable
internal fun MessagesEmptyPreview() = PreviewTheme { MessagesContent(MessagesUiState(emptyList()), now = now) }

@Preview(widthDp = 393, heightDp = 852)
@Composable
internal fun MessagesSearchPreview() =
    PreviewTheme {
        MessagesContent(
            MessagesUiState(
                chats,
                query = "Kar",
                results = listOf(ChatUser("11", "Dr. Aziz Karimov"), ChatUser("15", "Dr. Kamola Karimova")),
            ),
            now = now,
        )
    }

@Preview(widthDp = 393, heightDp = 852)
@Composable
internal fun ConversationPreview() =
    PreviewTheme {
        ConversationContent(
            ConversationUiState("Dr. Aziz Karimov", conversationId = "1", messages = messages, input = "Thank you!"),
            now = now,
        )
    }

@Preview(widthDp = 393, heightDp = 852)
@Composable
internal fun ConversationRecordingPreview() =
    PreviewTheme {
        ConversationContent(
            ConversationUiState("Dr. Aziz Karimov", conversationId = "1", messages = messages, recording = true),
            now = now,
        )
    }

@Preview(widthDp = 393, heightDp = 852)
@Composable
internal fun ConversationNewPreview() =
    PreviewTheme {
        ConversationContent(ConversationUiState("Dr. Kamola Karimova", doctorId = "15", messages = emptyList()))
    }
