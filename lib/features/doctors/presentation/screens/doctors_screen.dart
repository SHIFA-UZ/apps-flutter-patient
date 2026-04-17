import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:geolocator/geolocator.dart';
import 'package:go_router/go_router.dart';
import 'package:shifa_patient_app_v1/app/router.dart';
import 'package:shifa_patient_app_v1/core/localization/app_localizations.dart';
import 'package:shifa_patient_app_v1/core/models/doctor_model.dart';
import 'package:shifa_patient_app_v1/core/widgets/shifa_button.dart';
import 'package:shifa_patient_app_v1/core/theme/app_design_system.dart';
import 'package:shifa_patient_app_v1/core/utils/image_utils.dart';
import 'package:shifa_patient_app_v1/core/utils/location_utils.dart';
import 'package:shifa_patient_app_v1/core/widgets/app_header.dart';
import 'package:shifa_patient_app_v1/core/widgets/doctor_card.dart';
import 'package:shifa_patient_app_v1/core/widgets/empty_state.dart';
import 'package:shifa_patient_app_v1/core/widgets/segmented_control.dart';
import 'package:shifa_patient_app_v1/features/doctors/providers/doctors_provider.dart';
import 'package:shifa_patient_app_v1/features/profile/providers/profile_provider.dart';

class DoctorsScreen extends ConsumerStatefulWidget {
  const DoctorsScreen({super.key});

  @override
  ConsumerState<DoctorsScreen> createState() => _DoctorsScreenState();
}

enum _SortOption { distance, rating, reviews }

class _DoctorsScreenState extends ConsumerState<DoctorsScreen> {
  int _selectedTab = 0; // 0 = My Doctors, 1 = Recommended
  final _searchController = TextEditingController();
  _SortOption _sortBy = _SortOption.rating;
  String? _filterRegion;
  String? _filterSpecialty;
  /// One-time GPS location when user selects "Sort by distance"
  double? _sortDistanceLat;
  double? _sortDistanceLon;
  bool _isFetchingLocationForSort = false;

  @override
  void initState() {
    super.initState();
    // Load all doctors (Recommended tab) and my doctors (My Doctors tab) on init
    WidgetsBinding.instance.addPostFrameCallback((_) {
      ref.read(doctorsProvider.notifier).loadDoctors();
      ref.read(doctorsProvider.notifier).loadMyDoctors();
      ref.read(profileProvider.notifier).loadProfile();
    });
  }

  @override
  void dispose() {
    _searchController.dispose();
    super.dispose();
  }

  void _performSearch(String query) {
    if (query.isEmpty) {
      ref.read(doctorsProvider.notifier).loadDoctors();
    } else {
      ref.read(doctorsProvider.notifier).searchDoctors(search: query);
    }
  }

