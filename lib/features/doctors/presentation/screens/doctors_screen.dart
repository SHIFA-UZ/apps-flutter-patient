import 'dart:async';

import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:geolocator/geolocator.dart';
import 'package:go_router/go_router.dart';
import 'package:shifa_patient_app_v1/app/router.dart';
import 'package:shifa_patient_app_v1/app/shell_tab_index_provider.dart';
import 'package:shifa_patient_app_v1/core/layout/responsive_layout.dart';
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
import 'package:shifa_patient_app_v1/features/doctors/data/doctors_filter_catalog.dart';
import 'package:shifa_patient_app_v1/features/doctors/data/doctors_search_filters.dart';
import 'package:shifa_patient_app_v1/features/doctors/presentation/widgets/doctor_discovery_card.dart';
import 'package:shifa_patient_app_v1/features/doctors/presentation/widgets/doctor_filter_chips.dart';
import 'package:shifa_patient_app_v1/features/doctors/presentation/widgets/doctor_filters_sheet.dart';
import 'package:shifa_patient_app_v1/features/doctors/presentation/widgets/doctors_pagination_bar.dart';
import 'package:shifa_patient_app_v1/features/doctors/providers/doctors_provider.dart';
import 'package:shifa_patient_app_v1/features/profile/providers/profile_provider.dart';

class DoctorsScreen extends ConsumerStatefulWidget {
  const DoctorsScreen({super.key});

  @override
  ConsumerState<DoctorsScreen> createState() => _DoctorsScreenState();
}

class _DoctorsScreenState extends ConsumerState<DoctorsScreen> {
  int _selectedTab = 1;
  final _searchController = TextEditingController();
  Timer? _searchDebounce;

  bool _chipOnline = false;
  bool _chipNearMe = false;
  bool _chipToday = false;
  bool _chipThisWeek = false;
  bool _chipTopRated = false;
  bool _chipVerified = false;
  bool _chipGermany = false;
  bool _chipUzbekistan = false;

  double? _nearMeLat;
  double? _nearMeLon;
  bool _isFetchingLocation = false;

  DoctorFiltersSheetResult _sheetFilters = const DoctorFiltersSheetResult();
  final _scrollController = ScrollController();
  int _currentPage = 1;

  @override
  void initState() {
    super.initState();
    WidgetsBinding.instance.addPostFrameCallback((_) {
      _applyFilters();
      ref.read(doctorsProvider.notifier).loadFilterCatalog();
      ref.read(doctorsProvider.notifier).loadMyDoctors();
      ref.read(profileProvider.notifier).loadProfile();
    });
  }

  @override
  void dispose() {
    _searchDebounce?.cancel();
    _searchController.dispose();
    _scrollController.dispose();
    super.dispose();
  }

  void _scheduleSearch(String query) {
    _searchDebounce?.cancel();
    _searchDebounce = Timer(const Duration(milliseconds: 350), _applyFilters);
  }

  DoctorsSearchFilters _composeFilters() {
    String? consultationType = _sheetFilters.consultationType;
    if (_chipOnline && consultationType == null) consultationType = 'online';

    String? availableWithin = _sheetFilters.availableWithin;
    if (_chipToday) {
      availableWithin = 'today';
    } else if (_chipThisWeek) {
      availableWithin = 'week';
    }

    double? minRating = _sheetFilters.minRating;
    if (_chipTopRated && (minRating == null || minRating < 4.5)) {
      minRating = 4.5;
    }

    String? country = _sheetFilters.country;
    if (_chipGermany) {
      country = 'Germany';
    } else if (_chipUzbekistan) {
      country = 'Uzbekistan';
    }

    String? sortBy = _sheetFilters.sortBy;
    if (_chipNearMe && _nearMeLat != null && _nearMeLon != null) {
      sortBy = 'distance';
    } else if (_chipTopRated && sortBy == null) {
      sortBy = 'rating';
    }

    return DoctorsSearchFilters(
      search: _searchController.text.trim().isEmpty ? null : _searchController.text.trim(),
      profession: _sheetFilters.specialty,
      region: _sheetFilters.region,
      country: country,
      consultationType: consultationType,
      availableWithin: availableWithin,
      minRating: minRating,
      minPriceMinor: _sheetFilters.minPriceMinor,
      maxPriceMinor: _sheetFilters.maxPriceMinor,
      verifiedOnly: _chipVerified ? true : null,
      latitude: _chipNearMe ? _nearMeLat : null,
      longitude: _chipNearMe ? _nearMeLon : null,
      radiusKm: _chipNearMe ? 50 : null,
      sortBy: sortBy,
      includeNextAvailable: true,
      page: _currentPage,
      pageSize: DoctorsSearchFilters.defaultPageSize,
    );
  }

