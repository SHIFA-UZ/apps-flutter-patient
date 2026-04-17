import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';
import 'package:intl/intl.dart';
import 'package:shifa_patient_app_v1/app/router.dart';
import 'package:shifa_patient_app_v1/core/localization/app_localizations.dart';
import 'package:shifa_patient_app_v1/core/theme/app_design_system.dart';
import 'package:shifa_patient_app_v1/core/utils/date_utils.dart' as app_date_utils;
import 'package:shifa_patient_app_v1/core/utils/image_utils.dart';
import 'package:shifa_patient_app_v1/core/widgets/app_header.dart';
import 'package:shifa_patient_app_v1/core/widgets/appointment_card.dart';
import 'package:shifa_patient_app_v1/core/widgets/shifa_button.dart';
import 'package:shifa_patient_app_v1/core/widgets/empty_state.dart';
import 'package:shifa_patient_app_v1/core/widgets/segmented_control.dart';
import 'package:shifa_patient_app_v1/features/bookings/providers/bookings_provider.dart';
import 'package:shifa_patient_app_v1/features/profile/providers/profile_provider.dart';

class BookingsScreen extends ConsumerStatefulWidget {
  const BookingsScreen({super.key});

  @override
  ConsumerState<BookingsScreen> createState() => _BookingsScreenState();
}

class _BookingsScreenState extends ConsumerState<BookingsScreen> {
  int _selectedTab = 0; // 0 = Upcoming, 1 = Past

  @override
  void initState() {
    super.initState();
    // Load appointments on init
    WidgetsBinding.instance.addPostFrameCallback((_) {
      _loadAppointments();
      ref.read(profileProvider.notifier).loadProfile();
    });
  }

  void _loadAppointments() {
    final now = DateTime.now();
    final startDate = DateFormat('yyyy-MM-dd').format(now);
    
    if (_selectedTab == 0) {
      // Upcoming appointments - fetch CONFIRMED appointments from today onwards
      ref.read(bookingsProvider.notifier).loadAppointments(
        status: 'CONFIRMED',
        startDate: startDate,
      );
    } else {
      // Past appointments - fetch all appointments (no status filter) and filter by date client-side
      // This ensures we get all past appointments regardless of status (CONFIRMED, COMPLETED, etc.)
      ref.read(bookingsProvider.notifier).loadAppointments();
    }
  }

  // Memoize filtered and sorted appointments to avoid recomputing on every build
  List<dynamic> _getFilteredAppointments(List<dynamic> appointments, int selectedTab) {
    final now = DateTime.now();
    // Parse dates once and store with appointments
    final appointmentsWithDates = <Map<String, dynamic>>[];
    for (final appointment in appointments) {
      try {
        final startDate = app_date_utils.parseAppointmentDateTime(appointment.startAt);
        final isUpcoming = startDate.isAfter(now);
        
        if (selectedTab == 0 && isUpcoming) {
          // Upcoming: show all future appointments, including cancelled ones
          appointmentsWithDates.add({'appointment': appointment, 'date': startDate});
        } else if (selectedTab == 1 && !isUpcoming) {
          // Past: show all past appointments, including cancelled ones
          appointmentsWithDates.add({'appointment': appointment, 'date': startDate});
        }
      } catch (e) {
        // Skip invalid dates
      }
    }
    
    // Sort by date
    if (selectedTab == 0) {
      // Upcoming: sort ascending (earliest first)
      appointmentsWithDates.sort((a, b) => a['date'].compareTo(b['date']));
    } else {
      // Past: sort descending (most recent first)
      appointmentsWithDates.sort((a, b) => b['date'].compareTo(a['date']));
    }
    
    return appointmentsWithDates.map((item) => item['appointment']).toList();
  }

  @override
  Widget build(BuildContext context) {
    final l10n = AppLocalizations.of(context)!;
    final bookingsState = ref.watch(bookingsProvider);
    final profileState = ref.watch(profileProvider);
    final displayPhotoUrl = withCacheBuster(
      profileState.profile?.photoUrl,
      profileState.photoCacheKey,
    );

    // Use memoized computation for appointments
    final filteredAppointments = _getFilteredAppointments(bookingsState.appointments, _selectedTab);

    return Scaffold(
      backgroundColor: AppDesignSystem.background,
      body: RefreshIndicator(
        onRefresh: () async {
          ref.read(profileProvider.notifier).loadProfile();
          _loadAppointments();
        },
        child: CustomScrollView(
          physics: const AlwaysScrollableScrollPhysics(),
          slivers: [
          SliverToBoxAdapter(
            child: AppHeader(
              title: l10n.bookings,
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
                labels: [l10n.upcoming, l10n.past],
                onSelected: (index) {
                  setState(() => _selectedTab = index);
                  _loadAppointments();
                },
              ),
            ),
          ),
          if (bookingsState.isLoading)
            const SliverToBoxAdapter(
              child: Padding(
                padding: EdgeInsets.all(32),
                child: Center(child: CircularProgressIndicator()),
              ),
            )
          else if (bookingsState.error != null)
            SliverToBoxAdapter(
              child: EmptyState(
                icon: Icons.error_outline,
                message: '${l10n.error}: ${bookingsState.error}',
                action: ShifaPrimaryButton(
                  label: l10n.retry,
                  onPressed: _loadAppointments,
                  width: ButtonWidth.hug,
                ),
              ),
            )
          else if (filteredAppointments.isEmpty)
            SliverToBoxAdapter(
              child: EmptyState(
                icon: Icons.event_busy,
                message: l10n.noAppointmentsFound,
                action: TextButton(
                  onPressed: () => context.push(AppRoutes.createBooking),
                  child: Text(l10n.createBooking, style: TextStyle(color: AppDesignSystem.primary)),
                ),
              ),
            )
          else
            SliverPadding(
              padding: const EdgeInsets.symmetric(horizontal: AppDesignSystem.screenPaddingH),
              sliver: SliverList(
                delegate: SliverChildBuilderDelegate(
                  (context, index) {
                    final appointment = filteredAppointments[index];
                    return Padding(
                      padding: const EdgeInsets.only(bottom: AppDesignSystem.cardSpacing),
                      child: AppointmentCard(
                        appointment: appointment,
                        onTap: () => context.push('${AppRoutes.bookings}/${appointment.id}'),
                      ),
                    );
                  },
                  childCount: filteredAppointments.length,
                ),
              ),
            ),
          SliverToBoxAdapter(
            child: SizedBox(height: AppDesignSystem.safeBottomWithNavBar(context)),
          ),
        ],
        ),
      ),
      floatingActionButton: FloatingActionButton.extended(
        onPressed: () => context.push(AppRoutes.createBooking),
        backgroundColor: AppDesignSystem.primary,
        label: Text(l10n.createBooking),
        icon: const Icon(Icons.add),
        heroTag: 'bookings_fab',
      ),
      floatingActionButtonLocation: FloatingActionButtonLocation.centerFloat,
    );
  }
}
