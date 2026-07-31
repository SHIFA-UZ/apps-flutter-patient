/// Sanitizes auth errors from backend before surfacing to users.
/// Maps status codes to safe messages; filters out technical content (SQL, stack traces, etc.).
class AuthErrorSanitizer {
  AuthErrorSanitizer._();

  /// Known safe backend messages (exact match). If backend sends these, we pass through.
  static const _knownSafeMessages = {
    'No account found',
    'No doctor account found',
    'No patient account found',
    'No admin account found',
    'Doctor profile not found',
    'Email OTP required when email is provided',
    'Email verification code required',
    'Invalid or expired email verification code',
    'Invalid or expired verification code',
    'Invalid phone number',
    'Account is disabled',
    'Account is disabled.',
    'Not a doctor account',
    'Missing Bearer token',
    'Firebase verification not configured',
    'Invalid or expired token',
    'Access restricted to doctors.',
    'Your account has been blocked.',
    'Invalid key',
    'Key already used',
    'Username is required',
    'Password is required',
    'Invalid credentials',
    'Account is locked',
    'Invalid credentials - password mismatch',
    'You need to create a patient account first. Use Create Account in the patient app and link your doctor account.',
    'Phone verification not configured',
    'Invalid or expired phone verification',
    'Phone number not found in verification',
    'Invalid phone in verification',
    'Phone number does not match verification',
    'Email already registered',
    'Phone already registered',
    'Patient with this phone number already exists.',
    'Phone number not found',
    'User not found',
    'Too many requests. Please try again later.',
    'Too many verification requests. Please try again later.',
    'SMS could not be sent. Please use email verification or contact support.',
  };

  /// Patterns that indicate technical/internal content - do not show to users.
  static final _technicalPatterns = [
    RegExp(r'\b(SELECT|INSERT|UPDATE|DELETE|CREATE|DROP|ALTER)\b', caseSensitive: false),
    RegExp(r'\b(ORA-\d+|SQLSTATE|syntax error)\b', caseSensitive: false),
    RegExp(r'\bat\s+\w+\.\w+\s*\([^)]*\)', caseSensitive: false), // stack trace: at Class.method(File.kt:123)
    RegExp(r'\bCaused by:\s', caseSensitive: false),
    RegExp(r'\bException\s*:', caseSensitive: false),
    RegExp(r'\bNullPointerException|IllegalArgumentException|BadCredentialsException\b', caseSensitive: false),
    RegExp(r'^\s*\{.*\}\s*$'), // raw JSON object
    RegExp(r'java\.|kotlin\.|org\.spring|org\.hibernate', caseSensitive: false),
  ];

  static bool _isTechnical(String? msg) {
    if (msg == null || msg.isEmpty) return false;
    final s = msg.trim();
    if (s.length > 500) return true; // long messages often contain stack traces
    return _technicalPatterns.any((p) => p.hasMatch(s));
  }

  /// Returns a user-safe message.
  ///
  /// Strategy: prefer the backend's message when it is in the known-safe list
  /// (this preserves specific cases like "Invalid credentials - password mismatch"
  /// vs plain "Invalid credentials", or the "create patient account first" message
  /// returned on 403 in the patient app). Fall back to a status-code default only
  /// when the backend message is missing, technical, or unknown.
  static String sanitize({
    required int? statusCode,
    required dynamic data,
    required String defaultMessage,
  }) {
    final String? raw = _extractRawMessage(data);
    final String? trimmed = raw?.trim().isNotEmpty == true ? raw!.trim() : null;

    // 1. Exact known-safe backend message wins (covers both 401/403 nuances).
    if (trimmed != null && _knownSafeMessages.contains(trimmed)) {
      return trimmed;
    }

    // 2. Handle "Login error: <inner>" wrapper if inner is known-safe.
    const loginPrefix = 'Login error: ';
    if (trimmed != null && trimmed.startsWith(loginPrefix)) {
      final inner = trimmed.substring(loginPrefix.length).trim();
      if (_knownSafeMessages.contains(inner)) return inner;
      // Unknown/technical inner → fall through to status-code default.
    }

    // 3. Status-code defaults (used when backend message is missing, technical,
    //    or doesn't match any known-safe variant).
    if (statusCode != null) {
      switch (statusCode) {
        case 401:
          // Generic 401: unknown cause. Prefer a neutral message the UI can localize.
          return 'Invalid credentials';
        case 403:
          // Generic 403: forbidden. Do NOT assume "doctors only" — in the patient
          // app a 403 typically means the user has no patient role. Fall through
          // to defaultMessage so the caller's context wins.
          return defaultMessage;
        case 404:
          return 'No account found';
        case 429:
          return 'Too many requests. Please try again later.';
        case 500:
        case 502:
        case 503:
          return defaultMessage;
      }
    }

    if (trimmed == null) return defaultMessage;
    if (_isTechnical(trimmed)) return defaultMessage;

    // Unknown non-technical message: be conservative and show the default.
    return defaultMessage;
  }

  static String? _extractRawMessage(dynamic data) {
    if (data is Map<String, dynamic>) {
      return (data['message'] ?? data['error'])?.toString();
    }
    if (data is Map) {
      return (data['message'] ?? data['error'])?.toString();
    }
    if (data is String) return data;
    return null;
  }
}
