import 'package:flutter_riverpod/flutter_riverpod.dart';

import 'package:shifa_patient_app_v1/core/subscription/patient_subscription.dart';
import 'package:shifa_patient_app_v1/features/profile/providers/profile_provider.dart';

/// The currently authenticated patient's tier. Defaults to PREMIUM if the
/// profile has not yet loaded so we don't flash "feature locked" UI on cold
/// start. The backend remains the source of truth and will reject any call
/// that the tier doesn't allow.
final patientTierProvider = Provider<PatientTier>((ref) {
  final profile = ref.watch(profileProvider).profile;
  return parsePatientTier(profile?.subscriptionTier);
});

/// Reactive flag for a single feature. Prefer this over re-deriving from
/// [patientTierProvider] in widgets so we get fine-grained rebuilds.
final patientFeatureProvider = Provider.family<bool, PatientFeature>((ref, feature) {
  final profile = ref.watch(profileProvider).profile;
  // Backend echoes the granted features explicitly. Trust that list when
  // present so admin downgrades take effect immediately on the next refresh,
  // even if our enum mapping ever drifts.
  final backendName = featureBackendName(feature);
  final granted = profile?.features ?? const <String>[];
  if (granted.isNotEmpty) {
    return granted.contains(backendName);
  }
  final tier = parsePatientTier(profile?.subscriptionTier);
  return tierAllows(tier, feature);
});
