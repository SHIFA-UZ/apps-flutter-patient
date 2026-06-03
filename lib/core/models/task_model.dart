// lib/core/models/task_model.dart
import 'package:flutter/material.dart';

enum TaskCategory {
  vital,
  exercise,
  medication,
  other,
}

enum TaskStatus {
  draft,
  active,
  completed,
  expired,
  cancelled,
}

enum TaskInputType {
  numeric,
  text,
  boolean,
}

enum CheckInStatus {
  pending,
  completed,
  missed,
}

class RemoteCareTask {
  final int id;
  final String taskName;
  final String? description;
  final TaskCategory category;
  final TaskStatus status;
  final int timesPerDay;
  final TimeOfDay? morningTime;
  final TimeOfDay? afternoonTime;
  final TimeOfDay? eveningTime;
  final DateTime startDate;
  final DateTime? endDate;
  final int? durationDays;
  final TaskInputType inputType;
  final String? inputLabel;
  final bool notesRequired;
  final String? notesLabel;
  final DateTime createdAt;
  final TaskProgress? progress;

  RemoteCareTask({
    required this.id,
    required this.taskName,
    this.description,
    required this.category,
    required this.status,
    required this.timesPerDay,
    this.morningTime,
    this.afternoonTime,
    this.eveningTime,
    required this.startDate,
    this.endDate,
    this.durationDays,
    required this.inputType,
    this.inputLabel,
    required this.notesRequired,
    this.notesLabel,
    required this.createdAt,
    this.progress,
  });

  factory RemoteCareTask.fromJson(Map<String, dynamic> json) {
    TimeOfDay? parseTime(String? timeStr) {
      if (timeStr == null) return null;
      final parts = timeStr.split(':');
      if (parts.length >= 2) {
        return TimeOfDay(
          hour: int.parse(parts[0]),
          minute: int.parse(parts[1]),
        );
      }
      return null;
    }

    int parseId(dynamic value) {
      if (value is int) return value;
      if (value is num) return value.toInt();
      return int.parse(value.toString());
    }

    DateTime parseDateTime(dynamic value) {
      if (value is String) return DateTime.parse(value);
      throw FormatException('Invalid date: $value');
    }

    TaskStatus parseStatus(dynamic raw) {
      if (raw == null) return TaskStatus.active;
      final s = raw.toString().toUpperCase();
      return TaskStatus.values.firstWhere(
        (e) => e.name.toUpperCase() == s,
        orElse: () => TaskStatus.active,
      );
    }

    TaskCategory parseCategory(dynamic raw) {
      if (raw == null) return TaskCategory.other;
      final s = raw.toString().toUpperCase();
      return TaskCategory.values.firstWhere(
        (e) => e.name.toUpperCase() == s,
        orElse: () => TaskCategory.other,
      );
    }

    TaskInputType parseInputType(dynamic raw) {
      if (raw == null) return TaskInputType.text;
      final s = raw.toString().toUpperCase();
      return TaskInputType.values.firstWhere(
        (e) => e.name.toUpperCase() == s,
        orElse: () => TaskInputType.text,
      );
    }

    return RemoteCareTask(
      id: parseId(json['id']),
      taskName: json['taskName'] as String? ?? '',
      description: json['description'] as String?,
      category: parseCategory(json['category']),
      status: parseStatus(json['status']),
      timesPerDay: (json['timesPerDay'] as num?)?.toInt() ?? 1,
      morningTime: parseTime(json['morningTime'] as String?),
      afternoonTime: parseTime(json['afternoonTime'] as String?),
      eveningTime: parseTime(json['eveningTime'] as String?),
      startDate: parseDateTime(json['startDate']),
      endDate: json['endDate'] != null ? parseDateTime(json['endDate']) : null,
      durationDays: (json['durationDays'] as num?)?.toInt(),
      inputType: parseInputType(json['inputType']),
      inputLabel: json['inputLabel'] as String?,
      notesRequired: json['notesRequired'] as bool? ?? false,
      notesLabel: json['notesLabel'] as String?,
      createdAt: parseDateTime(json['createdAt']),
      progress: json['progress'] != null
          ? TaskProgress.fromJson(json['progress'] as Map<String, dynamic>)
          : null,
    );
  }
}

class TaskProgress {
  final int totalCheckIns;
  final int completedCheckIns;
  final int pendingCheckIns;
  final int missedCheckIns;

  TaskProgress({
    required this.totalCheckIns,
    required this.completedCheckIns,
    required this.pendingCheckIns,
    required this.missedCheckIns,
  });

  factory TaskProgress.fromJson(Map<String, dynamic> json) {
    return TaskProgress(
      totalCheckIns: (json['totalCheckIns'] as num?)?.toInt() ?? 0,
      completedCheckIns: (json['completedCheckIns'] as num?)?.toInt() ?? 0,
      pendingCheckIns: (json['pendingCheckIns'] as num?)?.toInt() ?? 0,
      missedCheckIns: (json['missedCheckIns'] as num?)?.toInt() ?? 0,
    );
  }

  double get completionPercentage {
    if (totalCheckIns == 0) return 0.0;
    return completedCheckIns / totalCheckIns;
  }
}

class TaskCheckIn {
  final int id;
  final DateTime scheduledDate;
  final TimeOfDay? scheduledTime;
  final CheckInStatus status;
  final double? numericValue;
  final String? textValue;
  final bool? booleanValue;
  final String? notes;
  final DateTime? completedAt;

  /// Combined date + time for this check-in (local). Used to enforce "open within 10 min" rule.
  DateTime get scheduledAt => DateTime(
        scheduledDate.year,
        scheduledDate.month,
        scheduledDate.day,
        scheduledTime?.hour ?? 0,
        scheduledTime?.minute ?? 0,
      );

  /// True if the patient is allowed to submit this check-in: either in the past or within 10 minutes in the future.
  bool isWithinAllowedCheckInWindow(DateTime now) {
    final cutoff = now.add(const Duration(minutes: 10));
    return !scheduledAt.isAfter(cutoff);
  }

  TaskCheckIn({
    required this.id,
    required this.scheduledDate,
    this.scheduledTime,
    required this.status,
    this.numericValue,
    this.textValue,
    this.booleanValue,
    this.notes,
    this.completedAt,
  });

  factory TaskCheckIn.fromJson(Map<String, dynamic> json) {
    TimeOfDay? parseTime(String? timeStr) {
      if (timeStr == null) return null;
      final parts = timeStr.split(':');
      if (parts.length >= 2) {
        return TimeOfDay(
          hour: int.parse(parts[0]),
          minute: int.parse(parts[1]),
        );
      }
      return null;
    }

    int parseId(dynamic value) {
      if (value is int) return value;
      if (value is num) return value.toInt();
      return int.parse(value.toString());
    }

    return TaskCheckIn(
      id: parseId(json['id']),
      scheduledDate: DateTime.parse(json['scheduledDate'] as String),
      scheduledTime: parseTime(json['scheduledTime'] as String?),
      status: CheckInStatus.values.firstWhere(
        (e) => e.name.toUpperCase() == (json['status'] as String).toUpperCase(),
        orElse: () => CheckInStatus.pending,
      ),
      numericValue: json['numericValue'] != null
          ? (json['numericValue'] as num).toDouble()
          : null,
      textValue: json['textValue'] as String?,
      booleanValue: json['booleanValue'] as bool?,
      notes: json['notes'] as String?,
      completedAt: json['completedAt'] != null
          ? DateTime.parse(json['completedAt'] as String)
          : null,
    );
  }
}