  /// One-time GPS fetch when user selects "Sort by distance"
  Future<void> _fetchCurrentLocationForSort() async {
    if (_isFetchingLocationForSort || !mounted) return;
    setState(() => _isFetchingLocationForSort = true);
    final l10n = AppLocalizations.of(context)!;
    ScaffoldMessenger.of(context).showSnackBar(
      SnackBar(content: Text(l10n.gettingYourLocation), duration: const Duration(seconds: 2)),
    );

    try {
      final serviceEnabled = await Geolocator.isLocationServiceEnabled();
      if (!serviceEnabled && mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
          SnackBar(
            content: Text(l10n.translate('locationServicesDisabled')),
            backgroundColor: Colors.orange,
            duration: const Duration(seconds: 3),
          ),
        );
        setState(() => _isFetchingLocationForSort = false);
        return;
      }

      LocationPermission permission = await Geolocator.checkPermission();
      if (permission == LocationPermission.denied) {
        permission = await Geolocator.requestPermission();
      }
      if (permission == LocationPermission.denied ||
          permission == LocationPermission.deniedForever) {
        if (mounted) {
          ScaffoldMessenger.of(context).showSnackBar(
            SnackBar(
              content: Text(l10n.translate('locationPermissionDenied')),
              backgroundColor: Colors.orange,
              duration: const Duration(seconds: 3),
            ),
          );
        }
        setState(() => _isFetchingLocationForSort = false);
        return;
      }

      final position = await Geolocator.getCurrentPosition(
        desiredAccuracy: LocationAccuracy.medium,
      );

      if (mounted) {
        setState(() {
          _sortDistanceLat = position.latitude;
          _sortDistanceLon = position.longitude;
          _isFetchingLocationForSort = false;
        });
        ScaffoldMessenger.of(context).showSnackBar(
          SnackBar(
            content: Text(l10n.usingCurrentLocation),
            backgroundColor: Colors.green,
            duration: const Duration(seconds: 2),
          ),
        );
      }
    } catch (e) {
      if (mounted) {
        setState(() => _isFetchingLocationForSort = false);
        ScaffoldMessenger.of(context).showSnackBar(
          SnackBar(
            content: Text(l10n.couldNotGetLocationUsingProfile),
            backgroundColor: Colors.orange,
            duration: const Duration(seconds: 3),
          ),
        );
      }
    }
  }

  /// Returns filtered and sorted list for Recommended tab
  List<DoctorModel> _getFilteredAndSortedDoctors(
    List<DoctorModel> doctors,
    double? patientLat,
    double? patientLon,
  ) {
    var list = List<DoctorModel>.from(doctors);

    // Filter by region
    if (_filterRegion != null && _filterRegion!.isNotEmpty) {
      list = list.where((d) => (d.region ?? '').trim() == _filterRegion!.trim()).toList();
    }

    // Filter by specialty (profession)
    if (_filterSpecialty != null && _filterSpecialty!.isNotEmpty) {
      list = list.where((d) => (d.profession ?? '').trim() == _filterSpecialty!.trim()).toList();
    }

    // Sort
    switch (_sortBy) {
      case _SortOption.distance:
        if (patientLat != null && patientLon != null) {
          list.sort((a, b) {
            final distA = distanceKm(patientLat, patientLon, a.latitude, a.longitude) ?? double.infinity;
            final distB = distanceKm(patientLat, patientLon, b.latitude, b.longitude) ?? double.infinity;
            return distA.compareTo(distB);
          });
        }
        break;
      case _SortOption.rating:
        list.sort((a, b) {
          final rA = a.rating ?? 0.0;
          final rB = b.rating ?? 0.0;
          return rB.compareTo(rA);
        });
        break;
      case _SortOption.reviews:
        list.sort((a, b) {
          final cA = a.reviewCount ?? 0;
          final cB = b.reviewCount ?? 0;
          return cB.compareTo(cA);
        });
        break;
    }

    return list;
  }

  @override
  Widget build(BuildContext context) {
    final l10n = AppLocalizations.of(context)!;
    final doctorsState = ref.watch(doctorsProvider);
    final profileState = ref.watch(profileProvider);
    final displayPhotoUrl = withCacheBuster(
      profileState.profile?.photoUrl,
      profileState.photoCacheKey,
    );

    final patientLat = _sortBy == _SortOption.distance && _sortDistanceLat != null && _sortDistanceLon != null
        ? _sortDistanceLat
        : profileState.profile?.latitude;
    final patientLon = _sortBy == _SortOption.distance && _sortDistanceLat != null && _sortDistanceLon != null
        ? _sortDistanceLon
        : profileState.profile?.longitude;
    // Tab 0 = My Doctors (doctors patient has had appointments with), Tab 1 = Recommended (all doctors)
    final displayList = _selectedTab == 0
        ? doctorsState.myDoctors
        : _getFilteredAndSortedDoctors(doctorsState.doctors, patientLat, patientLon);

    return Scaffold(
      backgroundColor: AppDesignSystem.background,
      body: RefreshIndicator(
        onRefresh: () async {
          await ref.read(doctorsProvider.notifier).loadDoctors();
          await ref.read(doctorsProvider.notifier).loadMyDoctors();
          await ref.read(profileProvider.notifier).loadProfile();
        },
        child: CustomScrollView(
          physics: const AlwaysScrollableScrollPhysics(),
          slivers: [
          SliverToBoxAdapter(
            child: AppHeader(
              title: l10n.doctors,
              showProfile: true,
              showNotification: true,
              showBack: false,
              onLogoTap: () => context.go(AppRoutes.home),
              onProfileTap: () => context.push(AppRoutes.account),
              profilePhotoUrl: displayPhotoUrl,
              onNotificationTap: () => context.push(AppRoutes.notifications),
              onChatTap: () => context.push(AppRoutes.chat),
            ),
          ),
          SliverToBoxAdapter(
            child: Padding(
              padding: const EdgeInsets.fromLTRB(
                AppDesignSystem.screenPaddingH,
                16,
                AppDesignSystem.screenPaddingH,
                8,
              ),
              child: SegmentedControl(
                selectedIndex: _selectedTab,
                labels: [l10n.myDoctors, l10n.recommended],
                onSelected: (index) => setState(() => _selectedTab = index),
              ),
            ),
          ),
          if (_selectedTab == 1)
            SliverToBoxAdapter(
              child: Padding(
                padding: const EdgeInsets.fromLTRB(
                  AppDesignSystem.screenPaddingH,
                  8,
                  AppDesignSystem.screenPaddingH,
                  8,
                ),
                child: TextField(
                  controller: _searchController,
                  decoration: InputDecoration(
                    hintText: l10n.searchDoctors,
                    prefixIcon: const Icon(Icons.search),
                    border: OutlineInputBorder(borderRadius: BorderRadius.circular(12)),
                    filled: true,
                    fillColor: AppDesignSystem.backgroundSecondary,
                  ),
                  onChanged: _performSearch,
                ),
              ),
            ),
          if (_selectedTab == 1)
            SliverToBoxAdapter(
              child: Padding(
                padding: const EdgeInsets.fromLTRB(
                  AppDesignSystem.screenPaddingH,
                  0,
                  AppDesignSystem.screenPaddingH,
                  8,
                ),
                child: Row(
                  children: [
                    Expanded(
                      child: DropdownButtonFormField<_SortOption>(
                        value: _sortBy,
                        decoration: InputDecoration(
                          labelText: l10n.sortBy,
                          border: OutlineInputBorder(borderRadius: BorderRadius.circular(12)),
                          contentPadding: const EdgeInsets.symmetric(horizontal: 12, vertical: 8),
                          filled: true,
                          fillColor: AppDesignSystem.backgroundSecondary,
                          suffixIcon: _isFetchingLocationForSort
                              ? const Padding(
                                  padding: EdgeInsets.only(right: 12),
                                  child: SizedBox(
                                    width: 20,
                                    height: 20,
                                    child: CircularProgressIndicator(strokeWidth: 2),
                                  ),
                                )
                              : null,
                        ),
                        isExpanded: true,
                        items: [
                          DropdownMenuItem(value: _SortOption.distance, child: Text(l10n.sortByDistance)),
                          DropdownMenuItem(value: _SortOption.rating, child: Text(l10n.sortByRating)),
                          DropdownMenuItem(value: _SortOption.reviews, child: Text(l10n.sortByReviews)),
                        ],
                        onChanged: (value) {
                          if (value == null) return;
                          setState(() => _sortBy = value);
                          if (value == _SortOption.distance) _fetchCurrentLocationForSort();
                        },
                      ),
                    ),
                    const SizedBox(width: 8),
                    Expanded(
                      child: _buildFilterDropdown<String>(
                        value: _filterRegion,
                        hint: l10n.filterByRegion,
                        allLabel: l10n.allRegions,
                        options: _regionOptions(doctorsState.doctors),
                        onChanged: (v) => setState(() => _filterRegion = v),
                      ),
                    ),
                    const SizedBox(width: 8),
                    Expanded(
                      child: _buildFilterDropdown<String>(
                        value: _filterSpecialty,
                        hint: l10n.filterBySpecialty,
                        allLabel: l10n.allSpecialties,
                        options: _specialtyOptions(doctorsState.doctors),
                        onChanged: (v) => setState(() => _filterSpecialty = v),
                      ),
                    ),
                  ],
                ),
              ),
            ),
          if (_selectedTab == 0 ? doctorsState.isLoadingMyDoctors : doctorsState.isLoading)
            const SliverToBoxAdapter(
              child: Padding(
                padding: EdgeInsets.all(32),
                child: Center(child: CircularProgressIndicator()),
              ),
            )
          else if (_selectedTab == 0 ? doctorsState.errorMyDoctors != null : doctorsState.error != null)
            SliverToBoxAdapter(
              child: EmptyState(
                icon: Icons.error_outline,
                message: '${l10n.error}: ${_selectedTab == 0 ? doctorsState.errorMyDoctors : doctorsState.error}',
                action: ShifaPrimaryButton(
                  label: l10n.retry,
                  onPressed: () {
                    if (_selectedTab == 0) {
                      ref.read(doctorsProvider.notifier).loadMyDoctors();
                    } else {
                      ref.read(doctorsProvider.notifier).loadDoctors();
                    }
                  },
                  width: ButtonWidth.hug,
                ),
              ),
            )
          else if (displayList.isEmpty)
            SliverToBoxAdapter(
              child: EmptyState(
                icon: Icons.medical_services,
                message: l10n.translate('noDoctorsFound'),
              ),
            )
          else
            SliverPadding(
              padding: const EdgeInsets.symmetric(horizontal: AppDesignSystem.screenPaddingH),
              sliver: SliverList(
                delegate: SliverChildBuilderDelegate(
                  (context, index) {
                    if (index >= displayList.length) return const SizedBox.shrink();
                    final doctor = displayList[index];
                    double? km;
                    if (patientLat != null && patientLon != null && doctor.latitude != null && doctor.longitude != null) {
                      km = distanceKm(patientLat, patientLon, doctor.latitude, doctor.longitude);
                    }
                    String? distText;
                    if (km != null && !km.isInfinite) {
                      distText = km < 1 ? '${(km * 1000).round()} m' : '${km.toStringAsFixed(1)} km';
                    }
                    return Padding(
                      padding: const EdgeInsets.only(bottom: AppDesignSystem.cardSpacing),
                      child: DoctorCard(
                        doctor: doctor,
                        distanceText: distText,
                        onTap: () => context.push('${AppRoutes.doctors}/${doctor.id}'),
                      ),
                    );
                  },
                  childCount: displayList.length,
                ),
              ),
            ),
          SliverToBoxAdapter(
            child: SizedBox(height: AppDesignSystem.safeBottomWithNavBar(context)),
          ),
        ],
        ),
      ),
    );
  }

  List<String> _regionOptions(List<DoctorModel> doctors) {
    final set = doctors
        .map((d) => d.region)
        .where((r) => r != null && r.toString().trim().isNotEmpty)
        .map((r) => r!.trim())
        .toSet();
    final list = set.toList()..sort();
    return list;
  }

  List<String> _specialtyOptions(List<DoctorModel> doctors) {
    final set = doctors
        .map((d) => d.profession)
        .where((p) => p != null && p.toString().trim().isNotEmpty)
        .map((p) => p!.trim())
        .toSet();
    final list = set.toList()..sort();
    return list;
  }

  Widget _buildFilterDropdown<T>({
    required T? value,
    required String hint,
    required String allLabel,
    required List<T> options,
    required void Function(T?) onChanged,
  }) {
    return DropdownButtonFormField<T?>(
      value: value,
      decoration: InputDecoration(
        labelText: hint,
        border: OutlineInputBorder(borderRadius: BorderRadius.circular(12)),
        contentPadding: const EdgeInsets.symmetric(horizontal: 12, vertical: 8),
        filled: true,
        fillColor: AppDesignSystem.backgroundSecondary,
      ),
      isExpanded: true,
      items: [
        DropdownMenuItem<T?>(value: null, child: Text(allLabel)),
        ...options.map((o) => DropdownMenuItem<T?>(value: o, child: Text(o.toString()))),
      ],
      onChanged: (v) => onChanged(v),
    );
  }
}
