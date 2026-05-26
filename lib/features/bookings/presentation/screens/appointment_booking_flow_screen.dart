import 'dart:async';

import 'package:flutter/material.dart';
import 'package:shifa_patient_app_v1/core/utils/app_logger.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';
import 'package:intl/intl.dart';
import 'package:shifa_patient_app_v1/app/router.dart';
import 'package:shifa_patient_app_v1/core/models/doctor_model.dart';
import 'package:shifa_patient_app_v1/core/models/appointment_model.dart';
import 'package:shifa_patient_app_v1/core/utils/image_utils.dart';
import 'package:shifa_patient_app_v1/features/doctors/providers/doctors_provider.dart';
import 'package:shifa_patient_app_v1/features/doctors/data/doctor_locations_repository.dart';
import 'package:shifa_patient_app_v1/features/bookings/providers/schedule_provider.dart';
import 'package:shifa_patient_app_v1/features/bookings/providers/bookings_provider.dart';
import 'package:shifa_patient_app_v1/features/bookings/data/bookings_repository.dart';
import 'package:shifa_patient_app_v1/features/bookings/data/schedule_repository.dart';
import 'package:shifa_patient_app_v1/core/localization/app_localizations.dart';
import 'package:shifa_patient_app_v1/core/localization/error_localizations.dart';
import 'package:shifa_patient_app_v1/core/theme/app_design_system.dart';
import 'package:shifa_patient_app_v1/core/utils/date_utils.dart' show parseAppointmentDateTime;
import 'package:shifa_patient_app_v1/core/widgets/shifa_button.dart';
import 'package:webview_flutter/webview_flutter.dart';

class AppointmentBookingFlowScreen extends ConsumerStatefulWidget {
  final String doctorId;
  final String? rescheduleId; // ID of appointment to cancel after booking new one

  const AppointmentBookingFlowScreen({
    super.key,
    required this.doctorId,
    this.rescheduleId,
  });

  @override
  ConsumerState<AppointmentBookingFlowScreen> createState() => _AppointmentBookingFlowScreenState();
}

class _AppointmentBookingFlowScreenState extends ConsumerState<AppointmentBookingFlowScreen> {
  DateTime _selectedDate = DateTime.now();
  String? _selectedTime; // This will store the full datetime string from the slot
  bool _isVideoConsultation = false;
  final _reasonController = TextEditingController();
  DoctorModel? _doctor;
  bool _isLoadingDoctor = true;
  bool _isBooking = false;
  int? _selectedServiceId;
  final GlobalKey _serviceSectionKey = GlobalKey();
  bool _highlightServiceSection = false;

  // Multi-location support
  List<PublicDoctorLocation> _locations = const [];
  int? _selectedLocationId;
  bool _locationStepCompleted = false;

  bool get _hasVideoServiceCatalog => _doctor?.serviceItems?.isNotEmpty ?? false;

  @override
  void initState() {
    super.initState();
    _loadDoctor();
    // Load the doctor's practice locations in parallel so we can show the picker
    // before slots when the doctor works at more than one clinic.
    WidgetsBinding.instance.addPostFrameCallback((_) async {
      if (!mounted) return;
      await _loadLocationsThenSlots();
    });
  }

  Future<void> _loadLocationsThenSlots() async {
    try {
      final locs = await ref
          .read(doctorLocationsRepositoryProvider)
          .getLocations(widget.doctorId);
      if (!mounted) return;
      final requiresLocationStep = !_isVideoConsultation && locs.length > 1;
      // For multi-location in-person flow, force an explicit location choice first.
      // For single-location (or video), preserve direct booking behavior.
      PublicDoctorLocation? preselected;
      if (!requiresLocationStep && locs.isNotEmpty) {
        preselected = locs.firstWhere(
          (l) => l.isPrimary,
          orElse: () => locs.first,
        );
      }
      setState(() {
        _locations = locs;
        _selectedLocationId = preselected?.id;
        _locationStepCompleted = !requiresLocationStep;
      });
    } catch (_) {
      // Ignore: booking flow proceeds without a location filter (backend will
      // fall back to the single/primary location for single-location doctors).
      if (mounted) {
        setState(() {
          _locationStepCompleted = true;
        });
      }
    }
    if (!mounted) return;
    if (!_locationStepCompleted) return;
    await _findAndLoadNextAvailableDate();
  }

  @override
  void dispose() {
    _reasonController.dispose();
    super.dispose();
  }

  Future<void> _loadDoctor() async {
    try {
      final doctor = await ref.read(doctorsProvider.notifier).getDoctorById(widget.doctorId);
      setState(() {
        _doctor = doctor;
        _isLoadingDoctor = false;
      });
    } catch (e) {
      setState(() {
        _isLoadingDoctor = false;
      });
      if (mounted) {
        final l10n = AppLocalizations.of(context)!;
        ScaffoldMessenger.of(context).showSnackBar(
          SnackBar(content: Text('${l10n.translate('failedToLoadDoctor')}: ${userFriendlyError(l10n, e, logContext: 'Booking flow')}')),
        );
      }
    }
  }

