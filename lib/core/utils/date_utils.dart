/// Parses ISO-8601 date-time strings from the API (UTC, e.g. 2026-02-12T13:00:00Z).
/// Returns the instant in local time so the patient always sees their device timezone.
/// Handles optional [ZoneId] suffix (e.g. [Asia/Tashkent]) by stripping it for Dart parse.
DateTime parseAppointmentDateTime(String value) {
  if (value.isEmpty) throw FormatException('Empty date string');
  final trimmed = value.trim();
  final withoutZoneId = trimmed.replaceFirst(RegExp(r'\[[^\]]*\]$'), '').trim();
  return DateTime.parse(withoutZoneId).toLocal();
}

/// Returns true if current time (UTC) is within the video join window used by the backend:
/// from 5 minutes before [startAt] until 15 minutes after [endAt].
/// [startAt] and [endAt] are ISO 8601 UTC strings (e.g. 2026-02-12T13:00:00Z).
bool isWithinVideoJoinWindow(String startAt, String endAt) {
  final now = DateTime.now().toUtc();
  final start = DateTime.parse(startAt);
  final end = DateTime.parse(endAt);
  final windowOpen = start.subtract(const Duration(minutes: 5));
  final windowClose = end.add(const Duration(minutes: 15));
  return !now.isBefore(windowOpen) && !now.isAfter(windowClose);
}
