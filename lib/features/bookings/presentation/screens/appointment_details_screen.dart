import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';
import 'package:intl/intl.dart';
import 'package:url_launcher/url_launcher.dart';
import 'package:shifa_patient_app_v1/app/router.dart';
import 'package:shifa_patient_app_v1/core/localization/app_localizations.dart';
import 'package:shifa_patient_app_v1/core/models/appointment_model.dart';
import 'package:shifa_patient_app_v1/core/services/local_notification_service.dart';
import 'package:shifa_patient_app_v1/core/utils/date_utils.dart' as app_date_utils;
import 'package:shifa_patient_app_v1/core/utils/image_utils.dart';
import 'package:shifa_patient_app_v1/features/bookings/data/bookings_repository.dart';
import 'package:shifa_patient_app_v1/core/widgets/shifa_button.dart';
import 'package:shifa_patient_app_v1/features/bookings/providers/bookings_provider.dart';

class AppointmentDetailsScreen extends ConsumerStatefulWidget {
  final String appointmentId;

  const AppointmentDetailsScreen({super.key, required this.appointmentId});

  @override
  ConsumerState<AppointmentDetailsScreen> createState() => _AppointmentDetailsScreenState();
}

class _AppointmentDetailsScreenState extends ConsumerState<AppointmentDetailsScreen> {
  bool _openingPayment = false;

  bool _needsPayment(dynamic appointment) {
    if (appointment.status == AppointmentStatus.cancelled ||
        appointment.status == AppointmentStatus.completed) {
      return false;
    }
    return appointment.paymentStatus.toString().toUpperCase() == 'PENDING';
  }

  Future<void> _openPaymentCheckout(String appointmentId) async {
    if (_openingPayment) return;
    setState(() => _openingPayment = true);
    try {
      await context.push<bool>('${AppRoutes.bookings}/$appointmentId/pay');
      if (mounted) {
        await ref.read(bookingsProvider.notifier).getAppointmentById(widget.appointmentId);
        setState(() {});
      }
    } finally {
      if (mounted) setState(() => _openingPayment = false);
    }
  }

  @override
  void initState() {
    super.initState();
    // Load appointment details
    WidgetsBinding.instance.addPostFrameCallback((_) {
      ref.read(bookingsProvider.notifier).getAppointmentById(widget.appointmentId);
    });
  }

  @override
  Widget build(BuildContext context) {
    final l10n = AppLocalizations.of(context)!;
    return Scaffold(
      appBar: AppBar(
        leading: IconButton(
          icon: const Icon(Icons.arrow_back),
          onPressed: () => context.pop(),
        ),
        title: Text(l10n.appointmentDetails),
      ),
      body: FutureBuilder(
        future: ref.read(bookingsProvider.notifier).getAppointmentById(widget.appointmentId),
        builder: (context, snapshot) {
          if (snapshot.connectionState == ConnectionState.waiting) {
            return const Center(child: CircularProgressIndicator());
          }

          if (snapshot.hasError) {
            return Center(
              child: Column(
                mainAxisAlignment: MainAxisAlignment.center,
                children: [
                  const Icon(Icons.error_outline, size: 48, color: Colors.red),
                  const SizedBox(height: 16),
                  Text('${l10n.error}: ${snapshot.error}'),
                  const SizedBox(height: 16),
                  ShifaPrimaryButton(
                    label: l10n.translate('goBack'),
                    onPressed: () => context.pop(),
                    width: ButtonWidth.hug,
                  ),
                ],
              ),
            );
          }

          final appointment = snapshot.data;
          if (appointment == null) {
            return Center(child: Text(l10n.translate('appointmentNotFound')));
          }

          return RefreshIndicator(
            onRefresh: () async {
              await ref.read(bookingsProvider.notifier).getAppointmentById(widget.appointmentId);
            },
            child: _buildContent(context, appointment),
          );
        },
      ),
    );
  }

  String _localizedReason(String reason, AppLocalizations l10n) {
    final r = reason.trim().toLowerCase();
    if (r == 'check up' || r == 'checkup') return l10n.checkUp ?? reason;
    return reason;
  }

