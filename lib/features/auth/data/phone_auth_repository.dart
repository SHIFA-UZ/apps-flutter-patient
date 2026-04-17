import 'package:firebase_auth/firebase_auth.dart';
import 'package:firebase_auth_platform_interface/firebase_auth_platform_interface.dart';
import 'package:flutter/foundation.dart' show kIsWeb;

/// Handles Firebase Phone Auth. On web uses signInWithPhoneNumber with invisible reCAPTCHA; on mobile: verifyPhoneNumber.
class PhoneAuthRepository {
  final FirebaseAuth _auth = FirebaseAuth.instance;

  /// Request OTP for [fullPhoneNumber]. On web: invisible reCAPTCHA + signInWithPhoneNumber; on mobile: verifyPhoneNumber.
  /// Pass [forceResendingToken] from a previous codeSent callback to force sending a new SMS (resend).
  Future<void> verifyPhoneNumber({
    required String fullPhoneNumber,
    int? forceResendingToken,
    required void Function(String verificationId, int? resendToken) onCodeSent,
    void Function(ConfirmationResult webConfirmationResult)? onWebCodeSent,
    void Function(PhoneAuthCredential credential)? onVerificationCompleted,
    void Function(FirebaseAuthException e)? onVerificationFailed,
    void Function(String verificationId)? onCodeAutoRetrievalTimeout,
  }) async {
    if (kIsWeb) {
      try {
        final platformAuth = (_auth as dynamic)._delegate as FirebaseAuthPlatform; // ignore: avoid_dynamic_calls
        final verifier = RecaptchaVerifier(
          auth: platformAuth,
          onError: (e) => onVerificationFailed?.call(e),
        );
        final result = await _auth.signInWithPhoneNumber(fullPhoneNumber, verifier);
        onWebCodeSent?.call(result);
        onCodeSent('', null);
      } catch (e) {
        onVerificationFailed?.call(
          e is FirebaseAuthException ? e : FirebaseAuthException(code: 'unknown', message: e.toString()),
        );
      }
      return;
    }
    await _auth.verifyPhoneNumber(
      phoneNumber: fullPhoneNumber,
      forceResendingToken: forceResendingToken,
      verificationCompleted: (credential) => onVerificationCompleted?.call(credential),
      verificationFailed: (e) => onVerificationFailed?.call(e),
      codeSent: (verificationId, resendToken) => onCodeSent(verificationId, resendToken),
      codeAutoRetrievalTimeout: (verificationId) => onCodeAutoRetrievalTimeout?.call(verificationId),
      timeout: const Duration(seconds: 120),
    );
  }

  /// Sign in with an existing phone auth credential (e.g. from verificationCompleted).
  Future<UserCredential> signInWithCredential(PhoneAuthCredential credential) async {
    return _auth.signInWithCredential(credential);
  }

  /// Verify SMS code. On web pass [webConfirmationResult]; on mobile use [verificationId].
  Future<UserCredential> signInWithPhoneCredential({
    required String verificationId,
    required String smsCode,
    ConfirmationResult? webConfirmationResult,
  }) async {
    if (kIsWeb && webConfirmationResult != null) {
      return webConfirmationResult.confirm(smsCode);
    }
    final credential = PhoneAuthProvider.credential(
      verificationId: verificationId,
      smsCode: smsCode,
    );
    return _auth.signInWithCredential(credential);
  }

  /// Get the current Firebase ID token. Call after sign-in. Throws if not signed in.
  Future<String> getIdToken(bool forceRefresh) async {
    final user = _auth.currentUser;
    if (user == null) throw Exception('Not signed in with Firebase');
    final token = await user.getIdToken(forceRefresh);
    if (token == null) throw Exception('Failed to get ID token');
    return token;
  }

  Future<void> signOut() async {
    await _auth.signOut();
  }
}
