import 'package:flutter/foundation.dart' show kIsWeb;
import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:geolocator/geolocator.dart';
import 'package:go_router/go_router.dart';
import 'package:intl/intl.dart';
import 'package:shifa_patient_app_v1/app/router.dart';
import 'package:shifa_patient_app_v1/core/localization/app_localizations.dart';
import 'package:shifa_patient_app_v1/core/models/doctor_model.dart';
import 'package:shifa_patient_app_v1/core/theme/app_design_system.dart';
import 'package:shifa_patient_app_v1/core/utils/image_utils.dart';
import 'package:shifa_patient_app_v1/core/utils/date_utils.dart' show parseAppointmentDateTime;
import 'package:shifa_patient_app_v1/core/utils/location_utils.dart' show distanceKm, nearbyDoctorsMaxRadiusKm;
import 'package:shifa_patient_app_v1/core/widgets/app_header.dart';
import 'package:shifa_patient_app_v1/core/widgets/appointment_card.dart';
import 'package:shifa_patient_app_v1/core/widgets/doctor_card.dart';
import 'package:shifa_patient_app_v1/core/widgets/empty_state.dart';
import 'package:shifa_patient_app_v1/core/widgets/remote_care_task_card.dart';
import 'package:shifa_patient_app_v1/core/widgets/section_title.dart';
import 'package:shifa_patient_app_v1/features/bookings/providers/bookings_provider.dart';
import 'package:shifa_patient_app_v1/features/doctors/providers/doctors_provider.dart';
import 'package:shifa_patient_app_v1/features/profile/providers/profile_provider.dart';
import 'package:shifa_patient_app_v1/core/services/app_lock_provider.dart';

class HomeScreen extends ConsumerStatefulWidget {
  const HomeScreen({super.key});

  @override
  ConsumerState<HomeScreen> createState() => _HomeScreenState();
}

class _HomeScreenState extends ConsumerState<HomeScreen> {
  double? _nearbyLocationLat;
  double? _nearbyLocationLon;
  bool _isFetchingLocation = false;

  @override
  void initState() {
    super.initState();
    WidgetsBinding.instance.addPostFrameCallback((_) {
      _loadData();
      _maybeShowBiometricPrompt();
    });
  }

  Future<void> _maybeShowBiometricPrompt() async {
    if (kIsWeb) return;
    if (!mounted) return;
    final l10n = AppLocalizations.of(context)!;
    final lockService = ref.read(appLockServiceProvider);
    final hasSeen = await lockService.hasSeenBiometricPrompt();
    final biometricAvailable = await lockService.isBiometricAvailable();
    if (!mounted) return;
    if (hasSeen || !biometricAvailable) return;
    final result = await showDialog<String>(
      context: context,
      barrierDismissible: false,
      builder: (ctx) => AlertDialog(
        title: Text(l10n.enableBiometricForAppSecurity),
        content: Text(l10n.enableBiometricPrompt),
        actions: [
          TextButton(
            onPressed: () => Navigator.pop(ctx, 'skip'),
            child: Text(l10n.skipBiometric),
          ),
          FilledButton(
            onPressed: () => Navigator.pop(ctx, 'enable'),
            child: Text(l10n.enableBiometric ?? 'Enable'),
          ),
        ],
      ),
    );
    if (!mounted) return;
    await lockService.setHasSeenBiometricPrompt();
    if (result == 'enable' && mounted) {
      context.push(AppRoutes.appLockSettings);
    }
  }

  void _loadData() {
    final now = DateTime.now();
    final startDate = DateFormat('yyyy-MM-dd').format(now);
    ref.read(bookingsProvider.notifier).loadAppointments(
      status: 'CONFIRMED',
      startDate: startDate,
    );
    ref.read(doctorsProvider.notifier).loadDoctors();
    ref.read(profileProvider.notifier).loadProfile();
  }

  String _getGreeting(String? fullName, AppLocalizations l10n) {
    if (fullName == null || fullName.isEmpty) {
      return '${l10n.hello}, ${l10n.patient}';
    }
    final nameParts = fullName.trim().split(RegExp(r'\s+'));
    final firstName = nameParts.isNotEmpty ? nameParts.first : l10n.patient;
    return '${l10n.hello}, $firstName';
  }