  Future<void> _loadAvailableSlots() async {
    final dateStr = DateFormat('yyyy-MM-dd').format(_selectedDate);
    await ref.read(scheduleProvider.notifier).loadAvailableSlots(
      doctorId: widget.doctorId,
      day: dateStr,
      locationId: _isVideoConsultation ? null : _selectedLocationId,
    );
  }

  /// Finds the nearest date that has at least one available slot and sets it.
  Future<void> _findAndLoadNextAvailableDate() async {
    final notifier = ref.read(scheduleProvider.notifier);
    final now = DateTime.now();
    final today = DateTime(now.year, now.month, now.day);

    for (int d = 0; d < 30; d++) {
      final date = today.add(Duration(days: d));
      final dateStr = DateFormat('yyyy-MM-dd').format(date);
      final slots = await notifier.loadAvailableSlots(
        doctorId: widget.doctorId,
        day: dateStr,
        locationId: _isVideoConsultation ? null : _selectedLocationId,
      );
      if (!mounted) return;

      final isToday = d == 0;
      final filtered = isToday
          ? slots.where((slot) {
              try {
                return DateTime.parse(slot.startAt).isAfter(now);
              } catch (_) {
                return true;
              }
            }).toList()
          : slots;

      if (filtered.isNotEmpty) {
        setState(() {
          _selectedDate = date;
          _selectedTime = null;
        });
        _handleScheduleUpdate(ref.read(scheduleProvider));
        return;
      }
    }
    // No slots in next 30 days: keep current _selectedDate (today), state already has empty slots
    _handleScheduleUpdate(ref.read(scheduleProvider));
  }

  Future<void> _selectDate(BuildContext context) async {
    final picked = await showDatePicker(
      context: context,
      initialDate: _selectedDate,
      firstDate: DateTime.now(),
      lastDate: DateTime.now().add(const Duration(days: 90)),
    );
    if (picked != null && picked != _selectedDate) {
      setState(() {
        _selectedDate = picked;
        _selectedTime = null; // Reset time when date changes
      });
      _loadAvailableSlots();
    }
  }

