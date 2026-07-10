import 'package:flutter/material.dart';
import 'package:shifa_patient_app_v1/core/localization/app_localizations.dart';
import 'package:shifa_patient_app_v1/core/models/doctor_model.dart';
import 'package:shifa_patient_app_v1/core/theme/app_design_system.dart';
import 'package:shifa_patient_app_v1/core/widgets/base_card.dart';
import 'package:shifa_patient_app_v1/core/widgets/shifa_button.dart';
import 'package:shifa_patient_app_v1/core/widgets/user_avatar.dart';

/// Compact-by-default discovery card; expand for clinic, price breakdown, and profile link.
class DoctorDiscoveryCard extends StatefulWidget {
  final DoctorModel doctor;
  final String? distanceText;
  final VoidCallback onBook;
  final VoidCallback onProfile;

  const DoctorDiscoveryCard({
    super.key,
    required this.doctor,
    this.distanceText,
    required this.onBook,
    required this.onProfile,
  });

  @override
  State<DoctorDiscoveryCard> createState() => _DoctorDiscoveryCardState();
}

class _DoctorDiscoveryCardState extends State<DoctorDiscoveryCard> {
  bool _expanded = false;

  String _formatPrice(int amountMinor, String currency) {
    final major = amountMinor / 100;
    final value = major == major.roundToDouble()
        ? major.toStringAsFixed(0)
        : major.toStringAsFixed(2);
    return '$value $currency';
  }

  String _formatAvailability(AppLocalizations l10n, String iso) {
    final dt = DateTime.tryParse(iso)?.toLocal();
    if (dt == null) return iso;
    return l10n.formatNextAvailableSlot(dt);
  }

  String? _locationLine(DoctorModel doctor) {
    final city = doctor.city?.trim();
    final country = doctor.locationCountry?.trim();
    if (city != null && city.isNotEmpty && country != null && country.isNotEmpty) {
      return '$city, $country';
    }
    if (city != null && city.isNotEmpty) return city;
    if (country != null && country.isNotEmpty) return country;
    if (doctor.region != null && doctor.region!.trim().isNotEmpty) {
      return doctor.region;
    }
    return null;
  }

  Widget _modeBadge(String label, Color color) {
    return Container(
      padding: const EdgeInsets.symmetric(horizontal: 8, vertical: 2),
      decoration: BoxDecoration(
        color: color.withValues(alpha: 0.12),
        borderRadius: BorderRadius.circular(999),
      ),
      child: Text(
        label,
        style: AppDesignSystem.caption.copyWith(
          color: color,
          fontWeight: FontWeight.w600,
          fontSize: 11,
        ),
      ),
    );
  }