  List<dynamic> _getUpcomingAppointments(List<dynamic> appointments) {
    final now = DateTime.now();
    final withDates = <Map<String, dynamic>>[];
    for (final a in appointments) {
      try {
        final startDate = parseAppointmentDateTime(a.startAt);
        if (startDate.isAfter(now)) {
          withDates.add({'appointment': a, 'date': startDate});
        }
      } catch (_) {}
    }
    withDates.sort((a, b) => a['date'].compareTo(b['date']));
    return withDates.map((e) => e['appointment']).toList();
  }

  /// Doctors within [nearbyDoctorsMaxRadiusKm] of the patient, sorted by distance.
  /// When location is missing, returns empty so we don't show doctors from far away.
  List<DoctorModel> _getNearbyDoctorsWithinRadius(
    List<DoctorModel> doctors,
    double? patientLat,
    double? patientLon,
  ) {
    if (patientLat == null || patientLon == null) return [];
    final withDistance = <(DoctorModel, double)>[];
    for (final d in doctors) {
      if (d.latitude == null || d.longitude == null) continue;
      final km = distanceKm(patientLat, patientLon, d.latitude, d.longitude);
      if (km != null && km <= nearbyDoctorsMaxRadiusKm) withDistance.add((d, km));
    }
    withDistance.sort((a, b) => a.$2.compareTo(b.$2));
    return withDistance.map((e) => e.$1).toList();
  }

  String? _formatDistance(double? km) {
    if (km == null || km.isInfinite) return null;
    if (km < 1) return '${(km * 1000).round()} m';
    return '${km.toStringAsFixed(1)} km';
  }

