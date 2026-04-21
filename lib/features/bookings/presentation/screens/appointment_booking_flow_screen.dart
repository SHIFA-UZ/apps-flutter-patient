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
import 'package:shifa_patient_app_v1/features/bookings/data/schedule_repository.dart';
import 'package:shifa_patient_app_v1/core/localization/app_localizations.dart';
import 'package:shifa_patient_app_v1/core/localization/error_localizations.dart';
import 'package:shifa_patient_app_v1/core/theme/app_design_system.dart';
import 'package:shifa_patient_app_v1/core/utils/date_utils.dart' show parseAppointmentDateTime;
import 'package:shifa_patient_app_v1/core/widgets/shifa_button.dart';

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

  // Multi-location support
  List<PublicDoctorLocation> _locations = const [];
  int? _selectedLocationId;
  bool _locationStepCompleted = false;

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

  Future<void> _confirmBooking() async {
    if (_selectedTime == null) return;

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
      await ref.read(bookingsProvider.notifier).bookAppointment(
        doctorId: widget.doctorId,
        startAt: startAtUtc,
        slotMinutes: selectedSlot.slotMinutes,
        reason: _reasonController.text.isEmpty ? null : _reasonController.text,
        isVideo: _isVideoConsultation,
        locationId: bookingLocationId,
      );

      AppLogger.debug('Appointment booked successfully!');

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

      // Show success message
      if (mounted) {
        final l10n = AppLocalizations.of(context)!;
        ScaffoldMessenger.of(context).showSnackBar(
          SnackBar(
            content: Text(widget.rescheduleId != null
                ? l10n.appointmentRescheduledSuccessfully
                : (l10n.appointmentSlotBooked ?? l10n.translate('appointmentSlotBooked'))),
            backgroundColor: Colors.green,
            duration: const Duration(seconds: 2),
          ),
        );

        // Navigate to bookings page after a short delay
        Future.delayed(const Duration(milliseconds: 500), () {
          if (mounted) {
            context.go(AppRoutes.bookings);
          }
        });
      }
    } catch (e, stackTrace) {
      AppLogger.error('Booking error:', e, stackTrace);
      
      setState(() {
        _isBooking = false;
      });
      
      if (mounted) {
        final l10n = AppLocalizations.of(context)!;
        ScaffoldMessenger.of(context).showSnackBar(
          SnackBar(
            content: Text('${l10n.translate('failedToBookAppointment')}: ${userFriendlyError(l10n, e, logContext: 'Booking flow')}'),
            backgroundColor: Colors.red,
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