  @override
  Widget build(BuildContext context) {
    final l10n = AppLocalizations.of(context)!;
    final scheduleState = ref.watch(scheduleProvider);
    final filteredSlots = _getFilteredSlots(scheduleState);
    final doctorName = _doctor?.fullName ?? l10n.translate('doctor');

    // Listen to schedule changes and auto-select first available slot
    ref.listen<ScheduleState>(
      scheduleProvider,
      (previous, next) {
        // Only handle updates when state actually changes (not on initial build)
        if (previous != next) {
          _handleScheduleUpdate(next);
        }
      },
    );

    // Also check current state on each build to ensure we have a selection
    if (!scheduleState.isLoading && 
        scheduleState.error == null && 
        filteredSlots.isNotEmpty && 
        _selectedTime == null) {
      WidgetsBinding.instance.addPostFrameCallback((_) {
        if (mounted) {
          setState(() {
            _selectedTime = filteredSlots.first.startAt;
          });
        }
      });
    }

    return Scaffold(
      appBar: AppBar(
        leading: IconButton(
          icon: const Icon(Icons.arrow_back),
          onPressed: () => context.pop(),
        ),
        title: Text(doctorName),
      ),
      body: _isLoadingDoctor
          ? const Center(child: CircularProgressIndicator())
          : SingleChildScrollView(
              padding: const EdgeInsets.all(16.0),
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.stretch,
                children: [
                  // Doctor info card
                  if (_doctor != null)
                    Card(
                      child: Padding(
                        padding: const EdgeInsets.all(16.0),
                        child: Row(
                          children: [
                            CircleAvatar(
                              radius: 30,
                              backgroundImage: _doctor!.photoUrl != null
                                  ? NetworkImage(normalizePhotoUrl(_doctor!.photoUrl!) ?? _doctor!.photoUrl!)
                                  : null,
                              child: _doctor!.photoUrl == null
                                  ? Text(
                                      _doctor!.firstName.isNotEmpty
                                          ? _doctor!.firstName[0].toUpperCase()
                                          : 'D',
                                      style: const TextStyle(fontSize: 24),
                                    )
                                  : null,
                            ),
                            const SizedBox(width: 16),
                            Expanded(
                              child: Column(
                                crossAxisAlignment: CrossAxisAlignment.start,
                                children: [
                                  Text(
                                    _doctor!.fullName,
                                    style: const TextStyle(
                                      fontSize: 18,
                                      fontWeight: FontWeight.bold,
                                    ),
                                  ),
                                  if (_doctor!.clinic != null)
                                    Text(
                                      _doctor!.clinic!,
                                      style: const TextStyle(color: Colors.grey),
                                    ),
                                ],
                              ),
                            ),
                          ],
                        ),
                      ),
                    ),
                  const SizedBox(height: 16),
                  // Video consultation toggle. When switching between video and in-person,
                  // slots must be re-fetched because video ignores locationId whereas
                  // in-person requires a specific practice location.
                  Card(
                    child: SwitchListTile(
                      title: Text(AppLocalizations.of(context)!.translate('videoConsultation')),
                      subtitle: Text(AppLocalizations.of(context)!.translate('haveYourAppointment')),
                      value: _isVideoConsultation,
                      onChanged: (value) {
                        final requiresLocationStep = !value && _locations.length > 1;
                        setState(() {
                          _isVideoConsultation = value;
                          _selectedTime = null;
                          _highlightServiceSection = false;
                          if (!value) {
                            _selectedServiceId = null;
                          }
                          if (requiresLocationStep) {
                            _locationStepCompleted = false;
                            _selectedLocationId = null;
                          } else {
                            _locationStepCompleted = true;
                            if (_selectedLocationId == null && _locations.isNotEmpty) {
                              final primary = _locations.firstWhere(
                                (l) => l.isPrimary,
                                orElse: () => _locations.first,
                              );
                              _selectedLocationId = primary.id;
                            }
                          }
                        });
                        if (_locationStepCompleted) {
                          _loadAvailableSlots();
                        }
                      },
                    ),
                  ),
                  if (_isVideoConsultation && _doctor != null) ...[
                    const SizedBox(height: 12),
                    if (!_hasVideoServiceCatalog) ...[
                      Card(
                        color: Theme.of(context).colorScheme.errorContainer.withValues(alpha: 0.35),
                        child: Padding(
                          padding: const EdgeInsets.all(12),
                          child: Row(
                            crossAxisAlignment: CrossAxisAlignment.start,
                            children: [
                              Icon(
                                Icons.info_outline,
                                color: Theme.of(context).colorScheme.onErrorContainer,
                              ),
                              const SizedBox(width: 12),
                              Expanded(
                                child: Text(
                                  l10n.translate('bookingVideoNoServicesOffered'),
                                  style: TextStyle(
                                    color: Theme.of(context).colorScheme.onErrorContainer,
                                    height: 1.35,
                                  ),
                                ),
                              ),
                            ],
                          ),
                        ),
                      ),
                    ] else ...[
                      Card(
                        key: _serviceSectionKey,
                        shape: RoundedRectangleBorder(
                          borderRadius: BorderRadius.circular(12),
                          side: BorderSide(
                            color: _highlightServiceSection
                                ? Colors.orange
                                : Theme.of(context).dividerColor.withValues(alpha: 0.25),
                            width: _highlightServiceSection ? 2.5 : 1,
                          ),
                        ),
                        child: Padding(
                          padding: const EdgeInsets.all(12),
                          child: Column(
                            crossAxisAlignment: CrossAxisAlignment.start,
                            children: [
                              Text(
                                l10n.translate('videoConsultationServiceLabel'),
                                style: const TextStyle(fontSize: 15, fontWeight: FontWeight.w700),
                              ),
                              const SizedBox(height: 8),
                              DropdownButtonFormField<int>(
                                value: _selectedServiceId,
                                items: (_doctor!.serviceItems ?? const [])
                                    .map((s) => DropdownMenuItem<int>(
                                          value: int.tryParse(s.id),
                                          child: Text(
                                            s.isFreeConsultation
                                                ? '${s.title} — ${l10n.translate('consultationServiceFreeBadge')}'
                                                : s.title,
                                          ),
                                        ))
                                    .toList(),
                                onChanged: (v) => setState(() {
                                  _selectedServiceId = v;
                                  _highlightServiceSection = false;
                                }),
                                decoration: InputDecoration(
                                  border: const OutlineInputBorder(),
                                  hintText: l10n
                                      .translate('videoConsultationServiceHint'),
                                ),
                              ),
                            ],
                          ),
                        ),
                      ),
                    ],
                  ],
                  const SizedBox(height: 16),
                  // Date selector
                  if (_locationStepCompleted)
                  Card(
                    child: Padding(
                      padding: const EdgeInsets.all(16.0),
                      child: Column(
                        children: [
                          Row(
                            mainAxisAlignment: MainAxisAlignment.spaceBetween,
                            children: [
                              Text(
                                l10n.translate('selectDate'),
                                style: const TextStyle(fontWeight: FontWeight.bold, fontSize: 16),
                              ),
                              TextButton.icon(
                                onPressed: () => _selectDate(context),
                                icon: const Icon(Icons.calendar_today),
                                label: Text(
                                  DateFormat('MMM dd, yyyy', l10n.locale.languageCode).format(_selectedDate),
                                  style: const TextStyle(fontWeight: FontWeight.bold),
                                ),
                              ),
                            ],
                          ),
                          const SizedBox(height: 8),
                          Text(
                            DateFormat('EEEE', l10n.locale.languageCode).format(_selectedDate),
                            style: const TextStyle(color: Colors.grey),
                          ),
                        ],
                      ),
                    ),
                  ),
                  if (_locationStepCompleted) const SizedBox(height: 16),
                  // Location picker (only when the doctor has multiple in-person locations
                  // AND the user isn't booking a video consultation). Shown BEFORE the
                  // time slots so the patient picks the clinic first.
                  if (!_isVideoConsultation && _locations.length > 1) ...[
                    _LocationPicker(
                      locations: _locations,
                      selectedId: _selectedLocationId,
                      onChanged: (id) {
                        if (id == _selectedLocationId) return;
                        setState(() {
                          _selectedLocationId = id;
                          _selectedTime = null;
                        });
                        if (_locationStepCompleted) {
                          _loadAvailableSlots();
                        }
                      },
                    ),
                    const SizedBox(height: 16),
                    if (!_locationStepCompleted)
                      ShifaPrimaryButton(
                        label: l10n.next,
                        onPressed: _selectedLocationId == null
                            ? null
                            : () async {
                                setState(() {
                                  _locationStepCompleted = true;
                                  _selectedTime = null;
                                });
                                await _findAndLoadNextAvailableDate();
                              },
                      ),
                    if (!_locationStepCompleted) const SizedBox(height: 16),
                  ],
                  // Time slots
                  if (_locationStepCompleted)
                  Card(
                    child: Padding(
                      padding: const EdgeInsets.all(16.0),
                      child: Column(
                        crossAxisAlignment: CrossAxisAlignment.start,
                        children: [
                          Text(
                            l10n.translate('availableTimes'),
                            style: const TextStyle(fontWeight: FontWeight.bold, fontSize: 16),
                          ),
                          const SizedBox(height: 16),
                          if (scheduleState.isLoading)
                            const Center(
                              child: Padding(
                                padding: EdgeInsets.all(16.0),
                                child: CircularProgressIndicator(),
                              ),
                            )
                          else if (scheduleState.error != null)
                            Text(
                              '${l10n.translate('errorLoadingSlots')}: ${scheduleState.error}',
                              style: const TextStyle(color: Colors.red),
                            )
                          else if (filteredSlots.isEmpty)
                            Text(
                              l10n.translate('noAvailableTimeSlots'),
                              style: const TextStyle(color: Colors.grey),
                            )
                          else
                            Wrap(
                              spacing: 8,
                              runSpacing: 8,
                              children: filteredSlots.map((slot) {
                                // Backend sends UTC (e.g. 2026-02-28T08:00:00Z); show in device local time
                                String timeStr;
                                try {
                                  final localDt = parseAppointmentDateTime(slot.startAt);
                                  timeStr = DateFormat('HH:mm').format(localDt);
                                } catch (e) {
                                  if (slot.startAt.contains('T')) {
                                    timeStr = slot.startAt.split('T')[1].substring(0, 5);
                                  } else {
                                    timeStr = slot.startAt;
                                  }
                                }
                                return _buildTimeChip(timeStr, slot.startAt);
                              }).toList(),
                            ),
                        ],
                      ),
                    ),
                  ),
                  if (_locationStepCompleted) const SizedBox(height: 16),
                  // Reason for visit
                  if (_locationStepCompleted)
                  Card(
                    child: Padding(
                      padding: const EdgeInsets.all(16.0),
                      child: Column(
                        crossAxisAlignment: CrossAxisAlignment.start,
                        children: [
                          Text(
                            l10n.translate('reasonForVisitOptional'),
                            style: const TextStyle(fontWeight: FontWeight.bold, fontSize: 16),
                          ),
                          const SizedBox(height: 12),
                          TextField(
                            controller: _reasonController,
                            decoration: InputDecoration(
                              hintText: l10n.translate('describeYourReason'),
                              border: OutlineInputBorder(
                                borderRadius: BorderRadius.circular(12),
                              ),
                            ),
                            maxLines: 3,
                          ),
                        ],
                      ),
                    ),
                  ),
                  if (_locationStepCompleted) const SizedBox(height: 24),
                  // Confirm button
                  if (_locationStepCompleted)
                  ShifaPrimaryButton(
                    label: l10n.translate('confirm'),
                    onPressed: (_selectedTime == null || _isBooking)
                        ? null
                        : () async {
                            await _confirmBooking();
                          },
                    isLoading: _isBooking,
                  ),
                ],
              ),
            ),
    );
  }

  List<AvailableSlot> _getFilteredSlots(ScheduleState state) {
    final now = DateTime.now();
    final isToday = DateUtils.isSameDay(_selectedDate, now);
    if (!isToday) return state.availableSlots;

    return state.availableSlots.where((slot) {
      try {
        final slotTime = DateTime.parse(slot.startAt);
        return slotTime.isAfter(now);
      } catch (_) {
        return true;
      }
    }).toList();
  }

  void _handleScheduleUpdate(ScheduleState next) {
    // Only auto-select when loading is complete and there's no error
    if (!next.isLoading && next.error == null && next.availableSlots.isNotEmpty) {
      final filtered = _getFilteredSlots(next);
      if (filtered.isNotEmpty) {
        // Auto-select first available slot if none is selected or current selection is invalid
        final shouldUpdateSelection = _selectedTime == null ||
            !filtered.any((slot) => slot.startAt == _selectedTime);
        if (shouldUpdateSelection && mounted) {
          WidgetsBinding.instance.addPostFrameCallback((_) {
            if (mounted) {
              setState(() {
                _selectedTime = filtered.first.startAt;
              });
            }
          });
        }
      } else if (_selectedTime != null && mounted) {
        // Clear selection if no valid slots available
        WidgetsBinding.instance.addPostFrameCallback((_) {
          if (mounted) {
            setState(() {
              _selectedTime = null;
            });
          }
        });
      }
    }
  }

  void _scrollServiceSectionIntoView() {
    WidgetsBinding.instance.addPostFrameCallback((_) {
      if (!mounted) return;
      final ctx = _serviceSectionKey.currentContext;
      if (ctx != null) {
        Scrollable.ensureVisible(
          ctx,
          duration: const Duration(milliseconds: 400),
          curve: Curves.easeInOut,
          alignment: 0.12,
        );
      }
    });
  }

  /// Video consultations require [serviceId] on the server. Validates before API call
  /// and guides the user to the service picker.
  bool _validateVideoConsultationService(AppLocalizations l10n) {
    if (!_isVideoConsultation || _doctor == null) return true;
    if (!mounted) return false;

    if (!_hasVideoServiceCatalog) {
      ScaffoldMessenger.of(context).showSnackBar(
        SnackBar(
          content: Text(l10n.translate('bookingVideoNoServicesOffered')),
          backgroundColor: Colors.orange.shade800,
          duration: const Duration(seconds: 5),
        ),
      );
      return false;
    }

    if (_selectedServiceId == null) {
      setState(() => _highlightServiceSection = true);
      _scrollServiceSectionIntoView();
      ScaffoldMessenger.of(context).showSnackBar(
        SnackBar(
          content: Text(l10n.translate('bookingSelectServiceForVideo')),
          backgroundColor: Colors.orange.shade800,
          duration: const Duration(seconds: 4),
        ),
      );
      return false;
    }

    return true;
  }

  Future<void> _confirmBooking() async {
    if (_selectedTime == null) return;

    final l10n = AppLocalizations.of(context)!;
    if (!_validateVideoConsultationService(l10n)) {
      return;
    }

    setState(() {
      _isBooking = true;
    });

    try {
      AppLogger.debug('Booking appointment - Selected time: $_selectedTime');
      // startAt is ISO 8601 UTC from the slot (Shifa Global Time Architecture v2)
      final startAtUtc = _selectedTime!;
      final scheduleState = ref.read(scheduleProvider);
      AvailableSlot selectedSlot;
      try {
        selectedSlot = scheduleState.availableSlots.firstWhere(
          (slot) => slot.startAt == startAtUtc,
        );
      } catch (e) {
        selectedSlot = AvailableSlot(
          startAt: startAtUtc,
          endAt: '',
          slotMinutes: 30,
        );
      }

      // Check for double booking (compare UTC instants)
      final bookingsState = ref.read(bookingsProvider);
      final hasConflict = bookingsState.appointments.any((apt) {
        if (apt.status == AppointmentStatus.cancelled) return false;
        try {
          return apt.startAt == startAtUtc;
        } catch (_) {
          return false;
        }
      });
      if (hasConflict) {
        throw Exception('You already have an appointment scheduled at this date and time. Please choose a different time slot.');
      }

      // Prefer the locationId that came back on the slot itself (authoritative for
      // this exact slot). Fall back to the patient's selection, which the backend
      // will validate.
      final bookingLocationId = _isVideoConsultation
          ? null
          : (selectedSlot.locationId ?? _selectedLocationId);

      AppLogger.debug(
          'Booking params - startAt: $startAtUtc, slotMinutes: ${selectedSlot.slotMinutes}, doctorId: ${widget.doctorId}, locationId: $bookingLocationId');
      final bookedAppointment = await ref.read(bookingsProvider.notifier).bookAppointment(
        doctorId: widget.doctorId,
        startAt: startAtUtc,
        slotMinutes: selectedSlot.slotMinutes,
        reason: _reasonController.text.isEmpty ? null : _reasonController.text,
        isVideo: _isVideoConsultation,
        serviceId: _isVideoConsultation ? _selectedServiceId : null,
        locationId: bookingLocationId,
      );

      AppLogger.debug('Appointment booked successfully!');

      var paymentCompleted = bookedAppointment.paymentStatus.toUpperCase() != 'PENDING';
      String? createdPaymentId;
      String? createdCheckoutUrl;
      String? createdCheckoutGateway;
      if (bookedAppointment.paymentStatus.toUpperCase() == 'PENDING') {
        final checkout = await ref.read(bookingsRepositoryProvider).createConsultationCheckout(
          appointmentId: bookedAppointment.id,
        );
        createdPaymentId = checkout['paymentId']?.toString();
        createdCheckoutUrl = checkout['checkoutUrl']?.toString();
        createdCheckoutGateway = checkout['gateway']?.toString();
        if (createdCheckoutUrl != null && createdCheckoutUrl.isNotEmpty && createdPaymentId != null) {
          final paid = await Navigator.of(context).push<bool>(
            MaterialPageRoute(
              builder: (_) => _PaymentCheckoutScreen(
                appointmentId: bookedAppointment.id,
                checkoutGateway: createdCheckoutGateway,
                checkoutUrl: createdCheckoutUrl!,
                paymentId: createdPaymentId!,
              ),
            ),
          );
          paymentCompleted = paid == true;
        }
      }

      // If this is a reschedule, cancel the old appointment
      if (widget.rescheduleId != null && widget.rescheduleId!.isNotEmpty) {
        try {
          AppLogger.debug('Cancelling old appointment: ${widget.rescheduleId}');
          await ref.read(bookingsProvider.notifier).cancelAppointment(widget.rescheduleId!);
          AppLogger.debug('Old appointment cancelled successfully');
        } catch (e) {
          AppLogger.error('Error cancelling old appointment:', e);
          // Don't fail the whole operation if cancel fails
          if (mounted) {
            final l10n = AppLocalizations.of(context)!;
            ScaffoldMessenger.of(context).showSnackBar(
              SnackBar(
                content: Text('${l10n.translate('newAppointmentBookedButFailedToCancelOld')}: ${userFriendlyError(l10n, e, logContext: 'Booking flow')}'),
                backgroundColor: Colors.orange,
                duration: const Duration(seconds: 4),
              ),
            );
          }
        }
      }

      // Show success message only once payment is completed.
      if (mounted) {
        final l10n = AppLocalizations.of(context)!;
        if (paymentCompleted) {
          ScaffoldMessenger.of(context).showSnackBar(
            SnackBar(
              content: Text(widget.rescheduleId != null
                  ? l10n.appointmentRescheduledSuccessfully
                  : (l10n.appointmentSlotBooked ?? l10n.translate('appointmentSlotBooked'))),
              backgroundColor: Colors.green,
              duration: const Duration(seconds: 2),
            ),
          );
          Future.delayed(const Duration(milliseconds: 500), () {
            if (mounted) {
              context.go(AppRoutes.bookings);
            }
          });
        } else {
          if (createdPaymentId != null) {
            final paidFromPending = await Navigator.of(context).push<bool>(
              MaterialPageRoute(
                builder: (_) => _PaymentPendingScreen(
                  appointmentId: bookedAppointment.id,
                  checkoutGateway: createdCheckoutGateway,
                  paymentId: createdPaymentId!,
                  checkoutUrl: createdCheckoutUrl,
                ),
              ),
            );
            paymentCompleted = paidFromPending == true;
          }

          if (paymentCompleted) {
            ScaffoldMessenger.of(context).showSnackBar(
              SnackBar(
                content: Text(widget.rescheduleId != null
                    ? l10n.appointmentRescheduledSuccessfully
                    : (l10n.appointmentSlotBooked ?? l10n.translate('appointmentSlotBooked'))),
                backgroundColor: Colors.green,
                duration: const Duration(seconds: 2),
              ),
            );
            Future.delayed(const Duration(milliseconds: 500), () {
              if (mounted) {
                context.go(AppRoutes.bookings);
              }
            });
            return;
          }

          setState(() {
            _isBooking = false;
          });
          ScaffoldMessenger.of(context).showSnackBar(
            SnackBar(
              content: Text(l10n.translate('paymentStillPendingConfirmBooking')),
              backgroundColor: Colors.orange,
              duration: Duration(seconds: 4),
            ),
          );
        }
      }
    } catch (e, stackTrace) {
      AppLogger.error('Booking error:', e, stackTrace);
      
      setState(() {
        _isBooking = false;
      });
      
      if (mounted) {
        final l10n = AppLocalizations.of(context)!;
        final friendly = userFriendlyError(l10n, e, logContext: 'Booking flow');
        final selectServiceMsg = l10n.translate('bookingSelectServiceForVideo');
        final isSelectServiceCase = friendly == selectServiceMsg ||
            e.toString().contains('serviceId is required for video consultation');
        ScaffoldMessenger.of(context).showSnackBar(
          SnackBar(
            content: Text(
              isSelectServiceCase
                  ? friendly
                  : '${l10n.translate('failedToBookAppointment')}: $friendly',
            ),
            backgroundColor: isSelectServiceCase ? Colors.orange.shade800 : Colors.red,
            duration: const Duration(seconds: 4),
          ),
        );
      }
    }
  }

  Widget _buildTimeChip(String time, String fullDateTime) {
    final isSelected = _selectedTime == fullDateTime;
    return ChoiceChip(
      label: Text(time),
      selected: isSelected,
      onSelected: (selected) {
        setState(() {
          _selectedTime = selected ? fullDateTime : null;
        });
      },
      selectedColor: AppDesignSystem.primary,
      labelStyle: TextStyle(
        color: isSelected ? Colors.white : Colors.black,
        fontWeight: isSelected ? FontWeight.bold : FontWeight.normal,
      ),
    );
  }
}

