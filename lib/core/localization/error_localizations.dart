import 'package:shifa_patient_app_v1/core/localization/app_localizations.dart';
import 'package:shifa_patient_app_v1/core/utils/app_logger.dart';

/// Maps known backend/API error messages (English) to localization keys.
/// Used to show errors in the user's language.
const Map<String, String> _errorMessageToKey = {
  // Auth
  'No account found': 'errorNoAccountFound',
  'No doctor account found': 'errorNoDoctorAccountFound',
  'No patient account found': 'errorNoPatientAccountFound',
  'No admin account found': 'errorNoAdminAccountFound',
  'Doctor profile not found': 'errorDoctorProfileNotFound',
  'Email OTP required when email is provided': 'errorEmailOtpRequiredWhenEmailProvided',
  'Email verification code required': 'errorEmailVerificationCodeRequired',
  'Invalid or expired email verification code': 'errorInvalidOrExpiredEmailVerificationCode',
  'Invalid phone number': 'errorInvalidPhoneNumber',
  'Account is disabled': 'errorAccountIsDisabled',
  'Account is disabled.': 'errorAccountIsDisabled',
  'Not a doctor account': 'errorNotADoctorAccount',
  'Missing Bearer token': 'errorMissingBearerToken',
  'Firebase verification not configured': 'errorFirebaseVerificationNotConfigured',
  'Invalid or expired token': 'errorInvalidOrExpiredToken',
  'Access restricted to doctors.': 'errorAccessRestrictedToDoctors',
  'Your account has been blocked.': 'errorYourAccountHasBeenBlocked',
  'Invalid key': 'errorInvalidKey',
  'Key already used': 'errorKeyAlreadyUsed',
  'Username is required': 'errorUsernameRequired',
  'Password is required': 'errorPasswordRequired',
  'Invalid credentials': 'errorInvalidCredentials',
  'Account is locked': 'errorAccountLocked',
  'Invalid credentials - password mismatch': 'errorInvalidCredentialsPasswordMismatch',
  'You need to create a patient account first. Use Create Account in the patient app and link your doctor account.': 'errorCreatePatientAccountFirst',
  'Phone verification not configured': 'errorPhoneVerificationNotConfigured',
  'Invalid or expired phone verification': 'errorInvalidOrExpiredPhoneVerification',
  'Phone number not found in verification': 'errorPhoneNumberNotFoundInVerification',
  'Invalid phone in verification': 'errorInvalidPhoneInVerification',
  'Phone number does not match verification': 'errorPhoneNumberDoesNotMatchVerification',
  'Email already registered': 'errorEmailAlreadyRegistered',
  'Phone already registered': 'errorPhoneAlreadyRegistered',
  'Patient with this phone number already exists.': 'errorPatientWithPhoneAlreadyExists',
  'Phone number not found': 'errorPhoneNumberNotFound',
  'User not found': 'errorUserNotFound',
  // Security
  'Session invalid': 'errorSessionInvalid',
  'Session expired or signed out': 'errorSessionExpiredOrSignedOut',
  'Invalid token': 'errorInvalidToken',
  'Too many requests. Please try again later.': 'errorTooManyRequests',
  // Video
  'Authentication required': 'errorAuthenticationRequired',
  'Video call is not yet available. You can join 5 minutes before the appointment start.': 'errorVideoCallNotYetAvailable',
  'Video call has ended. The join window closes 15 minutes after the appointment end.': 'errorVideoCallHasEnded',
  'Payment is required before joining this video consultation.': 'errorVideoCallPaymentRequired',
  'Camera and microphone access are required for video consultations. You can enable them in your device Settings.':
      'errorVideoCallMediaPermissionRequired',
  'Video call is not available. It may have ended, or the join window has closed (usually 15 minutes after the appointment end).': 'errorVideoCallHasEnded',
  'Appointment does not belong to this doctor': 'errorAppointmentDoesNotBelongToDoctor',
  'Appointment does not have a patient assigned. Please ensure the appointment is linked to your profile.': 'errorAppointmentDoesNotHavePatientAssigned',
  'Appointment does not belong to this patient': 'errorAppointmentDoesNotBelongToPatient',
  'Unable to determine user identity': 'errorUnableToDetermineUserIdentity',
  'Unable to determine user name': 'errorUnableToDetermineUserName',
  'User name cannot be blank': 'errorUserNameCannotBeBlank',
  'Failed to generate token': 'errorFailedToGenerateToken',
  'Daily.co API key is not configured. Please set DAILY_API_KEY environment variable in Railway.': 'errorDailyApiKeyNotConfigured',
  // App fallbacks
  'No token received from server': 'errorNoTokenReceivedFromServer',
  'Login failed': 'errorLoginFailed',
  'Failed to send email code': 'errorFailedToSendEmailCode',
  'Could not send verification email. Please try again later.': 'errorFailedToSendEmailCode',
  'Failed to send SMS code': 'errorFailedToSendSmsCode',
  'Could not send verification SMS. Please try again later.': 'errorFailedToSendSmsCode',
  'SMS verification is only available for Uzbek phone numbers. Please use email.':
      'errorSmsOnlyForUzbekPhone',
  'Could not verify account details': 'errorCouldNotVerifyAccountDetails',
  'Connection timed out. Check your internet and try again.': 'errorConnectionTimedOut',
  'Network error. Check your internet connection and try again.': 'errorNetworkConnection',
  'No token received': 'errorNoTokenReceived',
  'Failed to create patient account': 'errorFailedToCreatePatientAccount',
  'Registration failed': 'errorRegistrationFailed',
  'Failed to reset password': 'errorFailedToResetPassword',
  'Failed to change password': 'errorFailedToChangePassword',
  'Unknown error': 'errorUnknownError',
  'Something went wrong': 'errorSomethingWentWrong',
  'Session expired. Please start again.': 'errorSessionExpiredPleaseStartAgain',
  // Remote care check-in (API)
  'Boolean value required': 'checkInPleaseSelectYesOrNo',
  'Numeric value required': 'checkInValueRequired',
  'Text value required': 'checkInValueRequired',
  // Bookings (patient app video consultation)
  'serviceId is required for video consultation': 'bookingSelectServiceForVideo',
  'Please select a service for video consultation.': 'bookingSelectServiceForVideo',
};

