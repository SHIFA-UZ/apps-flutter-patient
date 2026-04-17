import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:shifa_patient_app_v1/core/localization/app_localizations.dart';
import 'package:shifa_patient_app_v1/core/services/app_lock_provider.dart';
import 'package:shifa_patient_app_v1/core/services/app_lock_service.dart';

class AppLockSettingsScreen extends ConsumerStatefulWidget {
  const AppLockSettingsScreen({super.key});

  @override
  ConsumerState<AppLockSettingsScreen> createState() => _AppLockSettingsScreenState();
}

class _AppLockSettingsScreenState extends ConsumerState<AppLockSettingsScreen> {
  bool _isLoading = false;
  bool _hasPin = false;
  int _inactivitySeconds = 30;

  @override
  void initState() {
    super.initState();
    _loadSettings();
  }

  Future<void> _loadSettings() async {
    setState(() {
      _isLoading = true;
    });

    final service = ref.read(appLockServiceProvider);
    final pinExists = await service.hasPin();
    final inactivitySecs = await service.getInactivitySeconds();

    setState(() {
      _hasPin = pinExists;
      _inactivitySeconds = inactivitySecs;
      _isLoading = false;
    });
  }

  Future<void> _toggleAppLock(bool enabled) async {
    final lockNotifier = ref.read(appLockStateProvider.notifier);
    await lockNotifier.setLockEnabled(enabled);

    if (enabled && !_hasPin) {
      // If enabling but no PIN exists, prompt to set PIN
      if (mounted) {
        final shouldSetPin = await showDialog<bool>(
          context: context,
          builder: (context) => AlertDialog(
            title: Text(l10n.setUpPin ?? 'Set Up PIN'),
            content: Text(l10n.setUpPinRequired ?? 'You need to set up a PIN code to enable app lock.'),
            actions: [
              TextButton(
                onPressed: () => Navigator.pop(context, false),
                child: Text(l10n.cancel),
              ),
              TextButton(
                onPressed: () => Navigator.pop(context, true),
                child: Text(l10n.setUp ?? 'Set Up'),
              ),
            ],
          ),
        );

        if (shouldSetPin == true) {
          await _setUpPin();
        } else {
          // Disable lock if user cancels PIN setup
          await lockNotifier.setLockEnabled(false);
        }
      }
    }

    if (mounted) {
      await _loadSettings();
    }
  }