class _PaymentCheckoutScreen extends ConsumerStatefulWidget {
  const _PaymentCheckoutScreen({
    required this.appointmentId,
    required this.checkoutUrl,
    required this.paymentId,
    this.checkoutGateway,
  });

  final String appointmentId;
  final String checkoutUrl;
  final String paymentId;
  /// From backend checkout response (`CLICK` vs `STRIPE`). Used for client-side Stripe fallback.
  final String? checkoutGateway;

  @override
  ConsumerState<_PaymentCheckoutScreen> createState() => _PaymentCheckoutScreenState();
}

class _PaymentCheckoutScreenState extends ConsumerState<_PaymentCheckoutScreen> {
  late final WebViewController _controller;
  late String _pollPaymentId;
  Timer? _pollingTimer;
  bool _checkingStatus = false;
  bool _stripeReloadTried = false;

  @override
  void initState() {
    super.initState();
    _pollPaymentId = widget.paymentId;
    _controller = WebViewController()
      ..setJavaScriptMode(JavaScriptMode.unrestricted)
      ..setNavigationDelegate(
        NavigationDelegate(
          onNavigationRequest: (request) {
            final url = request.url;
            if (url.contains('/api/payments/checkout/success')) {
              _checkPaymentStatus(forceCloseOnPaid: true);
              return NavigationDecision.prevent;
            }
            if (url.contains('/api/payments/checkout/cancel')) {
              if (mounted) Navigator.of(context).pop(false);
              return NavigationDecision.prevent;
            }
            return NavigationDecision.navigate;
          },
          onWebResourceError: (WebResourceError error) {
            unawaited(_maybeStripeFallbackAfterMainFrameFailure(error));
          },
        ),
      )
      ..loadRequest(Uri.parse(widget.checkoutUrl));

    _pollingTimer = Timer.periodic(
      const Duration(seconds: 3),
      (_) => _checkPaymentStatus(forceCloseOnPaid: true),
    );
  }

