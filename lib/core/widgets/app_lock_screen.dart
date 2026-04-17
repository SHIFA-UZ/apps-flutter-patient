import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:shifa_patient_app_v1/core/constants/assets.dart';
import 'package:shifa_patient_app_v1/core/localization/app_localizations.dart';
import 'package:shifa_patient_app_v1/core/services/app_lock_provider.dart';
import 'package:shifa_patient_app_v1/core/services/app_lock_service.dart';

class AppLockScreen extends ConsumerStatefulWidget {
  final VoidCallback onUnlock;
  final VoidCallback? onForceLogin;

  const AppLockScreen({super.key, required this.onUnlock, this.onForceLogin});

  @override
  ConsumerState<AppLockScreen> createState() => _AppLockScreenState();
}

class _AppLockScreenState extends ConsumerState<AppLockScreen> {
  final TextEditingController _pinController = TextEditingController();
  String _enteredPin = '';
  String? _errorMessage;
  int _failedPinAttempts = 0;
  DateTime? _cooldownUntil;

  Future<void> _verifyPin() async {
    if (_enteredPin.length < AppLockService.minPinLength) return;
    if (!AppLockService.isValidPin(_enteredPin)) return;

    final service = ref.read(appLockServiceProvider);
    final isValid = await service.verifyPin(_enteredPin);

    if (isValid && mounted) {
      widget.onUnlock();
    } else {
      final attempts = _failedPinAttempts + 1;
      final cooldownSecs = AppLockService.cooldownSecondsAfterFailedAttempt(attempts);
      if (mounted) {
        final l10n = AppLocalizations.of(context)!;
        setState(() {
          _failedPinAttempts = attempts;
          _errorMessage = l10n.incorrectPin ?? 'Incorrect PIN';
          _enteredPin = '';
          _pinController.clear();
          _cooldownUntil = DateTime.now().add(Duration(seconds: cooldownSecs));
        });
        _scheduleCooldownTick();
      }
      HapticFeedback.vibrate();
      if (attempts >= AppLockService.maxPinAttempts && widget.onForceLogin != null) {
        widget.onForceLogin!();
      }
    }
  }

  void _scheduleCooldownTick() {
    Future.delayed(const Duration(seconds: 1), () {
      if (!mounted) return;
      if (_cooldownUntil == null) return;
      if (DateTime.now().isBefore(_cooldownUntil!)) {
        setState(() {});
        _scheduleCooldownTick();
      } else {
        setState(() {
          _cooldownUntil = null;
          _errorMessage = null;
        });
      }
    });
  }

  void _onPinDigitPressed(String digit) {
    if (_cooldownUntil != null) return;
    if (_enteredPin.length >= AppLockService.maxPinLength) return;

    setState(() {
      _enteredPin += digit;
      _errorMessage = null;
    });
    _pinController.text = _enteredPin;
    HapticFeedback.selectionClick();

    if (_enteredPin.length >= AppLockService.minPinLength && AppLockService.isValidPin(_enteredPin)) {
      _verifyPin();
    }
  }

  void _onBackspace() {
    if (_cooldownUntil != null) return;
    if (_enteredPin.isEmpty) return;

    setState(() {
      _enteredPin = _enteredPin.substring(0, _enteredPin.length - 1);
      _errorMessage = null;
    });
    _pinController.text = _enteredPin;
    HapticFeedback.selectionClick();
  }