  Future<void> _setUpPin() async {
    final pin = await _showPinSetupDialog(isSetup: true);
    if (pin != null && AppLockService.isValidPin(pin)) {
      final confirmPin = await _showPinSetupDialog(
        isSetup: true,
        title: l10n.confirmPin ?? 'Confirm PIN',
        hint: l10n.reEnterPin ?? 'Re-enter your PIN',
      );

      if (confirmPin == pin) {
        final service = ref.read(appLockServiceProvider);
        await service.setPin(pin);
        
        if (mounted) {
          ScaffoldMessenger.of(context).showSnackBar(
            SnackBar(content: Text(l10n.pinSetSuccessfully ?? 'PIN set successfully')),
          );
          await _loadSettings();
        }
      } else {
        if (mounted) {
          ScaffoldMessenger.of(context).showSnackBar(
            SnackBar(content: Text(l10n.pinsDoNotMatch ?? 'PINs do not match')),
          );
        }
      }
    } else if (pin != null && pin.isNotEmpty) {
      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
          SnackBar(content: Text(l10n.translate('pinLengthRequirement'))),
        );
      }
    }
  }

  Future<void> _changePin() async {
    // First verify current PIN
    final currentPin = await _showPinSetupDialog(
      isSetup: false,
      title: l10n.enterCurrentPin ?? 'Enter Current PIN',
      hint: l10n.enterCurrentPin ?? 'Enter your current PIN',
    );

    if (currentPin == null) return;

    final service = ref.read(appLockServiceProvider);
    final isValid = await service.verifyPin(currentPin);

    if (!isValid) {
      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
          SnackBar(content: Text(l10n.incorrectPin ?? 'Incorrect PIN')),
        );
      }
      return;
    }

    // Set new PIN
    final newPin = await _showPinSetupDialog(
      isSetup: true,
      title: l10n.enterNewPin ?? 'Enter New PIN',
      hint: l10n.enterNewPin ?? 'Enter your new PIN',
    );

    if (newPin != null && AppLockService.isValidPin(newPin)) {
      final confirmPin = await _showPinSetupDialog(
        isSetup: true,
        title: l10n.confirmNewPin ?? 'Confirm New PIN',
        hint: l10n.reEnterPin ?? 'Re-enter your new PIN',
      );

      if (confirmPin == newPin) {
        await service.setPin(newPin);
        
        if (mounted) {
          ScaffoldMessenger.of(context).showSnackBar(
            SnackBar(content: Text(l10n.pinChangedSuccessfully ?? 'PIN changed successfully')),
          );
          await _loadSettings();
        }
      } else {
        if (mounted) {
          ScaffoldMessenger.of(context).showSnackBar(
            SnackBar(content: Text(l10n.pinsDoNotMatch ?? 'PINs do not match')),
          );
        }
      }
    } else if (newPin != null && newPin.isNotEmpty) {
      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
          SnackBar(content: Text(l10n.translate('pinLengthRequirement'))),
        );
      }
    }
  }

  Future<String?> _showPinSetupDialog({
    required bool isSetup,
    String? title,
    String? hint,
  }) async {
    final enteredPin = await showDialog<String>(
      context: context,
      barrierDismissible: false,
      builder: (context) => _PinEntryDialog(
        title: title ?? (isSetup ? l10n.setUpPin! : l10n.enterPin!),
        hint: hint ?? (l10n.enterPinCode ?? 'Enter your PIN code'),
      ),
    );
    return enteredPin;
  }

  Future<void> _clearPin() async {
    final confirmed = await showDialog<bool>(
      context: context,
      builder: (context) => AlertDialog(
        title: Text(l10n.clearPin ?? 'Clear PIN'),
        content: Text(l10n.clearPinConfirmation ?? 'Are you sure you want to clear your PIN? This will disable app lock.'),
        actions: [
          TextButton(
            onPressed: () => Navigator.pop(context, false),
            child: Text(l10n.cancel),
          ),
          TextButton(
            onPressed: () => Navigator.pop(context, true),
            style: TextButton.styleFrom(foregroundColor: Colors.red),
            child: Text(l10n.translate('clear')),
          ),
        ],
      ),
    );

    if (confirmed == true) {
      final service = ref.read(appLockServiceProvider);
      await service.clearPin();
      final lockNotifier = ref.read(appLockStateProvider.notifier);
      await lockNotifier.setLockEnabled(false);
      
      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
          SnackBar(content: Text(l10n.pinCleared ?? 'PIN cleared')),
        );
        await _loadSettings();
      }
    }
  }

  AppLocalizations get l10n => AppLocalizations.of(context)!;

  @override
  Widget build(BuildContext context) {
    final lockEnabled = ref.watch(appLockStateProvider);

    if (_isLoading) {
      return Scaffold(
        appBar: AppBar(
          title: Text(l10n.appLock ?? 'App Lock'),
        ),
        body: const Center(child: CircularProgressIndicator()),
      );
    }

    return Scaffold(
      appBar: AppBar(
        title: Text(l10n.appLock ?? 'App Lock'),
      ),
      body: ListView(
        padding: const EdgeInsets.all(16.0),
        children: [
          // Inactivity threshold (only when lock is enabled)
          if (lockEnabled && _hasPin)
            Card(
              child: ListTile(
                leading: const Icon(Icons.timer_outlined, color: Color(0xFF17C3B2)),
                title: Text(l10n.translate('lockAfterInactivity')),
                subtitle: Text('$_inactivitySeconds ${l10n.translate('seconds')}'),
                trailing: DropdownButton<int>(
                  value: _inactivitySeconds,
                  items: [30, 60].map((s) => DropdownMenuItem(value: s, child: Text('$s ${l10n.translate('sec')}'))).toList(),
                  onChanged: (v) async {
                    if (v == null) return;
                    await ref.read(appLockServiceProvider).setInactivitySeconds(v);
                    setState(() => _inactivitySeconds = v);
                  },
                ),
              ),
            ),
          if (lockEnabled && _hasPin) const SizedBox(height: 16),
          // App Lock Toggle
          Card(
            child: SwitchListTile(
              title: Text(l10n.enableAppLock ?? 'Enable App Lock'),
              subtitle: Text(
                lockEnabled
                    ? (l10n.appLockEnabled ?? 'App will lock when you close it')
                    : (l10n.appLockDisabled ?? 'App will not lock'),
              ),
              value: lockEnabled,
              onChanged: _toggleAppLock,
              secondary: const Icon(Icons.lock_outline, color: Color(0xFF17C3B2)),
            ),
          ),
          
          const SizedBox(height: 16),
          
          // PIN Section
          Card(
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Padding(
                  padding: const EdgeInsets.all(16.0),
                  child: Row(
                    children: [
                      const Icon(Icons.pin, color: Color(0xFF17C3B2)),
                      const SizedBox(width: 12),
                      Text(
                        l10n.pinCode ?? 'PIN Code',
                        style: const TextStyle(
                          fontSize: 18,
                          fontWeight: FontWeight.bold,
                        ),
                      ),
                    ],
                  ),
                ),
                const Divider(height: 1),
                if (!_hasPin)
                  ListTile(
                    title: Text(l10n.setUpPin ?? 'Set Up PIN'),
                    subtitle: Text(l10n.setUpPinDescription ?? 'Create a PIN code to secure your app'),
                    trailing: const Icon(Icons.arrow_forward_ios, size: 16),
                    onTap: _setUpPin,
                  )
                else
                  Column(
                  children: [
                    ListTile(
                      title: Text(l10n.changePin ?? 'Change PIN'),
                      subtitle: Text(l10n.changePinDescription ?? 'Update your PIN code'),
                      trailing: const Icon(Icons.arrow_forward_ios, size: 16),
                      onTap: _changePin,
                    ),
                    const Divider(height: 1),
                    ListTile(
                      title: Text(
                        l10n.clearPin ?? 'Clear PIN',
                        style: const TextStyle(color: Colors.red),
                      ),
                      subtitle: Text(l10n.clearPinDescription ?? 'Remove PIN and disable app lock'),
                      trailing: const Icon(Icons.delete_outline, color: Colors.red, size: 20),
                      onTap: _clearPin,
                    ),
                  ],
                ),
              ],
            ),
          ),
        ],
      ),
    );
  }
}