  Future<void> _maybeStripeFallbackAfterMainFrameFailure(WebResourceError error) async {
    if (_stripeReloadTried ||
        !mounted ||
        error.isForMainFrame != true ||
        widget.checkoutGateway != 'CLICK') {
      return;
    }
    _stripeReloadTried = true;
    try {
      final checkout = await ref.read(bookingsRepositoryProvider).createConsultationCheckout(
            appointmentId: widget.appointmentId,
            gateway: 'STRIPE',
          );
      final url = checkout['checkoutUrl']?.toString();
      final newId = checkout['paymentId']?.toString();
      if (!mounted || url == null || url.isEmpty || newId == null) return;
      setState(() => _pollPaymentId = newId);
      await _controller.loadRequest(Uri.parse(url));
    } catch (_) {
      // Silent: backend usually falls back automatically; WebView failure is uncommon.
    }
  }

  Future<void> _checkPaymentStatus({bool forceCloseOnPaid = false}) async {
    if (_checkingStatus || !mounted) return;
    _checkingStatus = true;
    try {
      final status = await ref.read(bookingsRepositoryProvider).getPaymentStatus(_pollPaymentId);
      final paymentStatus = (status['status']?.toString() ?? '').toUpperCase();
      if (paymentStatus == 'PAID' && mounted && forceCloseOnPaid) {
        Navigator.of(context).pop(true);
      }
    } catch (_) {
      // Ignore transient network issues while polling.
    } finally {
      _checkingStatus = false;
    }
  }

