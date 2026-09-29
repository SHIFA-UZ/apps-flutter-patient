import 'dart:async';

import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';
import 'package:shifa_patient_app_v1/app/router.dart';
import 'package:shifa_patient_app_v1/core/localization/app_localizations.dart';
import 'package:shifa_patient_app_v1/features/bookings/data/bookings_repository.dart';
import 'package:shifa_patient_app_v1/features/bookings/presentation/widgets/payment_gateway_chooser.dart';
import 'package:webview_flutter/webview_flutter.dart';

/// Opens Click checkout for a treatment-plan bill (full outstanding or one installment).
class TreatmentPlanPaymentScreen extends ConsumerStatefulWidget {
  const TreatmentPlanPaymentScreen({
    super.key,
    required this.planId,
    this.installmentItemId,
    this.gateway,
  });

  final String planId;
  final String? installmentItemId;
  final String? gateway;

  @override
  ConsumerState<TreatmentPlanPaymentScreen> createState() =>
      _TreatmentPlanPaymentScreenState();
}

class _TreatmentPlanPaymentScreenState
    extends ConsumerState<TreatmentPlanPaymentScreen> {
  String? _checkoutUrl;
  String? _paymentId;
  String? _error;
  bool _loading = true;
  String? _chosenGateway;

  @override
  void initState() {
    super.initState();
    _chosenGateway = widget.gateway?.toUpperCase();
    WidgetsBinding.instance.addPostFrameCallback((_) {
      if (_chosenGateway == null) {
        if (mounted) setState(() => _loading = false);
      } else {
        _startCheckout();
      }
    });
  }

  Future<void> _startCheckout() async {
    final gateway = _chosenGateway;
    if (gateway == null) return;
    final l10n = AppLocalizations.of(context)!;
    setState(() {
      _loading = true;
      _error = null;
      _checkoutUrl = null;
      _paymentId = null;
    });
    try {
      final checkout =
          await ref.read(bookingsRepositoryProvider).createTreatmentPlanCheckout(
                treatmentPlanId: widget.planId,
                installmentItemId: widget.installmentItemId,
                gateway: gateway,
              );
      final paymentId = checkout['paymentId']?.toString();
      final checkoutUrl = checkout['checkoutUrl']?.toString();
      if (!mounted) return;
      if (paymentId == null || checkoutUrl == null || checkoutUrl.isEmpty) {
        setState(() {
          _loading = false;
          _error = l10n.translate('paymentCouldNotStart');
        });
        return;
      }
      setState(() {
        _loading = false;
        _paymentId = paymentId;
        _checkoutUrl = checkoutUrl;
      });
    } catch (e) {
      if (!mounted) return;
      setState(() {
        _loading = false;
        _error = l10n
            .translate('paymentCouldNotStartWithError')
            .replaceAll('{{error}}', '$e');
      });
    }
  }

  @override
  Widget build(BuildContext context) {
    final l10n = AppLocalizations.of(context)!;
    if (_chosenGateway == null && !_loading && _error == null) {
      return Scaffold(
        appBar: AppBar(title: Text(l10n.translate('completePayment'))),
        body: Padding(
          padding: const EdgeInsets.all(24),
          child: PaymentGatewayChooser(
            onSelected: (gateway) {
              setState(() => _chosenGateway = gateway);
              _startCheckout();
            },
          ),
        ),
      );
    }
    if (_loading) {
      return Scaffold(
        appBar: AppBar(title: Text(l10n.translate('completePayment'))),
        body: const Center(child: CircularProgressIndicator()),
      );
    }
    if (_error != null) {
      return Scaffold(
        appBar: AppBar(title: Text(l10n.translate('completePayment'))),
        body: Center(
          child: Padding(
            padding: const EdgeInsets.all(24),
            child: Column(
              mainAxisSize: MainAxisSize.min,
              children: [
                Text(_error!, textAlign: TextAlign.center),
                const SizedBox(height: 16),
                FilledButton(
                  onPressed: _startCheckout,
                  child: Text(l10n.translate('retry')),
                ),
                TextButton(
                  onPressed: () => context.go(
                    '${AppRoutes.bookings}/treatment-plan/${widget.planId}',
                  ),
                  child: Text(l10n.translate('back')),
                ),
              ],
            ),
          ),
        ),
      );
    }
    return _TreatmentPlanPaymentWebView(
      planId: widget.planId,
      paymentId: _paymentId!,
      checkoutUrl: _checkoutUrl!,
    );
  }
}

class _TreatmentPlanPaymentWebView extends ConsumerStatefulWidget {
  const _TreatmentPlanPaymentWebView({
    required this.paymentId,
    required this.checkoutUrl,
    required this.planId,
  });

  final String paymentId;
  final String checkoutUrl;
  final String planId;

  @override
  ConsumerState<_TreatmentPlanPaymentWebView> createState() =>
      _TreatmentPlanPaymentWebViewState();
}

class _TreatmentPlanPaymentWebViewState
    extends ConsumerState<_TreatmentPlanPaymentWebView> {
  late final WebViewController _controller;
  Timer? _pollingTimer;
  bool _checkingStatus = false;

  @override
  void initState() {
    super.initState();
    _controller = WebViewController()
      ..setJavaScriptMode(JavaScriptMode.unrestricted)
      ..setNavigationDelegate(
        NavigationDelegate(
          onNavigationRequest: (request) {
            final url = request.url;
            if (url.contains('/api/payments/checkout/success')) {
              _checkPaymentStatus(forceCloseOnPaid: true);
              return NavigationDecision.prevent;
            }
            if (url.contains('/api/payments/checkout/cancel')) {
              if (mounted) Navigator.of(context).pop();
              return NavigationDecision.prevent;
            }
            return NavigationDecision.navigate;
          },
        ),
      )
      ..loadRequest(Uri.parse(widget.checkoutUrl));

    _pollingTimer = Timer.periodic(
      const Duration(seconds: 3),
      (_) => _checkPaymentStatus(forceCloseOnPaid: true),
    );
  }

  Future<void> _checkPaymentStatus({bool forceCloseOnPaid = false}) async {
    if (_checkingStatus || !mounted) return;
    _checkingStatus = true;
    try {
      final status = await ref
          .read(bookingsRepositoryProvider)
          .getPaymentStatus(widget.paymentId);
      final paymentStatus = (status['status']?.toString() ?? '').toUpperCase();
      if (paymentStatus == 'PAID' && mounted && forceCloseOnPaid) {
        if (context.mounted) {
          ScaffoldMessenger.of(context).showSnackBar(
            SnackBar(
              content: Text(
                AppLocalizations.of(context)!.treatmentPlanPaymentSuccess,
              ),
              backgroundColor: Colors.green,
            ),
          );
          context.go('${AppRoutes.bookings}/treatment-plan/${widget.planId}');
        }
      }
    } catch (_) {
      // Ignore transient polling errors.
    } finally {
      _checkingStatus = false;
    }
  }

  @override
  void dispose() {
    _pollingTimer?.cancel();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    final l10n = AppLocalizations.of(context)!;
    return Scaffold(
      appBar: AppBar(title: Text(l10n.translate('completePayment'))),
      body: WebViewWidget(controller: _controller),
    );
  }
}