  @override
  Widget build(BuildContext context) {
    final l10n = AppLocalizations.of(context)!;
    final doctor = widget.doctor;
    final isVerified = doctor.certificates != null && doctor.certificates!.isNotEmpty;
    final locationLine = _locationLine(doctor);
    final availability = doctor.nextAvailableStartAt;
    final startingPrice = doctor.startingPrice;

    return BaseCard(
      onTap: null,
      padding: const EdgeInsets.all(12),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          // ── Collapsed header (always visible) ──
          Row(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              Stack(
                clipBehavior: Clip.none,
                children: [
                  UserAvatar(
                    name: doctor.fullName,
                    photoUrl: doctor.photoUrl,
                    radius: 26,
                  ),
                  if (isVerified)
                    Positioned(
                      right: -2,
                      bottom: -2,
                      child: Container(
                        padding: const EdgeInsets.all(2),
                        decoration: const BoxDecoration(
                          color: AppDesignSystem.background,
                          shape: BoxShape.circle,
                        ),
                        child: Icon(
                          Icons.verified,
                          size: 16,
                          color: AppDesignSystem.primary,
                        ),
                      ),
                    ),
                ],
              ),
              const SizedBox(width: 12),
              Expanded(
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Row(
                      children: [
                        Expanded(
                          child: Text(
                            doctor.fullName,
                            style: AppDesignSystem.body2.copyWith(fontWeight: FontWeight.w700),
                            maxLines: 2,
                            overflow: TextOverflow.ellipsis,
                          ),
                        ),
                        if (doctor.rating != null && (doctor.reviewCount ?? 0) > 0) ...[
                          const SizedBox(width: 6),
                          Icon(Icons.star, size: 14, color: Colors.amber.shade700),
                          Text(
                            doctor.rating!.toStringAsFixed(1),
                            style: AppDesignSystem.caption.copyWith(fontWeight: FontWeight.w600),
                          ),
                          Text(
                            ' (${doctor.reviewCount})',
                            style: AppDesignSystem.caption,
                          ),
                        ],
                      ],
                    ),
                    if (doctor.profession != null && doctor.profession!.isNotEmpty) ...[
                      const SizedBox(height: 4),
                      Text(
                        l10n.translateProfession(doctor.profession!),
                        style: AppDesignSystem.caption.copyWith(
                          color: AppDesignSystem.textSecondary,
                        ),
                        maxLines: 1,
                        overflow: TextOverflow.ellipsis,
                      ),
                    ],
                  ],
                ),
              ),
              IconButton(
                visualDensity: VisualDensity.compact,
                padding: EdgeInsets.zero,
                constraints: const BoxConstraints(minWidth: 32, minHeight: 32),
                onPressed: () => setState(() => _expanded = !_expanded),
                icon: Icon(
                  _expanded ? Icons.expand_less : Icons.expand_more,
                  color: AppDesignSystem.textTertiary,
                ),
              ),
            ],
          ),

          // ── Collapsed details (always visible per plan) ──
          if (locationLine != null) ...[
            const SizedBox(height: 8),
            Row(
              children: [
                Icon(Icons.location_on_outlined, size: 14, color: AppDesignSystem.textTertiary),
                const SizedBox(width: 4),
                Expanded(
                  child: Text(
                    locationLine,
                    style: AppDesignSystem.caption.copyWith(color: AppDesignSystem.textSecondary),
                    maxLines: 1,
                    overflow: TextOverflow.ellipsis,
                  ),
                ),
              ],
            ),
          ],
          if (doctor.effectiveSupportsOnline || doctor.effectiveSupportsInPerson) ...[
            const SizedBox(height: 8),
            Wrap(
              spacing: 6,
              runSpacing: 4,
              children: [
                if (doctor.effectiveSupportsOnline)
                  _modeBadge(l10n.consultationOnline, Colors.green.shade700),
                if (doctor.effectiveSupportsInPerson)
                  _modeBadge(l10n.consultationInPerson, AppDesignSystem.primary),
              ],
            ),
          ],
          const SizedBox(height: 8),
          Row(
            children: [
              if (availability != null && availability.isNotEmpty) ...[
                Icon(Icons.event_available, size: 14, color: AppDesignSystem.primary),
                const SizedBox(width: 4),
                Expanded(
                  child: Text(
                    _formatAvailability(l10n, availability),
                    style: AppDesignSystem.caption.copyWith(
                      color: AppDesignSystem.textPrimary,
                      fontWeight: FontWeight.w600,
                    ),
                    maxLines: 1,
                    overflow: TextOverflow.ellipsis,
                  ),
                ),
              ] else
                Expanded(
                  child: Text(
                    l10n.noUpcomingAvailability,
                    style: AppDesignSystem.caption.copyWith(color: AppDesignSystem.textTertiary),
                  ),
                ),
              if (startingPrice != null) ...[
                const SizedBox(width: 8),
                Text(
                  l10n.fromPrice(_formatPrice(startingPrice.$1, startingPrice.$2)),
                  style: AppDesignSystem.caption.copyWith(fontWeight: FontWeight.w700),
                ),
              ],
            ],
          ),

          // ── Expanded extras only ──
          if (_expanded) ...[
            const SizedBox(height: 10),
            if (doctor.clinic != null && doctor.clinic!.trim().isNotEmpty)
              Text(
                doctor.clinic!,
                style: AppDesignSystem.caption.copyWith(color: AppDesignSystem.textTertiary),
              ),
            if (doctor.onlineMinPriceMinor != null && doctor.minPriceCurrency != null) ...[
              const SizedBox(height: 6),
              Text(
                '${l10n.consultationOnline}: ${_formatPrice(doctor.onlineMinPriceMinor!, doctor.minPriceCurrency!)}',
                style: AppDesignSystem.caption,
              ),
            ],
            if (doctor.clinicMinPriceMinor != null && doctor.minPriceCurrency != null) ...[
              const SizedBox(height: 4),
              Text(
                '${l10n.consultationInPerson}: ${_formatPrice(doctor.clinicMinPriceMinor!, doctor.minPriceCurrency!)}',
                style: AppDesignSystem.caption,
              ),
            ],
            if (widget.distanceText != null) ...[
              const SizedBox(height: 4),
              Text(widget.distanceText!, style: AppDesignSystem.caption),
            ],
            if (isVerified) ...[
              const SizedBox(height: 6),
              Row(
                children: [
                  Icon(Icons.verified_user, size: 14, color: AppDesignSystem.primary),
                  const SizedBox(width: 4),
                  Text(l10n.verifiedDoctor, style: AppDesignSystem.caption),
                ],
              ),
            ],
            const SizedBox(height: 8),
            Align(
              alignment: Alignment.centerLeft,
              child: TextButton(
                onPressed: widget.onProfile,
                style: TextButton.styleFrom(
                  padding: EdgeInsets.zero,
                  minimumSize: Size.zero,
                  tapTargetSize: MaterialTapTargetSize.shrinkWrap,
                ),
                child: Text(l10n.viewProfile),
              ),
            ),
          ],

          // ── Primary CTA (always visible) ──
          const SizedBox(height: 10),
          Row(
            children: [
              Expanded(
                child: ShifaPrimaryButton(
                  label: l10n.bookAppointment,
                  onPressed: widget.onBook,
                  width: ButtonWidth.fill,
                ),
              ),
              const SizedBox(width: 8),
              ShifaSecondaryButton.icon(
                icon: Icons.chevron_right,
                onPressed: widget.onProfile,
              ),
            ],
          ),
        ],
      ),
    );
  }
}