  @override
  void dispose() {
    _pollingTimer?.cancel();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    final l10n = AppLocalizations.of(context)!;
    return Scaffold(
      appBar: AppBar(
        title: Text(l10n.translate('completePayment')),
      ),
      body: WebViewWidget(controller: _controller),
    );
  }
}

class _PaymentPendingScreen extends ConsumerStatefulWidget {
  const _PaymentPendingScreen({
    required this.appointmentId,
    required this.paymentId,
    this.checkoutUrl,
    this.checkoutGateway,
  });

  final String appointmentId;
  final String paymentId;
  final String? checkoutUrl;
  final String? checkoutGateway;

  @override
  ConsumerState<_PaymentPendingScreen> createState() => _PaymentPendingScreenState();
}

class _PaymentPendingScreenState extends ConsumerState<_PaymentPendingScreen> {
  bool _checking = false;
  String _status = 'PENDING';

  Future<void> _checkStatus() async {
    if (_checking) return;
    setState(() => _checking = true);
    try {
      final status = await ref.read(bookingsRepositoryProvider).getPaymentStatus(widget.paymentId);
      final paymentStatus = (status['status']?.toString() ?? '').toUpperCase();
      setState(() => _status = paymentStatus);
      if (paymentStatus == 'PAID' && mounted) {
        Navigator.of(context).pop(true);
      }
    } catch (_) {
      if (mounted) {
        final l10n = AppLocalizations.of(context)!;
        ScaffoldMessenger.of(context).showSnackBar(
          SnackBar(content: Text(l10n.translate('couldNotRefreshPaymentStatus'))),
        );
      }
    } finally {
      if (mounted) setState(() => _checking = false);
    }
  }

