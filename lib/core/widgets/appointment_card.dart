import 'package:flutter/material.dart';
import 'package:intl/intl.dart';
import 'package:shifa_patient_app_v1/core/localization/app_localizations.dart';
import 'package:shifa_patient_app_v1/core/models/appointment_model.dart';
import 'package:shifa_patient_app_v1/core/utils/date_utils.dart' as app_date_utils;
import 'package:shifa_patient_app_v1/core/theme/app_design_system.dart';
import 'package:shifa_patient_app_v1/core/widgets/base_card.dart';
import 'package:shifa_patient_app_v1/core/widgets/user_avatar.dart';

final _dateFormat = DateFormat('dd MMM');
final _timeFormat = DateFormat('HH:mm');

/// Unified AppointmentCard: BaseCard, 48px avatar, doctor name, specialty · clinic, time row, status badge.
class AppointmentCard extends StatelessWidget {
  final dynamic appointment;
  final VoidCallback? onTap;
  final VoidCallback? onPayNow;

  const AppointmentCard({
    super.key,
    required this.appointment,
    this.onTap,
    this.onPayNow,
  });

  IconData _statusIcon(AppointmentStatus status) {
    switch (status) {
      case AppointmentStatus.confirmed:
        return Icons.check_circle;
      case AppointmentStatus.pending:
        return Icons.schedule;
      case AppointmentStatus.cancelled:
        return Icons.cancel;
      case AppointmentStatus.completed:
        return Icons.check_circle_outline;
    }
  }

  Color _statusColor(AppointmentStatus status) {
    switch (status) {
      case AppointmentStatus.confirmed:
        return Colors.blue;
      case AppointmentStatus.pending:
        return Colors.orange;
      case AppointmentStatus.cancelled:
        return Colors.red;
      case AppointmentStatus.completed:
        return Colors.green;
    }
  }

  @override
  Widget build(BuildContext context) {
    final l10n = AppLocalizations.of(context)!;
    AppointmentStatus status;
    try {
      status = appointment.status is AppointmentStatus
          ? appointment.status
          : AppointmentStatus.fromString(appointment.status?.toString() ?? 'PENDING');
    } catch (_) {
      status = AppointmentStatus.pending;
    }
    final isCancelled = status == AppointmentStatus.cancelled;
    final paymentPending = (appointment.paymentStatus?.toString().toUpperCase() == 'PENDING');
    final doctor = appointment.doctor;
    DateTime startDate;
    try {
      startDate = app_date_utils.parseAppointmentDateTime(appointment.startAt);
    } catch (_) {
      startDate = DateTime.now();
    }

    return BaseCard(
      onTap: isCancelled ? null : onTap,
      color: isCancelled ? AppDesignSystem.backgroundTertiary : null,
      child: Row(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Column(
            children: [
              UserAvatar(
                name: doctor?.fullName,
                photoUrl: doctor?.photoUrl,
                radius: 24,
              ),
              if (doctor?.rating != null && (doctor?.reviewCount ?? 0) > 0) ...[
                const SizedBox(height: 4),
                Row(
                  mainAxisSize: MainAxisSize.min,
                  children: [
                    Icon(Icons.star, size: 12, color: Colors.amber.shade700),
                    const SizedBox(width: 2),
                    Text(
                      doctor!.rating!.toStringAsFixed(1),
                      style: TextStyle(fontSize: 11, fontWeight: FontWeight.w600, color: Colors.amber.shade800),
                    ),
                    Text(' (${doctor.reviewCount})', style: AppDesignSystem.caption),
                  ],
                ),
              ],
            ],
          ),
          const SizedBox(width: 12),
          Expanded(
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Row(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Expanded(
                      child: Text(
                        doctor?.fullName ?? 'Unknown',
                        style: AppDesignSystem.body2.copyWith(
                          fontWeight: FontWeight.w600,
                          color: isCancelled ? AppDesignSystem.textTertiary : AppDesignSystem.textPrimary,
                        ),
                        maxLines: 1,
                        overflow: TextOverflow.ellipsis,
                      ),
                    ),
                    Container(
                      padding: const EdgeInsets.symmetric(horizontal: 6, vertical: 2),
                      decoration: BoxDecoration(
                        color: _statusColor(status).withOpacity(0.12),
                        borderRadius: BorderRadius.circular(8),
                      ),
                      child: Icon(_statusIcon(status), size: 16, color: _statusColor(status)),
                    ),
                  ],
                ),
                if (paymentPending) ...[
                  const SizedBox(height: 6),
                  Row(
                    children: [
                      Container(
                        padding: const EdgeInsets.symmetric(horizontal: 8, vertical: 3),
                        decoration: BoxDecoration(
                          color: Colors.orange.withOpacity(0.12),
                          borderRadius: BorderRadius.circular(10),
                        ),
                        child: Text(
                          l10n.translate('paymentPendingBadge'),
                          style: AppDesignSystem.caption.copyWith(
                            color: Colors.orange.shade800,
                            fontWeight: FontWeight.w700,
                          ),
                        ),
                      ),
                      const SizedBox(width: 8),
                      TextButton(
                        onPressed: onPayNow,
                        child: Text(l10n.translate('payNow')),
                      ),
                    ],
                  ),
                ],
                if (doctor?.profession != null && doctor!.profession!.isNotEmpty)
                  Text(
                    l10n.translateProfession(doctor.profession!) +
                        (doctor.clinic != null && doctor.clinic!.isNotEmpty ? ' · ${doctor.clinic}' : ''),
                    style: AppDesignSystem.caption.copyWith(color: isCancelled ? AppDesignSystem.textTertiary : AppDesignSystem.textSecondary),
                    maxLines: 1,
                    overflow: TextOverflow.ellipsis,
                  ),
                const SizedBox(height: 6),
                Row(
                  children: [
                    Icon(Icons.access_time, size: 14, color: AppDesignSystem.textSecondary),
                    const SizedBox(width: 4),
                    Text(
                      '${_timeFormat.format(startDate)}, ${_dateFormat.format(startDate)}',
                      style: AppDesignSystem.caption.copyWith(
                        color: isCancelled ? AppDesignSystem.textTertiary : AppDesignSystem.textPrimary,
                      ),
                    ),
                  ],
                ),
                if (appointment.reason != null && appointment.reason.toString().isNotEmpty)
                  Padding(
                    padding: const EdgeInsets.only(top: 2),
                    child: Text(
                      appointment.reason.toString(),
                      style: AppDesignSystem.caption.copyWith(
                        fontSize: 11,
                        color: isCancelled ? AppDesignSystem.textTertiary : AppDesignSystem.textSecondary,
                      ),
                      maxLines: 1,
                      overflow: TextOverflow.ellipsis,
                    ),
                  ),
              ],
            ),
          ),
        ],
      ),
    );
  }
}
