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

  @override
  void initState() {
    super.initState();
    _loadDoctor();
    // Find next available date with slots and load them (saves patient from searching)
    WidgetsBinding.instance.addPostFrameCallback((_) {
      if (mounted) {
        _findAndLoadNextAvailableDate();
      }
    });
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
                  // Date selector
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
                  const SizedBox(height: 16),
                  // Time slots
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
                  const SizedBox(height: 16),
                  // Video consultation toggle
                  Card(
                    child: SwitchListTile(
                      title: Text(AppLocalizations.of(context)!.translate('videoConsultation')),
                      subtitle: Text(AppLocalizations.of(context)!.translate('haveYourAppointment')),
                      value: _isVideoConsultation,
                      onChanged: (value) {
                        setState(() {
                          _isVideoConsultation = value;
                        });
                      },
                    ),
                  ),
                  const SizedBox(height: 16),
                  // Reason for visit
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
                  const SizedBox(height: 24),
                  // Confirm button
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

      AppLogger.debug('Booking params - startAt: $startAtUtc, slotMinutes: ${selectedSlot.slotMinutes}, doctorId: ${widget.doctorId}');
      await ref.read(bookingsProvider.notifier).bookAppointment(
        doctorId: widget.doctorId,
        startAt: startAtUtc,
        slotMinutes: selectedSlot.slotMinutes,
        reason: _reasonController.text.isEmpty ? null : _reasonController.text,
        isVideo: _isVideoConsultation,
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
