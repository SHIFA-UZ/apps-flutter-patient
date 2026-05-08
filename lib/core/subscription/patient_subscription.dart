/// Patient subscription tier and feature flags.
///
/// The backend exposes the patient's tier on `/patients/me/profile` as
/// `subscriptionTier` (string), with the granted feature list under `features`.
///
/// Patients may only be `PRO` or `PREMIUM`. The only difference between the
/// two on the patient side is access to Shifa AI (`PATIENT_SHIFA_AI`).
enum PatientTier {
  /// Everything except Shifa AI.
  pro,

  /// Everything, including Shifa AI.
  premium,
}

/// Patient-facing features that are gated by tier. Values must mirror the
/// names of `SubscriptionFeature` on the backend so we can match them against
/// the `features` list returned by `/patients/me/profile`.
enum PatientFeature {
  /// Shifa AI (chat, voice, doctor matching) — PREMIUM only.
  shifaAi,
}

const Map<PatientFeature, String> _featureBackendName = {
  PatientFeature.shifaAi: 'PATIENT_SHIFA_AI',
};

const Map<PatientFeature, PatientTier> _minTier = {
  PatientFeature.shifaAi: PatientTier.premium,
};

bool _tierMeets(PatientTier actual, PatientTier required) {
  // PRO < PREMIUM.
  if (required == PatientTier.pro) return true;
  return actual == PatientTier.premium;
}

/// Returns true when a patient with [tier] is allowed to use [feature].
bool tierAllows(PatientTier tier, PatientFeature feature) {
  final required = _minTier[feature] ?? PatientTier.premium;
  return _tierMeets(tier, required);
}

/// Backend enum name for [feature], e.g. `PATIENT_SHIFA_AI`.
String featureBackendName(PatientFeature feature) =>
    _featureBackendName[feature] ?? feature.name.toUpperCase();

/// Parses a backend tier string ("PRO" / "PREMIUM"). Defaults to [fallback]
/// (PREMIUM) so that a missing tier never silently downgrades the user.
PatientTier parsePatientTier(String? raw, {PatientTier fallback = PatientTier.premium}) {
  switch ((raw ?? '').trim().toUpperCase()) {
    case 'PRO':
      return PatientTier.pro;
    case 'PREMIUM':
      return PatientTier.premium;
    default:
      return fallback;
  }
}

/// Localised label for [tier] ("Pro" / "Premium").
String patientTierLabel(PatientTier tier) {
  switch (tier) {
    case PatientTier.pro:
      return 'Pro';
    case PatientTier.premium:
      return 'Premium';
  }
}
