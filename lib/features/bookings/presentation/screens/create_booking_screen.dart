import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';
import 'package:shifa_patient_app_v1/core/localization/app_localizations.dart';
import 'package:shifa_patient_app_v1/core/widgets/shifa_button.dart';
import 'package:shifa_patient_app_v1/core/models/doctor_model.dart';
import 'package:shifa_patient_app_v1/core/utils/image_utils.dart';
import 'package:shifa_patient_app_v1/features/doctors/data/doctors_search_filters.dart';
import 'package:shifa_patient_app_v1/features/doctors/providers/doctors_provider.dart';

class CreateBookingScreen extends ConsumerStatefulWidget {
  const CreateBookingScreen({super.key});

  @override
  ConsumerState<CreateBookingScreen> createState() => _CreateBookingScreenState();
}

class _CreateBookingScreenState extends ConsumerState<CreateBookingScreen> {
  final _searchController = TextEditingController();

  @override
  void initState() {
    super.initState();
    // Load doctors on init
    WidgetsBinding.instance.addPostFrameCallback((_) {
      ref.read(doctorsProvider.notifier).loadDoctors();
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
      ref.read(doctorsProvider.notifier).searchDoctors(
            filters: DoctorsSearchFilters(search: query),
          );
    }
  }

  @override
  Widget build(BuildContext context) {
    final l10n = AppLocalizations.of(context)!;
    final doctorsState = ref.watch(doctorsProvider);

    return Scaffold(
      appBar: AppBar(
        leading: IconButton(
          icon: const Icon(Icons.arrow_back),
          onPressed: () => context.pop(),
        ),
        title: Text(l10n.createBooking),
      ),
      body: Column(
        children: [
          Padding(
            padding: const EdgeInsets.all(16.0),
            child: TextField(
              controller: _searchController,
              decoration: InputDecoration(
                hintText: l10n.searchDoctors,
                prefixIcon: const Icon(Icons.search),
                border: OutlineInputBorder(
                  borderRadius: BorderRadius.circular(12),
                ),
              ),
              onChanged: _performSearch,
            ),
          ),
          Expanded(
            child: doctorsState.isLoading
                ? const Center(child: CircularProgressIndicator())
                : doctorsState.error != null
                    ? Center(
                        child: Column(
                          mainAxisAlignment: MainAxisAlignment.center,
                          children: [
                            Text(
                              '${l10n.error}: ${doctorsState.error}',
                              style: const TextStyle(color: Colors.red),
                              textAlign: TextAlign.center,
                            ),
                            const SizedBox(height: 16),
                            ShifaPrimaryButton(
                              label: l10n.retry,
                              onPressed: () => ref.read(doctorsProvider.notifier).loadDoctors(),
                              width: ButtonWidth.hug,
                            ),
                          ],
                        ),
                      )
                    : doctorsState.doctors.isEmpty
                        ? Center(
                            child: Text(l10n.noDoctorsAvailable),
                          )
                        : ListView.builder(
                            padding: const EdgeInsets.all(16.0),
                            itemCount: doctorsState.doctors.length,
                            itemBuilder: (context, index) {
                              final doctor = doctorsState.doctors[index];
                              return _buildDoctorListItem(context, doctor);
                            },
                          ),
          ),
        ],
      ),
    );
  }

  Widget _buildDoctorListItem(BuildContext context, DoctorModel doctor) {
    return Card(
      margin: const EdgeInsets.only(bottom: 12),
      child: ListTile(
        leading: CircleAvatar(
          radius: 25,
          backgroundImage: doctor.photoUrl != null
              ? NetworkImage(normalizePhotoUrl(doctor.photoUrl!) ?? doctor.photoUrl!)
              : null,
          child: doctor.photoUrl == null
              ? Text(
                  doctor.firstName.isNotEmpty
                      ? doctor.firstName[0].toUpperCase()
                      : 'D',
                  style: const TextStyle(fontSize: 20),
                )
              : null,
        ),
        title: Row(
          children: [
            Expanded(
              child: Column(
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
            ),
            if (doctor.rating != null) ...[
              const SizedBox(width: 8),
              Row(
                mainAxisSize: MainAxisSize.min,
                children: [
                  const Icon(Icons.star, color: Colors.amber, size: 16),
                  const SizedBox(width: 4),
                  Text(
                    doctor.rating!.toStringAsFixed(1),
                    style: const TextStyle(
                      fontSize: 14,
                      fontWeight: FontWeight.bold,
                      color: Colors.amber,
                    ),
                  ),
                ],
              ),
            ],
          ],
        ),
        subtitle: Text(doctor.clinic ?? ''),
        trailing: const Icon(Icons.chevron_right),
        onTap: () {
          context.push('/bookings/flow/${doctor.id}');
        },
      ),
    );
  }
}
