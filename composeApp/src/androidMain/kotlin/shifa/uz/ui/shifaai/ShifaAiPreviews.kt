package shifa.uz.ui.shifaai

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import kotlinx.datetime.Instant
import shifa.composeapp.generated.resources.Res
import shifa.composeapp.generated.resources.copilotDailyLimit
import shifa.uz.data.model.CopilotChat
import shifa.uz.data.model.CopilotMessage
import shifa.uz.data.model.CopilotMessage.Companion.ASSISTANT
import shifa.uz.data.model.CopilotMessage.Companion.USER
import shifa.uz.ui.widgets.PreviewTheme

private val now = Instant.parse("2026-09-28T09:00:00Z")

private fun at(time: String) = Instant.parse(time).toEpochMilliseconds()

private val headache =
    CopilotChat(
        "1",
        listOf(
            CopilotMessage(USER, "I've had a headache for three days, mostly in the evening. What could it be?"),
            CopilotMessage(
                ASSISTANT,
                "Evening headaches that last a few days are often tension headaches: long hours at a screen, " +
                    "poor sleep, stress or not drinking enough water can all cause them. Try regular breaks, water " +
                    "through the day and a steady sleep time. See a doctor soon if the pain is sudden and severe, " +
                    "wakes you up, or comes with fever, a stiff neck, vision changes or weakness.",
            ),
            CopilotMessage(USER, "Which doctor should I see?"),
        ),
        at("2026-09-28T08:40:00Z"),
    )

private val chats =
    listOf(
        headache,
        CopilotChat(
            "2",
            listOf(
                CopilotMessage(USER, "How often should I check my blood pressure?"),
                CopilotMessage(
                    ASSISTANT,
                    "If your readings are normal, once a year at a check-up is enough. " +
                        "With high blood pressure, check it at home every day.",
                ),
            ),
            at("2026-09-27T18:10:00Z"),
        ),
        CopilotChat(
            "3",
            listOf(
                CopilotMessage(USER, "Can I take ibuprofen with paracetamol?"),
                CopilotMessage(
                    ASSISTANT,
                    "Usually yes: they work differently and can be taken together or alternated. " +
                        "Check the doses on the pack.",
                ),
            ),
            at("2026-09-20T10:00:00Z"),
        ),
    )

@Preview(widthDp = 393, heightDp = 852)
@Composable
internal fun ShifaAiChatsPreview() = PreviewTheme { ShifaAiContent(ShifaAiUiState(chats), now = now) }

@Preview(widthDp = 393, heightDp = 852)
@Composable
internal fun ShifaAiNoChatsPreview() = PreviewTheme { ShifaAiContent(ShifaAiUiState(), now = now) }

@Preview(widthDp = 393, heightDp = 852)
@Composable
internal fun ShifaAiChatPreview() =
    PreviewTheme {
        ShifaAiChatContent(
            ShifaAiUiState(chats, chat = headache.copy(messages = headache.messages.dropLast(1))),
            onMore = {},
        )
    }

@Preview(widthDp = 393, heightDp = 852)
@Composable
internal fun ShifaAiThinkingPreview() =
    PreviewTheme {
        ShifaAiChatContent(
            ShifaAiUiState(chats, chat = headache, input = "And what tests", streaming = ""),
            onMore = {},
        )
    }

@Preview(widthDp = 393, heightDp = 852)
@Composable
internal fun ShifaAiRateLimitPreview() =
    PreviewTheme {
        ShifaAiChatContent(
            ShifaAiUiState(chats, chat = headache, error = Res.string.copilotDailyLimit, rateLimited = true),
            onMore = {},
        )
    }

@Preview(widthDp = 393, heightDp = 852)
@Composable
internal fun ShifaAiAddToChatPreview() =
    PreviewTheme {
        ShifaAiChatContent(ShifaAiUiState(chats, chat = headache), adding = true, onMore = {})
    }
