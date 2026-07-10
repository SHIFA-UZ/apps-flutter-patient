import 'package:intl/intl.dart';
import 'package:shifa_patient_app_v1/core/models/doctor_model.dart';
import 'package:shifa_patient_app_v1/features/bookings/data/schedule_repository.dart';

/// Short-lived cache so scrolling/filter tweaks do not re-hit the schedule API.
class _AvailabilityCache {
  _AvailabilityCache._();
  static final _AvailabilityCache instance = _AvailabilityCache._();

  final _entries = <String, _CacheEntry>{};
  static const _ttl = Duration(minutes: 5);

  String? get(String doctorId) {
    final entry = _entries[doctorId];
    if (entry == null) return null;
    if (DateTime.now().difference(entry.storedAt) > _ttl) {
      _entries.remove(doctorId);
      return null;
    }
    return entry.slotStartAt;
  }

  void put(String doctorId, String slotStartAt) {
    _entries[doctorId] = _CacheEntry(slotStartAt, DateTime.now());
  }
}

class _CacheEntry {
  final String slotStartAt;
  final DateTime storedAt;

  _CacheEntry(this.slotStartAt, this.storedAt);
}

/// Lightweight fallback when the doctors list API does not include availability.
/// Kept intentionally small: low concurrency, short lookahead, capped doctor count.
class DoctorAvailabilityEnricher {
  final ScheduleRepository _scheduleRepository;

  DoctorAvailabilityEnricher(this._scheduleRepository);

  /// Returns true when the list response already carries server-side availability.
  static bool serverProvidesAvailability(List<DoctorModel> doctors) {
    if (doctors.isEmpty) return false;
    return doctors
        .take(5)
        .any((d) => d.nextAvailableStartAt != null && d.nextAvailableStartAt!.isNotEmpty);
  }

  Future<List<DoctorModel>> enrichMissingAvailability(
    List<DoctorModel> doctors, {
    int? maxDoctors,
    int concurrency = 2,
    int lookaheadDays = 14,
  }) async {
    final limit = maxDoctors ?? doctors.length;
    final targets = doctors
        .where((d) => d.nextAvailableStartAt == null || d.nextAvailableStartAt!.isEmpty)
        .take(limit)
        .toList();

    if (targets.isEmpty) return doctors;

    final cache = _AvailabilityCache.instance;
    final slotById = <String, String>{};

    for (var i = 0; i < targets.length; i += concurrency) {
      final batch = targets.skip(i).take(concurrency).toList();
      await Future.wait(
        batch.map((doctor) async {
          final cached = cache.get(doctor.id);
          if (cached != null) {
            slotById[doctor.id] = cached;
            return;
          }
          final next = await _findNextSlot(doctor.id, lookaheadDays: lookaheadDays);
          if (next != null) {
            cache.put(doctor.id, next);
            slotById[doctor.id] = next;
          }
        }),
      );
    }

    if (slotById.isEmpty) return doctors;

    return doctors
        .map(
          (d) => slotById.containsKey(d.id)
              ? d.copyWith(nextAvailableStartAt: slotById[d.id])
              : d,
        )
        .toList();
  }

  Future<String?> _findNextSlot(String doctorId, {required int lookaheadDays}) async {
    final now = DateTime.now();
    final today = DateTime(now.year, now.month, now.day);

    for (var d = 0; d < lookaheadDays; d++) {
      final date = today.add(Duration(days: d));
      final dayStr = DateFormat('yyyy-MM-dd').format(date);
      try {
        final slots = await _scheduleRepository.getAvailableSlots(
          doctorId: doctorId,
          day: dayStr,
        );
        for (final slot in slots) {
          try {
            final start = DateTime.parse(slot.startAt).toLocal();
            if (d == 0 && !start.isAfter(now)) continue;
            return slot.startAt;
          } catch (_) {
            continue;
          }
        }
      } catch (_) {
        continue;
      }
    }
    return null;
  }
}
