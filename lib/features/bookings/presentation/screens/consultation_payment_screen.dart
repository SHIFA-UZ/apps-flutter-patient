import 'dart:async';

import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';
import 'package:shifa_patient_app_v1/app/router.dart';
import 'package:shifa_patient_app_v1/core/localization/app_localizations.dart';
import 'package:shifa_patient_app_v1/features/bookings/data/bookings_repository.dart';
import 'package:shifa_patient_app_v1/features/bookings/presentation/widgets/payment_gateway_chooser.dart';
import 'package:webview_flutter/webview_flutter.dart';

/// Opens Stripe (or other gateway) checkout for a single consultation payment.
/// Used from deep link `/bookings/:id/pay` (e.g. doctor "remind to pay" notification).
class ConsultationPaymentScreen extends ConsumerStatefulWidget {
  const ConsultationPaymentScreen({
    super.key,
    required this.appointmentId,
    this.gateway,
  });

  final String appointmentId;
  final String? gateway;

  @override
  ConsumerState<ConsultationPaymentScreen> createState() =>
      _ConsultationPaymentScreenState();
}

class _ConsultationPaymentScreenState extends ConsumerState<ConsultationPaymentScreen> {
  String? _checkoutUrl;
  String? _paymentId;
  String? _checkoutGateway;
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
      _checkoutGateway = null;
    });
    try {
      final checkout = await ref.read(bookingsRepositoryProvider).createConsultationCheckout(
            appointmentId: widget.appointmentId,
            gateway: gateway,
          );
      final paymentId = checkout['paymentId']?.toString();
      final checkoutUrl = checkout['checkoutUrl']?.toString();
      final checkoutGateway = checkout['gateway']?.toString();
      if (!mounted) return;
      if (paymentId == null ||
          checkoutUrl == null ||
          checkoutUrl.isEmpty) {
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
        _checkoutGateway = checkoutGateway;
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
        appBar: AppBar(
          title: Text(l10n.translate('completePayment')),
        ),
        body: const Center(child: CircularProgressIndicator()),
      );
    }
    if (_error != null) {
      return Scaffold(
        appBar: AppBar(
          title: Text(l10n.translate('completePayment')),
        ),
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
                  onPressed: () =>
                      context.go('${AppRoutes.bookings}/${widget.appointmentId}'),
                  child: Text(l10n.translate('back')),
                ),
              ],
            ),
          ),
        ),
      );
    }
    return _ConsultationPaymentWebView(
      appointmentId: widget.appointmentId,
      checkoutGateway: _checkoutGateway,
      paymentId: _paymentId!,
      checkoutUrl: _checkoutUrl!,
    );
  }
}

class _ConsultationPaymentWebView extends ConsumerStatefulWidget {
  const _ConsultationPaymentWebView({
    required this.paymentId,
    required this.checkoutUrl,
    required this.appointmentId,
    this.checkoutGateway,
  });

  final String paymentId;
  final String checkoutUrl;
  final String appointmentId;
  final String? checkoutGateway;

  @override
  ConsumerState<_ConsultationPaymentWebView> createState() =>
      _ConsultationPaymentWebViewState();
}

class _ConsultationPaymentWebViewState
    extends ConsumerState<_ConsultationPaymentWebView> {
  late final WebViewController _controller;
  late String _pollPaymentId;
  Timer? _pollingTimer;
  bool _checkingStatus = false;

  @override
  void initState() {
    super.initState();
    _pollPaymentId = widget.paymentId;
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
          .getPaymentStatus(_pollPaymentId);
      final paymentStatus = (status['status']?.toString() ?? '').toUpperCase();
      if (paymentStatus == 'PAID' && mounted && forceCloseOnPaid) {
        if (context.mounted) {
          ScaffoldMessenger.of(context).showSnackBar(
            SnackBar(
              content: Text(
                AppLocalizations.of(context)!
                    .translate('paymentCompletedAppointmentConfirmed'),
              ),
              backgroundColor: Colors.green,
            ),
          );
          context.go('${AppRoutes.bookings}/${widget.appointmentId}');
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
      appBar: AppBar(
        title: Text(l10n.translate('completePayment')),
      ),
      body: WebViewWidget(controller: _controller),
    );
  }
}
