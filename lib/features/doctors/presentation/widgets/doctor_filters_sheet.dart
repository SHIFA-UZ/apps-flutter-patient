import 'package:flutter/material.dart';
import 'package:shifa_patient_app_v1/core/layout/responsive_layout.dart';
import 'package:shifa_patient_app_v1/core/localization/app_localizations.dart';
import 'package:shifa_patient_app_v1/core/models/profession_model.dart';
import 'package:shifa_patient_app_v1/core/models/region_catalog.dart';
import 'package:shifa_patient_app_v1/core/theme/app_design_system.dart';
import 'package:shifa_patient_app_v1/core/widgets/shifa_button.dart';

class DoctorFiltersSheetResult {
  final String? region;
  final String? specialty;
  final String? country;
  final String? consultationType;
  final String? availableWithin;
  final double? minRating;
  final int? minPriceMinor;
  final int? maxPriceMinor;
  final String? sortBy;

  const DoctorFiltersSheetResult({
    this.region,
    this.specialty,
    this.country,
    this.consultationType,
    this.availableWithin,
    this.minRating,
    this.minPriceMinor,
    this.maxPriceMinor,
    this.sortBy,
  });
}

Future<DoctorFiltersSheetResult?> showDoctorFiltersSheet({
  required BuildContext context,
  required List<String> regionOptions,
  required List<String> specialtyOptions,
  required DoctorFiltersSheetResult initial,
}) {
  final sheet = _DoctorFiltersSheet(
    regionOptions: regionOptions,
    specialtyOptions: specialtyOptions,
    initial: initial,
  );

  if (ResponsiveLayout.isWide(context, breakpoint: 640)) {
    return showDialog<DoctorFiltersSheetResult>(
      context: context,
      builder: (ctx) => Dialog(
        insetPadding: const EdgeInsets.symmetric(horizontal: 24, vertical: 48),
        child: ConstrainedBox(
          constraints: BoxConstraints(
            maxWidth: 480,
            maxHeight: MediaQuery.sizeOf(ctx).height * 0.85,
          ),
          child: sheet,
        ),
      ),
    );
  }

  return showModalBottomSheet<DoctorFiltersSheetResult>(
    context: context,
    isScrollControlled: true,
    useSafeArea: true,
    showDragHandle: true,
    shape: const RoundedRectangleBorder(
      borderRadius: BorderRadius.vertical(top: Radius.circular(16)),
    ),
    builder: (context) => sheet,
  );
}

class _DoctorFiltersSheet extends StatefulWidget {
  const _DoctorFiltersSheet({
    required this.regionOptions,
    required this.specialtyOptions,
    required this.initial,
  });

  final List<String> regionOptions;
  final List<String> specialtyOptions;
  final DoctorFiltersSheetResult initial;

  @override
  State<_DoctorFiltersSheet> createState() => _DoctorFiltersSheetState();
}

class _DoctorFiltersSheetState extends State<_DoctorFiltersSheet> {
  late String? _region;
  late String? _specialty;
  late String? _country;
  late String? _consultationType;
  late String? _availableWithin;
  late double? _minRating;
  late int? _minPriceMinor;
  late int? _maxPriceMinor;
  late String? _sortBy;

  @override
  void initState() {
    super.initState();
    _region = widget.initial.region;
    _specialty = widget.initial.specialty;
    _country = widget.initial.country;
    _consultationType = widget.initial.consultationType;
    _availableWithin = widget.initial.availableWithin;
    _minRating = widget.initial.minRating;
    _minPriceMinor = widget.initial.minPriceMinor;
    _maxPriceMinor = widget.initial.maxPriceMinor;
    _sortBy = widget.initial.sortBy;
  }

  void _apply() {
    Navigator.of(context).pop(
      DoctorFiltersSheetResult(
        region: _region,
        specialty: _specialty,
        country: _country,
        consultationType: _consultationType,
        availableWithin: _availableWithin,
        minRating: _minRating,
        minPriceMinor: _minPriceMinor,
        maxPriceMinor: _maxPriceMinor,
        sortBy: _sortBy,
      ),
    );
  }

  void _clear() {
    setState(() {
      _region = null;
      _specialty = null;
      _country = null;
      _consultationType = null;
      _availableWithin = null;
      _minRating = null;
      _minPriceMinor = null;
      _maxPriceMinor = null;
      _sortBy = null;
    });
  }

