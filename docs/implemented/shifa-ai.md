# Shifa AI

## Chats (`ui/shifaai/ShifaAiScreen.kt`, AI-01)

- Figma: `1655:16752`. The centre tab. The header is Home's (avatar → Profile, bell → Notifications with the badge) over the Figma gradient image.
- Each question and reply keeps when it was sent (`CopilotMessage.sentAt`, epoch ms, never sent to X1), shown under the bubble like the doctor chat's times (not in Figma). Chats saved before this have none.
- Chats live only on this device, since X1 keeps no history: `CopilotStorage` in the secure store (health data), newest first, at most 20 chats of 60 messages each. A chat is saved once it has a message. The card title is the first question, the text is the last message, and the time reads like the inbox.
- Search matches any message in a chat, ignoring case. No chats: "No conversations yet"; no match: "No messages". "New Chat" floats above the bar.
- The chats are cleared on logout (AUTH-08, fixes 00 bug 4), on the first launch after a reinstall, and when the saved session gets a 401.

## Chat (`ShifaAiChatScreen.kt`)

- Figma: `1673:14621` (chat), `1673:14790` (header), `1681:13859` (Add to chat). An overlay above the tabs. The ViewModel lives in the tab shell, so the list sees every change.
- Send posts the whole chat to X1 (`patients/me/copilot/stream`, SSE, on the shared client) with the app language (EN / RU / UZ; German sends EN). The reply streams token by token into a teal received bubble, as Figma's chat `1772:17214` (three pulsing dots before the first token, `ShifaChatThinkingBubble`; "Thinking…" went). The newest turn stays in view, and the list sits above the keyboard.
- SSE parsing follows 09 §8: a blank line ends an event, `data:` keeps its leading space, and data left at EOF counts. An empty reply isn't saved.
- Errors: a 429, a `RATE_LIMIT` code or an SSE `RATE_LIMIT` error event shows the daily-limit banner and blocks sending until the chat is reopened. Other errors show the generic Shifa AI error; the question stays in the chat, unanswered. A 401 logs out.
- Back or leaving the chat stops the reply; what streamed so far is dropped.
- ⋮ deletes the chat after a confirm (the plan's "Clear chat"). It's hidden on a new, empty chat.
- The disclaimer (09 §6 item 5) sits above the first message. It isn't in Figma.
- Gating (09 §7): the profile's `features` decide when the list is present (`PATIENT_SHIFA_AI`), else the tier (only PRO is shut out). It fails open while the profile loads. A gated user has no Shifa AI tab, and an open chat or tab closes when the profile shuts it.
- Voice (AI-02, 09 §5.7): the mic asks for the microphone (denied: "Microphone access is needed…"), then records an AAC-LC .m4a, 32 kbps, 16 kHz, mono (`utils/VoiceRecorder.kt`: Android MediaRecorder, iOS AVAudioRecorder) while the bar shows Figma's recording look with a moving waveform (Figma's is static) and a stop button on the right. Stop sends X2 (`patients/me/copilot/transcribe`, the file and `languageHint` uz / ru / en from the app language) with "Transcribing…" in the bar. The transcript is sent as the question at once (09 §5.7 had it fill the input; the user wants it sent); an empty one leaves the input alone. A failure shows "Could not transcribe audio"; a 429 sets the rate limit. The temp file is deleted after each recording, and leaving the chat drops an unfinished one. No transcription feedback (09 §2.6; Flutter's builds had it off). Not tried on a device or against the backend. Demo data answers X2 with a fixed transcript.
- Not built:
  - Add to chat's Camera / Photo / File (no copilot upload API; "Coming soon");
  - doctor suggestions, AI booking and Figma's "Suggested Message" chips (AI-03).
- Not tried against the backend.
