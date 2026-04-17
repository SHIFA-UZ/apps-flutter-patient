import 'package:flutter/material.dart';
import 'package:go_router/go_router.dart';
import 'package:shifa_patient_app_v1/app/router.dart';
import 'package:shifa_patient_app_v1/core/localization/app_localizations.dart';
import 'package:shifa_patient_app_v1/core/widgets/shifa_button.dart';

class WaitingRoomScreen extends StatelessWidget {
  final String appointmentId;

  const WaitingRoomScreen({super.key, required this.appointmentId});

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      backgroundColor: Colors.white,
      appBar: AppBar(
        leading: IconButton(
          icon: const Icon(Icons.arrow_back),
          onPressed: () => context.pop(),
        ),
        title: Text(
          AppLocalizations.of(context)!.translate('waitingForDoctor'),
        ),
      ),
      body: Center(
        child: Column(
          mainAxisAlignment: MainAxisAlignment.center,
          children: [
            // Step indicators
            Row(
              mainAxisAlignment: MainAxisAlignment.center,
              children: [
                _buildStepIndicator(0, false),
                const SizedBox(width: 8),
                _buildStepIndicator(1, true),
                const SizedBox(width: 8),
                _buildStepIndicator(2, false),
              ],
            ),
            const SizedBox(height: 60),
            // Doctor avatar (no remote placeholder — tests and offline-safe)
            CircleAvatar(
              radius: 100,
              backgroundColor: Colors.grey[200],
              child: Icon(Icons.person, size: 96, color: Colors.grey[600]),
            ),
            const SizedBox(height: 40),
            const Text(
              'Waiting for doctor to join...',
              style: TextStyle(fontSize: 16, color: Colors.grey),
            ),
            const SizedBox(height: 40),
            Padding(
              padding: const EdgeInsets.symmetric(horizontal: 24.0),
              child: ShifaPrimaryButton(
                label: AppLocalizations.of(context)!.translate('join'),
                onPressed: () {
                  context.push('${AppRoutes.bookings}/$appointmentId/video');
                },
              ),
            ),
          ],
        ),
      ),
    );
  }

  Widget _buildStepIndicator(int step, bool isActive) {
    return Container(
      width: 8,
      height: 8,
      decoration: BoxDecoration(
        shape: BoxShape.circle,
        color: isActive ? const Color(0xFF17C3B2) : Colors.grey[300],
      ),
    );
  }
}
