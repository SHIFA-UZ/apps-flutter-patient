import 'package:flutter/material.dart';
import 'package:shifa_patient_app_v1/core/localization/app_localizations.dart';
import 'package:shifa_patient_app_v1/core/models/doctor_model.dart';
import 'package:shifa_patient_app_v1/core/theme/app_design_system.dart';
import 'package:shifa_patient_app_v1/core/widgets/base_card.dart';
import 'package:shifa_patient_app_v1/core/widgets/user_avatar.dart';

/// Consistent card for doctor listings: BaseCard, avatar 48px, name, specialty · clinic, distance.
class DoctorCard extends StatelessWidget {
  final DoctorModel doctor;
  final VoidCallback? onTap;
  final String? distanceText;

  const DoctorCard({
    super.key,
    required this.doctor,
    this.onTap,
    this.distanceText,
  });

  @override
  Widget build(BuildContext context) {
    final l10n = AppLocalizations.of(context)!;

    return BaseCard(
      onTap: onTap,
      child: Row(
        children: [
          Column(
            children: [
              UserAvatar(
                name: doctor.fullName,
                photoUrl: doctor.photoUrl,
                radius: 24,
              ),
              if (doctor.rating != null && (doctor.reviewCount ?? 0) > 0) ...[
                const SizedBox(height: 4),
                Row(
                  mainAxisSize: MainAxisSize.min,
                  children: [
                    Icon(Icons.star, size: 12, color: Colors.amber.shade700),
                    const SizedBox(width: 2),
                    Text(
                      doctor.rating!.toStringAsFixed(1),
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
                Text(
                  doctor.fullName,
                  style: AppDesignSystem.body2.copyWith(fontWeight: FontWeight.w600),
                  maxLines: 1,
                  overflow: TextOverflow.ellipsis,
                ),
                if (doctor.profession != null && doctor.profession!.isNotEmpty)
                  Text(
                    l10n.translateProfession(doctor.profession!) +
                        (doctor.clinic != null && doctor.clinic!.isNotEmpty ? ' · ${doctor.clinic}' : ''),
                    style: AppDesignSystem.caption,
                    maxLines: 1,
                    overflow: TextOverflow.ellipsis,
                  ),
                if (distanceText != null)
                  Padding(
                    padding: const EdgeInsets.only(top: 2),
                    child: Text(distanceText!, style: AppDesignSystem.caption.copyWith(color: AppDesignSystem.textTertiary)),
                  ),
              ],
            ),
          ),
          Icon(Icons.chevron_right, color: AppDesignSystem.textTertiary),
        ],
      ),
    );
  }
}