  @override
  Widget build(BuildContext context) {
    final l10n = AppLocalizations.of(context)!;
    final bottomInset = MediaQuery.viewInsetsOf(context).bottom;

    return Padding(
      padding: EdgeInsets.fromLTRB(16, 0, 16, 16 + bottomInset),
      child: SingleChildScrollView(
        physics: AppDesignSystem.listScrollPhysics(context),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.stretch,
          mainAxisSize: MainAxisSize.min,
          children: [
            const SizedBox(height: 8),
            Text(l10n.moreFilters, style: AppDesignSystem.h2),
            const SizedBox(height: 16),
            _dropdown(
              label: l10n.sortBy,
              value: _sortBy,
              items: {
                null: l10n.sortBy,
                'rating': l10n.sortByRating,
                'distance': l10n.sortByDistance,
                'reviews': l10n.sortByReviews,
                'availability': l10n.sortByAvailability,
              },
              onChanged: (v) => setState(() => _sortBy = v),
            ),
            const SizedBox(height: 12),
            _dropdown(
              label: l10n.filterByRegion,
              value: _region,
              items: {
                null: l10n.allRegions,
                ...{
                  for (final r in UzbekistanRegions.sortKeysForDisplay(
                    widget.regionOptions,
                    l10n.locale.languageCode,
                  ))
                    r: l10n.translateRegion(r),
                },
              },
              onChanged: (v) => setState(() => _region = v),
            ),
            const SizedBox(height: 12),
            _dropdown(
              label: l10n.filterBySpecialty,
              value: _specialty,
              items: {
                null: l10n.allSpecialties,
                ...{
                  for (final s in ProfessionData.sortEnglishKeysForDisplay(
                    widget.specialtyOptions,
                    l10n.locale.languageCode,
                  ))
                    s: l10n.translateProfession(s),
                },
              },
              onChanged: (v) => setState(() => _specialty = v),
            ),
            const SizedBox(height: 12),
            _dropdown(
              label: l10n.filterByCountry,
              value: _country,
              items: {
                null: l10n.allCountries,
                'Germany': l10n.countryGermany,
                'Uzbekistan': l10n.countryUzbekistan,
                'Turkey': l10n.countryTurkey,
                'United Arab Emirates': l10n.countryUae,
              },
              onChanged: (v) => setState(() => _country = v),
            ),
            const SizedBox(height: 12),
            _dropdown(
              label: l10n.filterConsultationType,
              value: _consultationType,
              items: {
                null: l10n.allConsultationTypes,
                'online': l10n.consultationOnline,
                'in_person': l10n.consultationInPerson,
                'both': l10n.consultationBoth,
              },
              onChanged: (v) => setState(() => _consultationType = v),
            ),
            const SizedBox(height: 12),
            _dropdown(
              label: l10n.filterAvailability,
              value: _availableWithin,
              items: {
                null: l10n.allAvailability,
                'today': l10n.availabilityToday,
                'tomorrow': l10n.availabilityTomorrow,
                'week': l10n.availabilityThisWeek,
              },
              onChanged: (v) => setState(() => _availableWithin = v),
            ),
            const SizedBox(height: 12),
            _dropdown(
              label: l10n.filterPrice,
              value: _priceBandKey(),
              items: {
                null: l10n.allPrices,
                'under20': l10n.priceUnder20,
                '20to50': l10n.price20to50,
                'over50': l10n.priceOver50,
              },
              onChanged: (v) => setState(() => _applyPriceBand(v)),
            ),
            const SizedBox(height: 12),
            _dropdown(
              label: l10n.filterMinRating,
              value: _minRating?.toString(),
              items: {
                null: l10n.allRatings,
                '4.5': '4.5+',
                '4.0': '4.0+',
              },
              onChanged: (v) => setState(() => _minRating = v != null ? double.tryParse(v) : null),
            ),
            const SizedBox(height: 20),
            Row(
              children: [
                Expanded(
                  child: ShifaSecondaryButton(
                    label: l10n.clear,
                    onPressed: _clear,
                  ),
                ),
                const SizedBox(width: 12),
                Expanded(
                  child: ShifaPrimaryButton(
                    label: l10n.applyFilters,
                    onPressed: _apply,
                  ),
                ),
              ],
            ),
          ],
        ),
      ),
    );
  }

  String? _priceBandKey() {
    if (_minPriceMinor == null && _maxPriceMinor == 2000) return 'under20';
    if (_minPriceMinor == 2000 && _maxPriceMinor == 5000) return '20to50';
    if (_minPriceMinor == 5000 && _maxPriceMinor == null) return 'over50';
    return null;
  }

  void _applyPriceBand(String? key) {
    switch (key) {
      case 'under20':
        _minPriceMinor = null;
        _maxPriceMinor = 2000;
      case '20to50':
        _minPriceMinor = 2000;
        _maxPriceMinor = 5000;
      case 'over50':
        _minPriceMinor = 5000;
        _maxPriceMinor = null;
      default:
        _minPriceMinor = null;
        _maxPriceMinor = null;
    }
  }

  Widget _dropdown({
    required String label,
    required String? value,
    required Map<String?, String> items,
    required ValueChanged<String?> onChanged,
  }) {
    return DropdownButtonFormField<String?>(
      value: value,
      decoration: InputDecoration(
        labelText: label,
        border: OutlineInputBorder(borderRadius: BorderRadius.circular(12)),
        filled: true,
        fillColor: AppDesignSystem.backgroundSecondary,
      ),
      isExpanded: true,
      selectedItemBuilder: (context) {
        return items.entries
            .map(
              (e) => Align(
                alignment: Alignment.centerLeft,
                child: Text(
                  e.value,
                  maxLines: 1,
                  overflow: TextOverflow.ellipsis,
                ),
              ),
            )
            .toList();
      },
      items: items.entries
          .map(
            (e) => DropdownMenuItem<String?>(
              value: e.key,
              child: Text(e.value, maxLines: 1, overflow: TextOverflow.ellipsis),
            ),
          )
          .toList(),
      onChanged: onChanged,
    );
  }
}
