import 'dart:async';
import 'dart:math' as math;

import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';
import 'package:shifa_patient_app_v1/app/router.dart';
import 'package:shifa_patient_app_v1/core/constants/assets.dart';
import 'package:shifa_patient_app_v1/features/auth/providers/auth_provider.dart';

class SplashScreen extends ConsumerStatefulWidget {
  const SplashScreen({super.key});

  @override
  ConsumerState<SplashScreen> createState() => _SplashScreenState();
}

class _SplashScreenState extends ConsumerState<SplashScreen>
    with SingleTickerProviderStateMixin {
  late final AnimationController _c;
  late final Animation<double> _t;
  late final DateTime _startedAt;
  bool _navigated = false;
  Timer? _fallbackTimer;

  static const _minSplashDuration = Duration(milliseconds: 1600);
  static const _authWaitTimeout = Duration(seconds: 4);
  static const _hardFallback = Duration(seconds: 5);

  @override
  void initState() {
    super.initState();
    _startedAt = DateTime.now();
    _c = AnimationController(vsync: this, duration: _minSplashDuration);
    _t = CurvedAnimation(parent: _c, curve: Curves.easeInOutCubic)
      ..addStatusListener((status) {
        if (status == AnimationStatus.completed) {
          unawaited(_navigateAway());
        }
      });
    _c.forward();
    _fallbackTimer = Timer(_hardFallback, () => unawaited(_navigateAway()));
  }

  Future<void> _navigateAway() async {
    if (_navigated || !mounted) return;
    _navigated = true;
    _fallbackTimer?.cancel();

    final elapsed = DateTime.now().difference(_startedAt);
    final remaining = _minSplashDuration - elapsed;
    if (remaining > Duration.zero) {
      await Future<void>.delayed(remaining);
    }

    try {
      await ref
          .read(authStateProvider.notifier)
          .ensureInitialAuthChecked()
          .timeout(_authWaitTimeout);
    } catch (_) {
      // Auth restore timed out — continue with best-known auth state.
    }

    if (!mounted || !context.mounted) return;

    final auth = ref.read(authStateProvider);
    final destination = !auth.isAuthenticated
        ? AppRoutes.login
        : auth.forcePasswordReset
            ? AppRoutes.resetPassword
            : AppRoutes.home;

    context.go(destination);
  }

  @override
  void dispose() {
    _fallbackTimer?.cancel();
    _c.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    const brand = Color(0xFF17C3B2);
    return AnimatedBuilder(
      animation: _t,
      builder: (context, _) {
        final size = MediaQuery.of(context).size;
        final w = size.width;
        final h = size.height;

        final bg = Color.lerp(brand, Colors.white, _t.value)!;

        final blTranslate = Offset(
          _lerp(-0.15 * w, -0.55 * w, _t.value),
          _lerp(0.25 * h, 0.55 * h, _t.value),
        );
        final blScale = _lerp(1.35, 1.05, _t.value);
        final blRotation = _lerp(-10, -25, _t.value) * math.pi / 180.0;

        final trTranslate = Offset(
          _lerp(0.10 * w, 0.55 * w, _t.value),
          _lerp(-0.20 * h, -0.45 * h, _t.value),
        );
        final trScale = _lerp(1.10, 0.95, _t.value);
        final trRotation = _lerp(10, 22, _t.value) * math.pi / 180.0;

        final logoScale = _lerp(0.96, 1.00, _t.value);
        final logoOpacity = Curves.easeIn.transform(_t.value.clamp(0.0, 1.0));

        return Scaffold(
          backgroundColor: bg,
          body: Stack(
            fit: StackFit.expand,
            children: [
              _Blob(
                baseSize: math.max(w, h) * 1.0,
                translate: blTranslate,
                scale: blScale,
                rotation: blRotation,
                color: brand,
              ),
              _Blob(
                baseSize: math.max(w, h) * 0.95,
                translate: trTranslate,
                scale: trScale,
                rotation: trRotation,
                color: brand.withAlpha((0.90 * 255).toInt()),
              ),
              Center(
                child: Opacity(
                  opacity: logoOpacity,
                  child: Transform.scale(
                    scale: logoScale,
                    child: ClipRRect(
                      borderRadius: BorderRadius.circular(20),
                      child: Image.asset(
                        Assets.shifaLogoPng,
                        width: 96,
                        height: 96,
                        fit: BoxFit.contain,
                      ),
                    ),
                  ),
                ),
              ),
            ],
          ),
        );
      },
    );
  }

  double _lerp(double a, double b, double t) => a + (b - a) * t;
}

/// Soft gradient blobs — avoids ImageFilter.blur, which can freeze low-end Android GPUs.
class _Blob extends StatelessWidget {
  final double baseSize;
  final Offset translate;
  final double scale;
  final double rotation;
  final Color color;

  const _Blob({
    required this.baseSize,
    required this.translate,
    required this.scale,
    required this.rotation,
    required this.color,
  });

  @override
  Widget build(BuildContext context) {
    final size = baseSize * scale;
    return Transform.translate(
      offset: translate,
      child: Transform.rotate(
        angle: rotation,
        child: Container(
          width: size * 1.2,
          height: size * 0.85,
          decoration: BoxDecoration(
            borderRadius: BorderRadius.circular(size * 0.45),
            gradient: RadialGradient(
              center: Alignment.center,
              radius: 0.85,
              colors: [
                color.withAlpha((0.70 * 255).toInt()),
                color.withAlpha((0.25 * 255).toInt()),
                color.withAlpha(0),
              ],
              stops: const [0.0, 0.55, 1.0],
            ),
          ),
        ),
      ),
    );
  }
}