/// Prefixes for messages that contain a dynamic part (e.g. "Appointment not found: 12345").
/// Map: prefix -> (key, placeholder name for suffix).
final List<({String prefix, String key, String placeholder})> _errorMessagePrefixToKey = [
  (prefix: 'Invalid response from server: ', key: 'errorInvalidResponseFromServer', placeholder: '{{code}}'),
  (prefix: 'Network error: ', key: 'errorNetworkError', placeholder: '{{type}}'),
  (prefix: 'Appointment not found: ', key: 'errorAppointmentNotFound', placeholder: '{{id}}'),
  (prefix: 'Failed to retrieve patient profile: ', key: 'errorFailedToRetrievePatientProfile', placeholder: '{{detail}}'),
  (prefix: 'Failed to get or create room: ', key: 'errorFailedToGetOrCreateRoom', placeholder: '{{detail}}'),
  (prefix: 'Failed to generate token: ', key: 'errorFailedToGenerateTokenDetail', placeholder: '{{detail}}'),
  (prefix: 'Failed to generate video token: ', key: 'errorFailedToGenerateVideoToken', placeholder: '{{detail}}'),
  (prefix: 'Error ', key: 'errorStatusCode', placeholder: '{{code}}'), // e.g. "Error 500" — only when suffix is digits
];

/// Patterns that indicate technical content - never show to users.
final _technicalPatterns = [
  RegExp(r'\b(SELECT|INSERT|UPDATE|DELETE|CREATE|DROP|ALTER)\b', caseSensitive: false),
  RegExp(r'\b(ORA-\d+|SQLSTATE|syntax error)\b', caseSensitive: false),
  RegExp(r'\bat\s+\w+\.\w+\s*\([^)]*\)', caseSensitive: false),
  RegExp(r'\bCaused by:\s', caseSensitive: false),
  RegExp(r'\bNullPointerException|IllegalArgumentException|BadCredentialsException\b', caseSensitive: false),
  RegExp(r'java\.|kotlin\.|org\.spring|org\.hibernate', caseSensitive: false),
];

bool _isTechnicalMessage(String msg) {
  if (msg.length > 500) return true;
  return _technicalPatterns.any((p) => p.hasMatch(msg));
}