  Future<void> _continuePayment() async {
    final url = widget.checkoutUrl;
    if (url == null || url.isEmpty) return;
    final paid = await Navigator.of(context).push<bool>(
      MaterialPageRoute(
        builder: (_) => _PaymentCheckoutScreen(
          appointmentId: widget.appointmentId,
          checkoutGateway: widget.checkoutGateway,
          checkoutUrl: url,
          paymentId: widget.paymentId,
        ),
      ),
    );
    if (paid == true && mounted) {
      Navigator.of(context).pop(true);
    }
  }

  @override
  void initState() {
    super.initState();
    _checkStatus();
  }

  @override
  Widget build(BuildContext context) {
    final l10n = AppLocalizations.of(context)!;
    return Scaffold(
      appBar: AppBar(title: Text(l10n.translate('paymentPendingTitle'))),
      body: Padding(
        padding: const EdgeInsets.all(16),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.stretch,
          children: [
            Text(
              l10n.translate('paymentPendingMessage'),
              style: TextStyle(fontSize: 16),
            ),
            const SizedBox(height: 12),
            Text(
              l10n
                  .translate('currentPaymentStatus')
                  .replaceAll('{{status}}', _status),
            ),
            const SizedBox(height: 24),
            ShifaPrimaryButton(
              label: _checking
                  ? l10n.translate('checking')
                  : l10n.translate('checkPaymentStatus'),
              onPressed: _checking ? null : _checkStatus,
              isLoading: _checking,
            ),
            const SizedBox(height: 12),
            ShifaPrimaryButton(
              label: l10n.translate('continuePayment'),
              onPressed: (widget.checkoutUrl == null || widget.checkoutUrl!.isEmpty) ? null : _continuePayment,
            ),
            const SizedBox(height: 12),
            TextButton(
              onPressed: () => Navigator.of(context).pop(false),
              child: Text(l10n.translate('backToBookings')),
            ),
          ],
        ),
      ),
    );
  }
}