  @override
  void dispose() {
    _pinController.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    final l10n = AppLocalizations.of(context)!;

    return PopScope(
      canPop: false,
      child: Scaffold(
      backgroundColor: Colors.white,
      body: SafeArea(
        child: Center(
          child: Padding(
            padding: const EdgeInsets.all(24.0),
            child: Column(
              mainAxisAlignment: MainAxisAlignment.center,
              children: [
                ClipRRect(
                  borderRadius: BorderRadius.circular(14),
                  child: Image.asset(
                    Assets.shifaLogoPng,
                    width: 64,
                    height: 64,
                    fit: BoxFit.contain,
                  ),
                ),
                const SizedBox(height: 24),
                Text(
                  l10n.unlockShifa,
                  style: const TextStyle(
                    fontSize: 24,
                    fontWeight: FontWeight.bold,
                    color: Colors.black87,
                  ),
                ),
                const SizedBox(height: 8),
                Text(
                  l10n.enterPinToUnlock,
                  style: const TextStyle(
                    fontSize: 14,
                    color: Colors.grey,
                  ),
                  textAlign: TextAlign.center,
                ),
                const SizedBox(height: 40),
                
                // PIN Display (4-6 dots)
                Container(
                  width: 200,
                  padding: const EdgeInsets.symmetric(vertical: 16),
                  child: Row(
                    mainAxisAlignment: MainAxisAlignment.center,
                    children: List.generate(AppLockService.maxPinLength, (index) {
                      return Container(
                        margin: const EdgeInsets.symmetric(horizontal: 8),
                        width: 16,
                        height: 16,
                        decoration: BoxDecoration(
                          shape: BoxShape.circle,
                          color: index < _enteredPin.length
                              ? const Color(0xFF17C3B2)
                              : Colors.grey[300],
                        ),
                      );
                    }),
                  ),
                ),
                
                if (_errorMessage != null || _cooldownUntil != null) ...[
                  const SizedBox(height: 16),
                  Text(
                    _cooldownUntil != null && DateTime.now().isBefore(_cooldownUntil!)
                        ? l10n.translate('tryAgainInSeconds').replaceAll('{{seconds}}', _cooldownUntil!.difference(DateTime.now()).inSeconds.clamp(0, 999).toString())
                        : _errorMessage ?? '',
                    style: const TextStyle(
                      color: Colors.red,
                      fontSize: 14,
                    ),
                  ),
                ],
                
                const SizedBox(height: 24),
                
                // Forgot PIN? Log out - recovery path when user forgot PIN or session is invalid
                if (widget.onForceLogin != null)
                  TextButton(
                    onPressed: () => widget.onForceLogin!(),
                    child: Text(
                      l10n.forgotPinLogOut ?? 'Forgot PIN? Log out',
                      style: const TextStyle(
                        fontSize: 14,
                        color: Color(0xFF17C3B2),
                        decoration: TextDecoration.underline,
                      ),
                    ),
                  ),
                
                const SizedBox(height: 24),
                
                // PIN Pad (disabled during cooldown)
                IgnorePointer(
                  ignoring: _cooldownUntil != null && DateTime.now().isBefore(_cooldownUntil!),
                  child: Opacity(
                    opacity: (_cooldownUntil != null && DateTime.now().isBefore(_cooldownUntil!)) ? 0.5 : 1,
                    child: _buildPinPad(),
                  ),
                ),
              ],
            ),
          ),
        ),
      ),
    ),
    );
  }

  Widget _buildPinPad() {
    return Column(
      children: [
        Row(
          mainAxisAlignment: MainAxisAlignment.center,
          children: [
            _buildPinButton('1'),
            const SizedBox(width: 16),
            _buildPinButton('2'),
            const SizedBox(width: 16),
            _buildPinButton('3'),
          ],
        ),
        const SizedBox(height: 16),
        Row(
          mainAxisAlignment: MainAxisAlignment.center,
          children: [
            _buildPinButton('4'),
            const SizedBox(width: 16),
            _buildPinButton('5'),
            const SizedBox(width: 16),
            _buildPinButton('6'),
          ],
        ),
        const SizedBox(height: 16),
        Row(
          mainAxisAlignment: MainAxisAlignment.center,
          children: [
            _buildPinButton('7'),
            const SizedBox(width: 16),
            _buildPinButton('8'),
            const SizedBox(width: 16),
            _buildPinButton('9'),
          ],
        ),
        const SizedBox(height: 16),
        Row(
          mainAxisAlignment: MainAxisAlignment.center,
          children: [
            const SizedBox(width: 80),
            _buildPinButton('0'),
            const SizedBox(width: 16),
            _buildBackspaceButton(),
          ],
        ),
      ],
    );
  }

  Widget _buildPinButton(String digit) {
    return Material(
      color: Colors.transparent,
      child: InkWell(
        onTap: () => _onPinDigitPressed(digit),
        borderRadius: BorderRadius.circular(40),
        child: Container(
          width: 80,
          height: 80,
          decoration: BoxDecoration(
            shape: BoxShape.circle,
            border: Border.all(color: Colors.grey[300]!, width: 1),
          ),
          child: Center(
            child: Text(
              digit,
              style: const TextStyle(
                fontSize: 24,
                fontWeight: FontWeight.w500,
                color: Colors.black87,
              ),
            ),
          ),
        ),
      ),
    );
  }

  Widget _buildBackspaceButton() {
    return Material(
      color: Colors.transparent,
      child: InkWell(
        onTap: _onBackspace,
        borderRadius: BorderRadius.circular(40),
        child: Container(
          width: 80,
          height: 80,
          decoration: BoxDecoration(
            shape: BoxShape.circle,
            border: Border.all(color: Colors.grey[300]!, width: 1),
          ),
          child: const Center(
            child: Icon(Icons.backspace, size: 24, color: Colors.black87),
          ),
        ),
      ),
    );
  }
}