/// Returns the localized error message for [rawMessage] (usually from backend or exception).
/// Uses [l10n] to resolve keys. Never surfaces technical content (SQL, stack traces, etc.).
String translateError(AppLocalizations l10n, String rawMessage) {
  if (rawMessage.isEmpty) return l10n.translate('errorSomethingWentWrong');

  var trimmed = rawMessage.trim();
  // Unwrap nested wrappers from repository (e.g. "Failed to book appointment: Exception: ...")
  for (var depth = 0; depth < 8; depth++) {
    var next = trimmed.startsWith('Exception:')
        ? trimmed.substring('Exception:'.length).trim()
        : trimmed;
    if (next.startsWith('Failed to book appointment:')) {
      next = next.substring('Failed to book appointment:'.length).trim();
    }
    if (next == trimmed) break;
    trimmed = next;
  }

  // Safety net: never show technical content
  if (_isTechnicalMessage(trimmed)) return l10n.translate('errorSomethingWentWrong');

  // Login error: X - if X is technical, we already returned above; if not mapped, use generic
  if (trimmed.startsWith('Login error: ')) {
    final inner = trimmed.substring('Login error: '.length).trim();
    if (_isTechnicalMessage(inner)) return l10n.translate('errorLoginFailed');
    // Try to translate inner; if no match, fall through to return generic at end
  }

  // Exact match
  final exactKey = _errorMessageToKey[trimmed];
  if (exactKey != null) {
    final translated = l10n.translate(exactKey);
    if (translated != exactKey) return translated;
  }

  // Video token / join-window errors (API may vary slightly)
  final lower = trimmed.toLowerCase();
  if (lower.contains('timeoutexception') &&
      (lower.contains('join failed') || lower.contains('future not completed'))) {
    final t = l10n.translate('videoCallConnectionTimeout');
    if (t != 'videoCallConnectionTimeout') return t;
  }
  if (lower.contains('payment is required') &&
      (lower.contains('video') || lower.contains('consultation'))) {
    final t = l10n.translate('errorVideoCallPaymentRequired');
    if (t != 'errorVideoCallPaymentRequired') return t;
  }
  if (lower.contains('not yet available') && lower.contains('5 minutes before')) {
    final t = l10n.translate('errorVideoCallNotYetAvailable');
    if (t != 'errorVideoCallNotYetAvailable') return t;
  }
  if (lower.contains('video call has ended') ||
      (lower.contains('join window') && lower.contains('15'))) {
    final t = l10n.translate('errorVideoCallHasEnded');
    if (t != 'errorVideoCallHasEnded') return t;
  }
  if (lower.contains('call error occurred')) {
    final t = l10n.translate('callErrorOccurred');
    if (t != 'callErrorOccurred') return t;
  }

  // Access denied: This app requires ROLE role
  if (trimmed.startsWith('Access denied: This app requires ') && trimmed.endsWith(' role')) {
    final role = trimmed.substring('Access denied: This app requires '.length, trimmed.length - ' role'.length);
    final t = l10n.translate('errorAccessDeniedThisAppRequiresRole');
    return t.replaceAll(r'${requiredRole.name}', role);
  }

  // Prefix match (message with dynamic part)
  for (final entry in _errorMessagePrefixToKey) {
    if (!trimmed.startsWith(entry.prefix)) continue;
    final suffix = trimmed.substring(entry.prefix.length);
    // Only use errorStatusCode when suffix looks like a status code (digits)
    if (entry.key == 'errorStatusCode' && !RegExp(r'^\d+$').hasMatch(suffix.trim())) continue;
    final translated = l10n.translate(entry.key);
    if (translated != entry.key) {
      return translated.replaceAll(entry.placeholder, suffix.trim());
    }
  }

  // Patient profile not found for user X. Please ensure...
  const patientProfilePrefix = 'Patient profile not found for user ';
  if (trimmed.startsWith(patientProfilePrefix)) {
    final translated = l10n.translate('errorPatientProfileNotFoundForUser');
    if (translated != 'errorPatientProfileNotFoundForUser') {
      final rest = trimmed.substring(patientProfilePrefix.length);
      final id = rest.split('.').first.trim();
      return translated.replaceAll('{{id}}', id).replaceAll(r'${user.id}', id);
    }
  }

  // Login error: ... / Registration error: ... (handled above for Login; inner not shown if technical)
  if (trimmed.startsWith('Login error: ')) {
    return l10n.translate('errorLoginFailed');
  }
  if (trimmed.startsWith('Registration error: ')) {
    return l10n.translate('errorRegistrationFailed');
  }

  // Unknown message - if it looks like raw output, use generic
  if (trimmed.startsWith('{') && trimmed.endsWith('}')) {
    return l10n.translate('errorSomethingWentWrong');
  }

  // Exception-like or technical - never show raw to users
  if (RegExp(r'\b\w*Exception\b|\bError\s*:', caseSensitive: false).hasMatch(trimmed) ||
      trimmed.contains(' at ') ||
      trimmed.length > 120) {
    return l10n.translate('errorSomethingWentWrong');
  }

  return trimmed;
}

/// Returns a user-friendly error message and logs the raw error in debug only.
String userFriendlyError(AppLocalizations l10n, Object error, {String? logContext}) {
  final raw = error.toString();
  AppLogger.error(logContext ?? 'Error', error);
  return translateError(l10n, raw);
}