  Future<void> _fetchCurrentLocation() async {
    if (_isFetchingLocation || !mounted) return;
    setState(() => _isFetchingLocation = true);
    final l10n = AppLocalizations.of(context)!;
    ScaffoldMessenger.of(context).showSnackBar(
      SnackBar(content: Text(l10n.gettingYourLocation), duration: const Duration(seconds: 2)),
    );
    try {
      final enabled = await Geolocator.isLocationServiceEnabled();
      if (!enabled && mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
          SnackBar(content: Text(l10n.translate('locationServicesDisabled')), backgroundColor: Colors.orange),
        );
        setState(() => _isFetchingLocation = false);
        return;
      }
      LocationPermission perm = await Geolocator.checkPermission();
      if (perm == LocationPermission.denied) perm = await Geolocator.requestPermission();
      if (perm == LocationPermission.denied || perm == LocationPermission.deniedForever) {
        if (mounted) {
          ScaffoldMessenger.of(context).showSnackBar(
            SnackBar(content: Text(l10n.translate('locationPermissionDenied')), backgroundColor: Colors.orange),
          );
        }
        setState(() => _isFetchingLocation = false);
        return;
      }
      final pos = await Geolocator.getCurrentPosition(desiredAccuracy: LocationAccuracy.medium);
      if (mounted) {
        setState(() {
          _nearbyLocationLat = pos.latitude;
          _nearbyLocationLon = pos.longitude;
          _isFetchingLocation = false;
        });
        ScaffoldMessenger.of(context).showSnackBar(
          SnackBar(content: Text(l10n.usingCurrentLocation), backgroundColor: Colors.green, duration: const Duration(seconds: 2)),
        );
      }
    } catch (e) {
      if (mounted) {
        setState(() => _isFetchingLocation = false);
        ScaffoldMessenger.of(context).showSnackBar(
          SnackBar(content: Text(l10n.couldNotGetLocationUsingProfile), backgroundColor: Colors.orange),
        );
      }
    }
  }

  @override
  Widget build(BuildContext context) {
    final l10n = AppLocalizations.of(context)!;
    final bookingsState = ref.watch(bookingsProvider);
    final doctorsState = ref.watch(doctorsProvider);
    final profileState = ref.watch(profileProvider);
    final displayPhotoUrl = withCacheBuster(profileState.profile?.photoUrl, profileState.photoCacheKey);

    final upcomingAppointments = _getUpcomingAppointments(bookingsState.appointments);
    final patientLat = _nearbyLocationLat ?? profileState.profile?.latitude;
    final patientLon = _nearbyLocationLon ?? profileState.profile?.longitude;
    final nearbyDoctors = _getNearbyDoctorsWithinRadius(doctorsState.doctors, patientLat, patientLon).take(3).toList();
    final hasLocation = patientLat != null && patientLon != null;

    return Scaffold(
      backgroundColor: AppDesignSystem.background,
      body: RefreshIndicator(
        onRefresh: () async {
          _loadData();
        },
        child: CustomScrollView(
          physics: const AlwaysScrollableScrollPhysics(),
          slivers: [
          SliverToBoxAdapter(
            child: AppHeader(
              title: _getGreeting(profileState.profile?.fullName, l10n),
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
          SliverPadding(
            padding: const EdgeInsets.fromLTRB(
              AppDesignSystem.screenPaddingH,
              AppDesignSystem.sectionToSectionSpacing,
              AppDesignSystem.screenPaddingH,
              0,
            ),
            sliver: SliverList(
              delegate: SliverChildListDelegate([
                SectionTitle(title: l10n.upcomingAppointments),
                if (bookingsState.isLoading)
                  const Center(child: Padding(padding: EdgeInsets.all(24), child: CircularProgressIndicator()))
                else if (upcomingAppointments.isEmpty)
                  EmptyState(
                    icon: Icons.event_available,
                    message: l10n.noUpcomingAppointments,
                    action: TextButton(
                      onPressed: () => context.push(AppRoutes.bookings),
                      child: Text(l10n.bookings, style: TextStyle(color: AppDesignSystem.primary)),
                    ),
                  )
                else ...[
                  ...upcomingAppointments.take(3).map((a) => Padding(
                    padding: const EdgeInsets.only(bottom: AppDesignSystem.cardSpacing),
                    child: AppointmentCard(
                      appointment: a,
                      onTap: () => context.push('${AppRoutes.bookings}/${a.id}'),
                    ),
                  )),
                  if (upcomingAppointments.length > 3)
                    Padding(
                      padding: const EdgeInsets.only(top: 4),
                      child: TextButton(
                        onPressed: () => context.push(AppRoutes.bookings),
                        child: Text(l10n.bookings, style: TextStyle(color: AppDesignSystem.primary)),
                      ),
                    ),
                ],
                const SizedBox(height: AppDesignSystem.sectionToSectionSpacing),
                SectionTitle(title: l10n.myTasks ?? 'My Tasks'),
                RemoteCareTaskCard(
                  title: l10n.remoteCareTasks ?? 'Remote Care Tasks',
                  subtitle: l10n.viewAndCompleteAssignedTasks ?? 'View and complete your assigned tasks',
                  trailingLabel: l10n.viewTasks,
                  onTap: () => context.push(AppRoutes.tasks),
                ),
                const SizedBox(height: AppDesignSystem.sectionToSectionSpacing),
                SectionTitle(
                  title: l10n.doctorsNearMe,
                  trailing: !hasLocation
                      ? TextButton.icon(
                          onPressed: _isFetchingLocation ? null : _fetchCurrentLocation,
                          icon: _isFetchingLocation
                              ? const SizedBox(width: 14, height: 14, child: CircularProgressIndicator(strokeWidth: 2))
                              : Icon(Icons.my_location, size: 16, color: AppDesignSystem.primary),
                          label: Text(l10n.useMyLocation, style: AppDesignSystem.caption),
                        )
                      : null,
                ),
                if (doctorsState.isLoading)
                  const Center(child: Padding(padding: EdgeInsets.all(24), child: CircularProgressIndicator()))
                else if (nearbyDoctors.isEmpty)
                  EmptyState(
                    icon: Icons.medical_services,
                    message: hasLocation ? (l10n.noDoctorsInCloseDistance ?? 'No doctors in close distance') : l10n.noDoctorsAvailable,
                  )
                else
                  ...nearbyDoctors.map((d) {
                    double? km;
                    if (patientLat != null && patientLon != null && d.latitude != null && d.longitude != null) {
                      km = distanceKm(patientLat, patientLon, d.latitude, d.longitude);
                    }
                    final distText = km != null ? _formatDistance(km) : null;
                    return Padding(
                      padding: const EdgeInsets.only(bottom: AppDesignSystem.cardSpacing),
                      child: DoctorCard(
                        doctor: d,
                        distanceText: distText,
                        onTap: () => context.push('${AppRoutes.doctors}/${d.id}'),
                      ),
                    );
                  }),
                SizedBox(height: AppDesignSystem.safeBottomWithNavBar(context)),
              ]),
            ),
          ),
        ],
        ),
      ),
    );
  }
}
