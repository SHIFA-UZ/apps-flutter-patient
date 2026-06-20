/// Thrown when the SMS provider cannot deliver an OTP (e.g. DevSMS out of credit).
/// The user should be prompted to continue with email verification instead.
class SmsOtpUnavailableException implements Exception {
  final String message;

  SmsOtpUnavailableException([this.message = kSmsOtpUnavailableMessage]);

  @override
  String toString() => message;
}

const kSmsOtpUnavailableMessage =
    'SMS could not be sent. Please use email verification or contact support.';

const kOtpRateLimitMessage =
    'Too many verification requests. Please try again later.';

/// Thrown when OTP send hits the hourly rate limit (distinct from SMS provider failure).
class OtpRateLimitException implements Exception {
  final String message;

  OtpRateLimitException([this.message = kOtpRateLimitMessage]);

  @override
  String toString() => message;
}