  Widget _buildReviewSection(BuildContext context, dynamic appointment, AppLocalizations l10n) {
    final appointmentId = appointment.id as String;
    final reviewAsync = ref.watch(appointmentReviewProvider(appointmentId));
    return reviewAsync.when(
      loading: () => const Center(
        child: Padding(
          padding: EdgeInsets.symmetric(vertical: 16),
          child: SizedBox(
            width: 24,
            height: 24,
            child: CircularProgressIndicator(strokeWidth: 2),
          ),
        ),
      ),
      error: (_, __) => _leaveReviewButton(context, appointment, l10n),
      data: (AppointmentReview? review) {
        if (review != null) {
          return Card(
            color: Colors.green.shade50,
            child: Padding(
              padding: const EdgeInsets.all(16),
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Row(
                    children: [
                      Icon(Icons.star, color: Colors.amber.shade700, size: 20),
                      const SizedBox(width: 8),
                      Text(
                        '${l10n.yourRating ?? 'Your rating'}: ${review.rating} ${l10n.translate('stars')}',
                        style: TextStyle(
                          fontWeight: FontWeight.bold,
                          color: Colors.green.shade900,
                          fontSize: 16,
                        ),
                      ),
                    ],
                  ),
                  if (review.comment != null && review.comment!.isNotEmpty) ...[
                    const SizedBox(height: 8),
                    Text(
                      review.comment!,
                      style: TextStyle(color: Colors.green.shade800),
                    ),
                  ],
                  const SizedBox(height: 8),
                  Text(
                    l10n.thankYouForYourRating ?? 'Thank you for your rating.',
                    style: TextStyle(
                      fontWeight: FontWeight.w600,
                      color: Colors.green.shade800,
                    ),
                  ),
                ],
              ),
            ),
          );
        }
        return _leaveReviewButton(context, appointment, l10n);
      },
    );
  }

  Widget _leaveReviewButton(BuildContext context, dynamic appointment, AppLocalizations l10n) {
    return ShifaPrimaryButton(
      label: l10n.leaveReview,
      icon: Icons.rate_review,
      onPressed: () {
        context.push(
          '${AppRoutes.bookings}/${appointment.id}/review',
          extra: {
            'doctorId': appointment.doctorId,
            'doctorName': appointment.doctor?.fullName ?? '',
            'appointmentId': appointment.id,
          },
        );
      },
    );
  }

  Widget _buildContent(BuildContext context, appointment) {
    final l10n = AppLocalizations.of(context)!;
    try {
      final startDate = app_date_utils.parseAppointmentDateTime(appointment.startAt);
      final endDate = app_date_utils.parseAppointmentDateTime(appointment.endAt);
      final locale = Localizations.localeOf(context);
      final timeFormat = DateFormat('HH:mm', locale.toString());
      final dayFormat = DateFormat('EEEE', locale.toString());
      final fullDateFormat = DateFormat('dd MMMM yyyy', locale.toString());

      final dateTimeString = '${dayFormat.format(startDate)} ${timeFormat.format(startDate)} - ${timeFormat.format(endDate)}, ${fullDateFormat.format(startDate)}';
      
      final locationText = appointment.isVideo
          ? (l10n.videoConsultation ?? 'Video Consultation')
          : (appointment.location.isNotEmpty ? appointment.location : l10n.translate('clinicAddress'));

      // Appointment timing: only show 48h banner when future and < 48h; show cancel/reschedule only when >= 48h in future
      final now = DateTime.now();
      final hoursUntilAppointment = startDate.difference(now).inHours;
      final hasAppointmentEnded = endDate.isBefore(now);
      final isPastAppointment = startDate.isBefore(now);
      final isFutureAndAtLeast48Hours = !isPastAppointment && hoursUntilAppointment >= 48;
      final isFutureAndWithin48Hours = !isPastAppointment && hoursUntilAppointment < 48;
      // Video join window: button only from 5 min before start to 15 min after end (match backend)
      final canJoinVideo = appointment.isVideo && app_date_utils.isWithinVideoJoinWindow(appointment.startAt, appointment.endAt);
      if (appointment.isVideo && !isPastAppointment && !canJoinVideo) {
        WidgetsBinding.instance.addPostFrameCallback((_) {
          LocalNotificationService().scheduleVideoJoinReminder(
            appointmentId: appointment.id,
            startAtIso8601: appointment.startAt,
            title: l10n.translate('videoCallYouCanJoinNow'),
            body: l10n.translate('videoCallYouCanJoinNow'),
          );
        });
      }

      return SingleChildScrollView(
        padding: const EdgeInsets.all(16.0),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.stretch,
          children: [
            _buildDoctorCard(context, appointment),
            const SizedBox(height: 16),
            _buildInfoCard(l10n.dateAndTime, dateTimeString),
            const SizedBox(height: 16),
            _buildInfoCard(l10n.location, locationText),
            if (appointment.reason != null && appointment.reason!.isNotEmpty) ...[
              const SizedBox(height: 16),
              _buildInfoCard(l10n.reasonForVisit, _localizedReason(appointment.reason!, l10n)),
            ],
            if (_needsPayment(appointment)) ...[
              const SizedBox(height: 16),
              Card(
                color: Colors.amber.shade50,
                child: Padding(
                  padding: const EdgeInsets.all(16.0),
                  child: Column(
                    crossAxisAlignment: CrossAxisAlignment.stretch,
                    children: [
                      Row(
                        children: [
                          Icon(Icons.payments_outlined, color: Colors.amber.shade900),
                          const SizedBox(width: 8),
                          Expanded(
                            child: Text(
                              l10n.translate('paymentPendingTitle'),
                              style: TextStyle(
                                fontWeight: FontWeight.bold,
                                color: Colors.amber.shade900,
                              ),
                            ),
                          ),
                        ],
                      ),
                      const SizedBox(height: 8),
                      Text(
                        l10n.translate('paymentPendingMessage'),
                        style: TextStyle(color: Colors.amber.shade900, fontSize: 14),
                      ),
                      const SizedBox(height: 16),
                      ShifaPrimaryButton(
                        label: l10n.translate('payNow'),
                        icon: Icons.payment,
                        isLoading: _openingPayment,
                        onPressed: _openingPayment ? null : () => _openPaymentCheckout(appointment.id),
                      ),
                    ],
                  ),
                ),
              ),
            ],
            if (hasAppointmentEnded) ...[
              const SizedBox(height: 16),
              ShifaPrimaryButton(
                label: l10n.translate('viewVisitSummary'),
                icon: Icons.auto_awesome,
                onPressed: () {
                  context.push('${AppRoutes.bookings}/${appointment.id}/visit-summary');
                },
              ),
            ],
            // Sign appointment summary (when doctor requested signature)
            if (appointment.signatureRequested && !appointment.alreadySigned) ...[
              const SizedBox(height: 16),
              ShifaPrimaryButton(
                label: l10n.signAppointmentSummary,
                icon: Icons.draw,
                onPressed: () {
                  context.push('${AppRoutes.bookings}/${appointment.id}/sign');
                },
              ),
              const SizedBox(height: 12),
            ],
            if (appointment.signatureRequested && appointment.alreadySigned) ...[
              const SizedBox(height: 16),
              Card(
                color: Colors.green.shade50,
                child: Padding(
                  padding: const EdgeInsets.all(12),
                  child: Row(
                    children: [
                      Icon(Icons.check_circle, color: Colors.green.shade700),
                      const SizedBox(width: 8),
                      Text(
                        l10n.signatureSubmittedSuccess,
                        style: TextStyle(color: Colors.green.shade800),
                      ),
                    ],
                  ),
                ),
              ),
              const SizedBox(height: 12),
            ],
            const SizedBox(height: 32),
            // Rating section: only for past, non-cancelled appointments
            if (isPastAppointment &&
                appointment.status != AppointmentStatus.cancelled &&
                appointment.doctor != null) ...[
              _buildReviewSection(context, appointment, l10n),
              const SizedBox(height: 12),
            ],
            // Show "Join Video Call" button when within join window (5 min before start to 15 min after end; backend allows join until 15 min after end)
            if (appointment.isVideo && canJoinVideo) ...[
              ShifaPrimaryButton(
                label: l10n.joinVideoCall,
                icon: Icons.videocam,
                onPressed: () {
                  context.push('${AppRoutes.bookings}/${appointment.id}/video');
                },
              ),
              const SizedBox(height: 12),
            ],
            // Hint when video call is upcoming but join window not yet open (before start-5min)
            if (appointment.isVideo && !canJoinVideo && !isPastAppointment) ...[
              Padding(
                padding: const EdgeInsets.only(bottom: 12),
                child: Text(
                  l10n.translate('errorVideoCallNotYetAvailable'),
                  style: TextStyle(color: Colors.grey.shade700, fontSize: 13),
                ),
              ),
            ],
            // Reschedule: only when appointment is at least 48 hours in the future
            if (isFutureAndAtLeast48Hours) ...[
              ShifaPrimaryButton(
                label: l10n.changeBooking,
                icon: Icons.edit_calendar,
                onPressed: () {
                  context.push('${AppRoutes.bookingFlow}/${appointment.doctorId}?rescheduleId=${appointment.id}');
                },
              ),
              const SizedBox(height: 12),
            ],
            // 48h notice + contact doctor: only when appointment is in the future and less than 48 hours away
            if (isFutureAndWithin48Hours) ...[
              Card(
                color: Colors.orange.shade50,
                child: Padding(
                  padding: const EdgeInsets.all(16.0),
                  child: Column(
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: [
                      Row(
                        children: [
                          Icon(Icons.info_outline, color: Colors.orange.shade700),
                          const SizedBox(width: 8),
                          Text(
                            l10n.appointmentLessThan48Hours,
                            style: TextStyle(
                              fontWeight: FontWeight.bold,
                              color: Colors.orange.shade700,
                            ),
                          ),
                        ],
                      ),
                      const SizedBox(height: 8),
                      Text(
                        l10n.contactDoctorDirectly,
                        style: TextStyle(color: Colors.orange.shade900),
                      ),
                      if (appointment.doctor?.phone != null || appointment.doctor?.email != null) ...[
                        const SizedBox(height: 12),
                        if (appointment.doctor?.phone != null)
                          _buildContactOption(
                            icon: Icons.phone,
                            label: l10n.callDoctor,
                            value: appointment.doctor!.phone!,
                            onTap: () async {
                              final phone = appointment.doctor!.phone!;
                              final uri = Uri.parse('tel:$phone');
                              if (await canLaunchUrl(uri)) {
                                await launchUrl(uri);
                              } else {
                                if (mounted) {
                                  ScaffoldMessenger.of(context).showSnackBar(
                                    SnackBar(content: Text('${l10n.translate('cannotMakePhoneCall')} $phone')),
                                  );
                                }
                              }
                            },
                          ),
                        if (appointment.doctor?.email != null)
                          _buildContactOption(
                            icon: Icons.email,
                            label: l10n.emailDoctor,
                            value: appointment.doctor!.email!,
                            onTap: () async {
                              final email = appointment.doctor!.email!;
                              final uri = Uri.parse('mailto:$email');
                              if (await canLaunchUrl(uri)) {
                                await launchUrl(uri);
                              } else {
                                if (mounted) {
                                  ScaffoldMessenger.of(context).showSnackBar(
                                    SnackBar(content: Text('${l10n.translate('cannotSendEmail')} $email')),
                                  );
                                }
                              }
                            },
                          ),
                      ],
                    ],
                  ),
                ),
              ),
              const SizedBox(height: 12),
            ],
            // Cancel: only when appointment is at least 48 hours in the future (not when within 48h)
            if (isFutureAndAtLeast48Hours)
              ShifaSecondaryButton(
                label: l10n.cancelBooking,
                destructive: true,
                onPressed: () async {
                  final confirmed = await showDialog<bool>(
                    context: context,
                    builder: (dialogContext) {
                      final dialogL10n = AppLocalizations.of(dialogContext)!;
                      return AlertDialog(
                        title: Text(dialogL10n.cancelAppointment),
                        content: Text(dialogL10n.areYouSureCancel),
                        actions: [
                          TextButton(
                            onPressed: () => Navigator.of(dialogContext).pop(false),
                            child: Text(dialogL10n.no),
                          ),
                          TextButton(
                            onPressed: () => Navigator.of(dialogContext).pop(true),
                            child: Text(dialogL10n.yes, style: const TextStyle(color: Colors.red)),
                          ),
                        ],
                      );
                    },
                  );

                  if (confirmed == true) {
                    try {
                      await ref.read(bookingsProvider.notifier).cancelAppointment(appointment.id);
                      if (mounted) {
                        ScaffoldMessenger.of(context).showSnackBar(
                          SnackBar(content: Text(l10n.appointmentCancelledSuccessfully)),
                        );
                        context.pop();
                      }
                    } catch (e) {
                      if (mounted) {
                        ScaffoldMessenger.of(context).showSnackBar(
                          SnackBar(content: Text('${l10n.errorCancellingAppointment}: $e')),
                        );
                      }
                    }
                  }
                },
              ),
          ],
        ),
      );
    } catch (e) {
      return Center(
        child: Column(
          mainAxisAlignment: MainAxisAlignment.center,
          children: [
            const Icon(Icons.error_outline, size: 48, color: Colors.red),
            const SizedBox(height: 16),
            Text('${l10n.translate('errorParsingAppointmentData')}: $e'),
            const SizedBox(height: 16),
            ShifaPrimaryButton(
              label: l10n.translate('goBack'),
              onPressed: () => context.pop(),
              width: ButtonWidth.hug,
            ),
          ],
        ),
      );
    }
  }

  Widget _buildDoctorCard(BuildContext context, appointment) {
    final l10n = AppLocalizations.of(context)!;
    final doctor = appointment.doctor;
    if (doctor == null) {
      return Card(
        child: ListTile(
          title: Text(l10n.translate('unknownDoctor')),
        ),
      );
    }

    return Card(
      child: ListTile(
        leading: CircleAvatar(
          radius: 30,
          backgroundImage: doctor.photoUrl != null
              ? NetworkImage(normalizePhotoUrl(doctor.photoUrl!) ?? doctor.photoUrl!)
              : null,
          child: doctor.photoUrl == null
              ? Text(
                  doctor.firstName.isNotEmpty
                      ? doctor.firstName[0].toUpperCase()
                      : 'D',
                  style: const TextStyle(fontSize: 24),
                )
              : null,
        ),
        title: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Text(
              doctor.fullName,
              style: const TextStyle(fontWeight: FontWeight.bold),
              maxLines: 1,
              overflow: TextOverflow.ellipsis,
            ),
            if (doctor.profession != null && doctor.profession!.isNotEmpty)
              Text(
                AppLocalizations.of(context)!.translateProfession(doctor.profession!),
                style: const TextStyle(
                  fontSize: 14,
                  color: Colors.grey,
                  fontWeight: FontWeight.normal,
                ),
                maxLines: 1,
                overflow: TextOverflow.ellipsis,
              ),
          ],
        ),
        subtitle: Text(doctor.clinic ?? ''),
        trailing: const Icon(Icons.chevron_right),
        onTap: () {
          context.push('${AppRoutes.doctors}/${doctor.id}');
        },
      ),
    );
  }

  Widget _buildInfoCard(String title, String content) {
    return Card(
      child: Padding(
        padding: const EdgeInsets.all(16.0),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Text(
              title,
              style: const TextStyle(fontWeight: FontWeight.bold, fontSize: 16),
            ),
            const SizedBox(height: 8),
            Text(content),
          ],
        ),
      ),
    );
  }

  Widget _buildContactOption({
    required IconData icon,
    required String label,
    required String value,
    required VoidCallback onTap,
  }) {
    return InkWell(
      onTap: onTap,
      child: Padding(
        padding: const EdgeInsets.symmetric(vertical: 8.0),
        child: Row(
          children: [
            Icon(icon, size: 20, color: const Color(0xFF17C3B2)),
            const SizedBox(width: 12),
            Expanded(
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Text(
                    label,
                    style: const TextStyle(
                      fontWeight: FontWeight.w600,
                      fontSize: 14,
                    ),
                  ),
                  Text(
                    value,
                    style: TextStyle(
                      fontSize: 12,
                      color: Colors.grey.shade700,
                    ),
                  ),
                ],
              ),
            ),
            const Icon(Icons.chevron_right, size: 20),
          ],
        ),
      ),
    );
  }
}