  Future<void> _applyFilters({bool resetPage = true}) async {
    if (resetPage) {
      setState(() => _currentPage = 1);
    }
    await ref.read(doctorsProvider.notifier).searchDoctors(filters: _composeFilters());
  }

  Future<void> _goToPage(int page) async {
    if (page < 1 || page == _currentPage) return;
    setState(() => _currentPage = page);
    await ref.read(doctorsProvider.notifier).searchDoctors(filters: _composeFilters());
    if (_scrollController.hasClients) {
      await _scrollController.animateTo(
        0,
        duration: const Duration(milliseconds: 250),
        curve: Curves.easeOut,
      );
    }
  }

  Future<void> _toggleNearMe() async {
    if (_chipNearMe) {
      setState(() {
        _chipNearMe = false;
        _nearMeLat = null;
        _nearMeLon = null;
      });
      await _applyFilters();
      return;
    }

    setState(() => _isFetchingLocation = true);
    final l10n = AppLocalizations.of(context)!;
    try {
      final serviceEnabled = await Geolocator.isLocationServiceEnabled();
      if (!serviceEnabled && mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
          SnackBar(content: Text(l10n.translate('locationServicesDisabled'))),
        );
        setState(() => _isFetchingLocation = false);
        return;
      }

      var permission = await Geolocator.checkPermission();
      if (permission == LocationPermission.denied) {
        permission = await Geolocator.requestPermission();
      }
      if (permission == LocationPermission.denied ||
          permission == LocationPermission.deniedForever) {
        if (mounted) {
          ScaffoldMessenger.of(context).showSnackBar(
            SnackBar(content: Text(l10n.translate('locationPermissionDenied'))),
          );
        }
        setState(() => _isFetchingLocation = false);
        return;
      }

      final position = await Geolocator.getCurrentPosition(
        desiredAccuracy: LocationAccuracy.medium,
      );
      if (!mounted) return;
      setState(() {
        _chipNearMe = true;
        _nearMeLat = position.latitude;
        _nearMeLon = position.longitude;
        _isFetchingLocation = false;
      });
      await _applyFilters();
    } catch (_) {
      if (mounted) {
        setState(() => _isFetchingLocation = false);
        ScaffoldMessenger.of(context).showSnackBar(
          SnackBar(content: Text(l10n.couldNotGetLocationUsingProfile)),
        );
      }
    }
  }

  Future<void> _openFiltersSheet(DoctorsState doctorsState) async {
    final country = _sheetFilters.country ??
        (_chipGermany ? 'Germany' : null);
    final regionOptions = doctorsState.catalogRegionOptions.isNotEmpty
        ? doctorsState.catalogRegionOptions
        : DoctorsFilterCatalog.regionOptions(
            doctorsState.doctors,
            countryFilter: country,
          );
    final specialtyOptions = doctorsState.catalogSpecialtyOptions.isNotEmpty
        ? doctorsState.catalogSpecialtyOptions
        : DoctorsFilterCatalog.specialtyOptions(doctorsState.doctors);

    final result = await showDoctorFiltersSheet(
      context: context,
      regionOptions: regionOptions,
      specialtyOptions: specialtyOptions,
      initial: _sheetFilters,
    );
    if (result == null || !mounted) return;
    setState(() => _sheetFilters = result);
    await _applyFilters();
  }

  String? _distanceText(DoctorModel doctor, double? patientLat, double? patientLon) {
    if (doctor.distanceKm != null) {
      final km = doctor.distanceKm!;
      return km < 1 ? '${(km * 1000).round()} m' : '${km.toStringAsFixed(1)} km';
    }
    if (patientLat != null &&
        patientLon != null &&
        doctor.latitude != null &&
        doctor.longitude != null) {
      final km = distanceKm(patientLat, patientLon, doctor.latitude, doctor.longitude);
      if (km != null && !km.isInfinite) {
        return km < 1 ? '${(km * 1000).round()} m' : '${km.toStringAsFixed(1)} km';
      }
    }
    return null;
  }

  @override
  Widget build(BuildContext context) {
    ref.listen<int>(shellTabIndexProvider, (previous, next) {
      if (next == 3 && previous != 3 && _selectedTab == 1) {
        _applyFilters();
      }
    });

    final l10n = AppLocalizations.of(context)!;
    final doctorsState = ref.watch(doctorsProvider);
    final profileState = ref.watch(profileProvider);
    final displayPhotoUrl = withCacheBuster(
      profileState.profile?.photoUrl,
      profileState.photoCacheKey,
    );

    final patientLat = _chipNearMe ? _nearMeLat : profileState.profile?.latitude;
    final patientLon = _chipNearMe ? _nearMeLon : profileState.profile?.longitude;

    final displayList =
        _selectedTab == 0 ? doctorsState.myDoctors : doctorsState.doctors;
    final totalCount = doctorsState.totalCount;
    final pagePadding = ResponsiveLayout.symmetricPagePadding(context);
    final listHorizontal = ResponsiveLayout.horizontalInset(context);

    return Scaffold(
      backgroundColor: AppDesignSystem.background,
      body: RefreshIndicator(
        onRefresh: () async {
          await _applyFilters();
          await ref.read(doctorsProvider.notifier).loadMyDoctors();
          await ref.read(profileProvider.notifier).loadProfile();
        },
        child: CustomScrollView(
          controller: _scrollController,
          physics: AppDesignSystem.listScrollPhysics(context),
          slivers: [
            SliverToBoxAdapter(
              child: AppHeader(
                title: l10n.findADoctor,
                subtitle: totalCount > 0 ? l10n.doctorsCount(totalCount) : null,
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
                padding: pagePadding.copyWith(top: 16, bottom: 8),
                child: SegmentedControl(
                  selectedIndex: _selectedTab,
                  labels: [l10n.recentDoctors, l10n.recommended],
                  onSelected: (index) => setState(() => _selectedTab = index),
                ),
              ),
            ),
            if (_selectedTab == 1) ...[
              SliverToBoxAdapter(
                child: Padding(
                  padding: pagePadding.copyWith(top: 8, bottom: 8),
                  child: TextField(
                    controller: _searchController,
                    textInputAction: TextInputAction.search,
                    decoration: InputDecoration(
                      hintText: l10n.searchDoctorsHint,
                      prefixIcon: const Icon(Icons.search),
                      border: OutlineInputBorder(borderRadius: BorderRadius.circular(12)),
                      filled: true,
                      fillColor: AppDesignSystem.backgroundSecondary,
                    ),
                    onChanged: _scheduleSearch,
                  ),
                ),
              ),
              SliverToBoxAdapter(
                child: Padding(
                  padding: const EdgeInsets.only(bottom: 8),
                  child: DoctorQuickFilterChips(
                    online: _chipOnline,
                    nearMe: _chipNearMe,
                    today: _chipToday,
                    thisWeek: _chipThisWeek,
                    topRated: _chipTopRated,
                    verified: _chipVerified,
                    germany: _chipGermany,
                    uzbekistan: _chipUzbekistan,
                    isFetchingLocation: _isFetchingLocation,
                    onOnlineChanged: () async {
                      setState(() => _chipOnline = !_chipOnline);
                      await _applyFilters();
                    },
                    onNearMeChanged: _toggleNearMe,
                    onTodayChanged: () async {
                      setState(() {
                        _chipToday = !_chipToday;
                        if (_chipToday) _chipThisWeek = false;
                      });
                      await _applyFilters();
                    },
                    onThisWeekChanged: () async {
                      setState(() {
                        _chipThisWeek = !_chipThisWeek;
                        if (_chipThisWeek) _chipToday = false;
                      });
                      await _applyFilters();
                    },
                    onTopRatedChanged: () async {
                      setState(() => _chipTopRated = !_chipTopRated);
                      await _applyFilters();
                    },
                    onVerifiedChanged: () async {
                      setState(() => _chipVerified = !_chipVerified);
                      await _applyFilters();
                    },
                    onGermanyChanged: () async {
                      setState(() {
                        _chipGermany = !_chipGermany;
                        if (_chipGermany) _chipUzbekistan = false;
                      });
                      await _applyFilters();
                    },
                    onUzbekistanChanged: () async {
                      setState(() {
                        _chipUzbekistan = !_chipUzbekistan;
                        if (_chipUzbekistan) _chipGermany = false;
                      });
                      await _applyFilters();
                    },
                    onMoreFilters: () => _openFiltersSheet(doctorsState),
                  ),
                ),
              ),
            ],
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
                  message:
                      '${l10n.error}: ${_selectedTab == 0 ? doctorsState.errorMyDoctors : doctorsState.error}',
                  action: ShifaPrimaryButton(
                    label: l10n.retry,
                    onPressed: () {
                      if (_selectedTab == 0) {
                        ref.read(doctorsProvider.notifier).loadMyDoctors();
                      } else {
                        _applyFilters();
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
                padding: EdgeInsets.symmetric(horizontal: listHorizontal),
                sliver: SliverList(
                  delegate: SliverChildBuilderDelegate(
                    (context, index) {
                      if (index >= displayList.length) return const SizedBox.shrink();
                      final doctor = displayList[index];
                      final distText = _distanceText(doctor, patientLat, patientLon);

                      if (_selectedTab == 0) {
                        return Padding(
                          padding: const EdgeInsets.only(bottom: AppDesignSystem.cardSpacing),
                          child: DoctorCard(
                            doctor: doctor,
                            distanceText: distText,
                            onTap: () => context.push('${AppRoutes.doctors}/${doctor.id}'),
                          ),
                        );
                      }

                      return Padding(
                        padding: const EdgeInsets.only(bottom: AppDesignSystem.cardSpacing),
                        child: DoctorDiscoveryCard(
                          doctor: doctor,
                          distanceText: distText,
                          onBook: () => context.push('/bookings/flow/${doctor.id}'),
                          onProfile: () => context.push('${AppRoutes.doctors}/${doctor.id}'),
                        ),
                      );
                    },
                    childCount: displayList.length,
                  ),
                ),
              ),
            if (_selectedTab == 1 && !doctorsState.isLoading && displayList.isNotEmpty)
              SliverToBoxAdapter(
                child: Padding(
                  padding: pagePadding,
                  child: DoctorsPaginationBar(
                    currentPage: doctorsState.currentPage,
                    totalPages: doctorsState.totalPages,
                    totalCount: doctorsState.totalCount,
                    onPageSelected: _goToPage,
                  ),
                ),
              ),
            if (_selectedTab == 1 &&
                doctorsState.isEnrichingAvailability &&
                !doctorsState.isLoading)
              const SliverToBoxAdapter(
                child: Padding(
                  padding: EdgeInsets.only(bottom: 8),
                  child: Center(
                    child: SizedBox(
                      width: 20,
                      height: 20,
                      child: CircularProgressIndicator(strokeWidth: 2),
                    ),
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
}