/// Presents the doctor's practice locations as a single-select list so the
/// patient must pick one before the time slots are filtered to that clinic.
class _LocationPicker extends StatelessWidget {
  const _LocationPicker({
    required this.locations,
    required this.selectedId,
    required this.onChanged,
  });

  final List<PublicDoctorLocation> locations;
  final int? selectedId;
  final ValueChanged<int?> onChanged;

  @override
  Widget build(BuildContext context) {
    final l10n = AppLocalizations.of(context)!;
    return Card(
      child: Padding(
        padding: const EdgeInsets.all(16.0),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Row(
              children: [
                const Icon(Icons.place_outlined),
                const SizedBox(width: 8),
                Text(
                  l10n.translate('selectLocation'),
                  style: const TextStyle(
                    fontWeight: FontWeight.bold,
                    fontSize: 16,
                  ),
                ),
              ],
            ),
            const SizedBox(height: 12),
            ...locations.map((loc) {
              final isSelected = loc.id == selectedId;
              return Padding(
                padding: const EdgeInsets.only(bottom: 8),
                child: InkWell(
                  borderRadius: BorderRadius.circular(10),
                  onTap: () => onChanged(loc.id),
                  child: Container(
                    padding: const EdgeInsets.symmetric(
                      horizontal: 12,
                      vertical: 10,
                    ),
                    decoration: BoxDecoration(
                      color: isSelected
                          ? AppDesignSystem.primary.withOpacity(0.08)
                          : Colors.transparent,
                      border: Border.all(
                        color: isSelected
                            ? AppDesignSystem.primary
                            : Colors.grey.shade300,
                      ),
                      borderRadius: BorderRadius.circular(10),
                    ),
                    child: Row(
                      crossAxisAlignment: CrossAxisAlignment.start,
                      children: [
                        Radio<int?>(
                          value: loc.id,
                          groupValue: selectedId,
                          onChanged: onChanged,
                        ),
                        Expanded(
                          child: Column(
                            crossAxisAlignment: CrossAxisAlignment.start,
                            children: [
                              Text(
                                loc.label +
                                    (loc.isPrimary
                                        ? ' · ${l10n.translate('primary')}'
                                        : ''),
                                style: const TextStyle(
                                  fontWeight: FontWeight.w600,
                                ),
                              ),
                              if (loc.displaySubtitle.isNotEmpty)
                                Padding(
                                  padding: const EdgeInsets.only(top: 2),
                                  child: Text(
                                    loc.displaySubtitle,
                                    style: TextStyle(
                                      color: Colors.grey.shade700,
                                      fontSize: 13,
                                    ),
                                  ),
                                ),
                            ],
                          ),
                        ),
                      ],
                    ),
                  ),
                ),
              );
            }),
          ],
        ),
      ),
    );
  }
}
