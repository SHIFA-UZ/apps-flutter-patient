import 'package:flutter/material.dart';
import 'package:go_router/go_router.dart';
import 'package:shifa_patient_app_v1/app/router.dart';
import 'package:shifa_patient_app_v1/core/localization/app_localizations.dart';
import 'package:shifa_patient_app_v1/core/widgets/shifa_button.dart';

class ConfirmBookingScreen extends StatelessWidget {
  final String doctorId;
  final String date;
  final String time;
  final String? reason;
  final bool isVideo;

  const ConfirmBookingScreen({
    super.key,
    required this.doctorId,
    required this.date,
    required this.time,
    this.reason,
    required this.isVideo,
  });

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        leading: IconButton(
          icon: const Icon(Icons.arrow_back),
          onPressed: () => context.pop(),
        ),
        title: Text(AppLocalizations.of(context)!.translate('confirm')),
      ),
      body: SingleChildScrollView(
        padding: const EdgeInsets.all(16.0),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.stretch,
          children: [
            Card(
              child: Padding(
                padding: const EdgeInsets.all(16.0),
                child: Row(
                  children: [
                    const CircleAvatar(
                      radius: 30,
                      backgroundImage: NetworkImage('https://via.placeholder.com/60'),
                    ),
                    const SizedBox(width: 16),
                    Expanded(
                      child: Column(
                        crossAxisAlignment: CrossAxisAlignment.start,
                        children: [
                          Text(AppLocalizations.of(context)!.doctor, style: const TextStyle(fontWeight: FontWeight.bold)),
                          Row(
                            children: [
                              const Icon(Icons.videocam, size: 16, color: Colors.grey),
                              const SizedBox(width: 4),
                              Text(AppLocalizations.of(context)!.translate('videoConsultation'), style: const TextStyle(color: Colors.grey, fontSize: 12)),
                            ],
                          ),
                        ],
                      ),
                    ),
                    Column(
                      crossAxisAlignment: CrossAxisAlignment.end,
                      children: [
                        Text('$time, $date', style: const TextStyle(fontSize: 12)),
                        const SizedBox(height: 4),
                        Text(AppLocalizations.of(context)!.checkUp!, style: const TextStyle(fontSize: 12, color: Colors.grey)),
                      ],
                    ),
                  ],
                ),
              ),
            ),
            const SizedBox(height: 16),
            Card(
              child: Padding(
                padding: const EdgeInsets.all(16.0),
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Text(AppLocalizations.of(context)!.reasonForVisit, style: const TextStyle(fontWeight: FontWeight.bold)),
                    const SizedBox(height: 8),
                    Text(reason ?? AppLocalizations.of(context)!.checkUp!),
                  ],
                ),
              ),
            ),
            const SizedBox(height: 32),
            ShifaPrimaryButton(
              label: AppLocalizations.of(context)!.translate('confirm'),
              onPressed: () {
                // TODO: Confirm booking
                context.go(AppRoutes.bookings);
              },
            ),
          ],
        ),
      ),
    );
  }
}