class _PinEntryDialog extends StatefulWidget {
  final String title;
  final String hint;

  const _PinEntryDialog({
    required this.title,
    required this.hint,
  });

  @override
  State<_PinEntryDialog> createState() => _PinEntryDialogState();
}

class _PinEntryDialogState extends State<_PinEntryDialog> {
  String _enteredPin = '';
  final TextEditingController _controller = TextEditingController();

  void _onPinDigitPressed(String digit) {
    if (_enteredPin.length >= AppLockService.maxPinLength) return;

    setState(() {
      _enteredPin += digit;
    });
    _controller.text = _enteredPin;
    HapticFeedback.selectionClick();
  }

  void _submitPin() {
    if (AppLockService.isValidPin(_enteredPin)) {
      Navigator.pop(context, _enteredPin);
    }
  }

  void _onBackspace() {
    if (_enteredPin.isEmpty) return;

    setState(() {
      _enteredPin = _enteredPin.substring(0, _enteredPin.length - 1);
    });
    _controller.text = _enteredPin;
    HapticFeedback.selectionClick();
  }

  @override
  void dispose() {
    _controller.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    return Dialog(
      child: Padding(
        padding: const EdgeInsets.all(24.0),
        child: Column(
          mainAxisSize: MainAxisSize.min,
          children: [
            Text(
              widget.title,
              style: const TextStyle(
                fontSize: 20,
                fontWeight: FontWeight.bold,
              ),
            ),
            const SizedBox(height: 8),
            Text(
              widget.hint,
              style: TextStyle(
                fontSize: 14,
                color: Colors.grey[600],
              ),
              textAlign: TextAlign.center,
            ),
            const SizedBox(height: 24),
            
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
            
            const SizedBox(height: 24),
            
            // PIN Pad
            Column(
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
            ),
            
            const SizedBox(height: 16),
            
            Row(
              mainAxisAlignment: MainAxisAlignment.center,
              children: [
                TextButton(
                  onPressed: () => Navigator.pop(context),
                  child: Text(AppLocalizations.of(context)!.cancel),
                ),
                if (_enteredPin.length >= AppLockService.minPinLength)
                  FilledButton(
                    onPressed: _submitPin,
                    child: Text(AppLocalizations.of(context)!.confirm ?? 'Confirm'),
                  ),
              ],
            ),
          ],
        ),
      ),
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
