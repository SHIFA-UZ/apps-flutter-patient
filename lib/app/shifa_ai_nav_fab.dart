import 'package:flutter/material.dart';
import 'package:flutter_svg/flutter_svg.dart';
import 'package:shifa_patient_app_v1/core/constants/assets.dart';
import 'package:shifa_patient_app_v1/core/theme/app_design_system.dart';

/// Shifa AI center nav FAB — radial teal disc, soft outer aura, teal glow (matches brand palette).
/// Uses [OverflowBox] so aura/glow can extend past layout [size] without expanding nav constraints.
class ShifaAiNavFab extends StatefulWidget {
  const ShifaAiNavFab({
    super.key,
    required this.size,
    required this.onPressed,
    this.active = false,
  });

  final double size;
  final VoidCallback onPressed;
  /// True when co-pilot route is visible — slightly richer idle glow.
  final bool active;

  @override
  State<ShifaAiNavFab> createState() => _ShifaAiNavFabState();
}

class _ShifaAiNavFabState extends State<ShifaAiNavFab> with TickerProviderStateMixin {
  late final AnimationController _pulse;
  late final AnimationController _press;

  static const double _auraExpand = 12;

  @override
  void initState() {
    super.initState();
    _pulse = AnimationController(
      vsync: this,
      duration: const Duration(milliseconds: 4200),
    )..repeat(reverse: true);
    _press = AnimationController(
      vsync: this,
      duration: const Duration(milliseconds: 130),
      lowerBound: 0,
      upperBound: 1,
    );
  }

  @override
  void dispose() {
    _pulse.dispose();
    _press.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    return Semantics(
      button: true,
      label: 'Shifa AI',
      child: AnimatedBuilder(
        animation: Listenable.merge([_pulse, _press]),
        builder: (context, _) {
          final pulse = CurvedAnimation(parent: _pulse, curve: Curves.easeInOutSine).value;
          final press = CurvedAnimation(parent: _press, curve: Curves.easeOutCubic).value;

          final pulseBreath = 1.0 + pulse * 0.012;
          final scale = pulseBreath * (1.0 - 0.048 * press);

          final glowBoost = widget.active ? 1.18 : 1.0;
          final glowOpacity =
              ((0.11 + pulse * 0.07) * glowBoost * (1.0 + 0.5 * press)).clamp(0.07, 0.32);
          final glowBlur = 16.0 + pulse * 5.5 + press * 11.0;
          final glowBlurOuter = 26.0 + pulse * 7.0 + press * 13.0;

          final s = widget.size;

          /// Deeper center → [primaryAi] body → lighter turquoise rim (mock-aligned #26BAA4 band).
          final discGradient = RadialGradient(
            center: const Alignment(-0.2, -0.26),
            radius: 1.02,
            colors: [
              AppDesignSystem.primaryDark,
              Color.lerp(AppDesignSystem.primaryDark, AppDesignSystem.primaryAi, 0.42)!,
              AppDesignSystem.primaryAi,
              Color.lerp(AppDesignSystem.primaryAi, AppDesignSystem.primaryLight, 0.5)!,
              Color.lerp(AppDesignSystem.primaryLight, AppDesignSystem.secondaryLight, 0.55)!,
            ],
            stops: const [0.0, 0.22, 0.46, 0.72, 1.0],
          );

          return SizedBox(
            width: s,
            height: s,
            child: OverflowBox(
              maxWidth: s + _auraExpand,
              maxHeight: s + _auraExpand,
              alignment: Alignment.center,
              child: Transform.scale(
                scale: scale,
                alignment: Alignment.center,
                child: SizedBox(
                  width: s + _auraExpand,
                  height: s + _auraExpand,
                  child: Stack(
                    alignment: Alignment.center,
                    clipBehavior: Clip.none,
                    children: [
                      // Wide soft teal wash (outer halo — “smart” glow)
                      Container(
                        width: s + 10,
                        height: s + 10,
                        decoration: BoxDecoration(
                          shape: BoxShape.circle,
                          gradient: RadialGradient(
                            colors: [
                              AppDesignSystem.primaryAi.withValues(alpha: 0),
                              AppDesignSystem.primaryAi.withValues(alpha: 0.11),
                            ],
                          ),
                          boxShadow: [
                            BoxShadow(
                              color: AppDesignSystem.primaryLight.withValues(alpha: glowOpacity * 0.52),
                              blurRadius: glowBlurOuter,
                              spreadRadius: -3,
                            ),
                            BoxShadow(
                              color: AppDesignSystem.primaryAi.withValues(alpha: glowOpacity * 0.44),
                              blurRadius: glowBlur,
                              spreadRadius: -1,
                            ),
                          ],
                        ),
                      ),
                      // Semi-transparent “glass” ring behind the solid disc
                      Container(
                        width: s + 7,
                        height: s + 7,
                        decoration: BoxDecoration(
                          shape: BoxShape.circle,
                          border: Border.all(
                            width: 2,
                            color: Color.lerp(
                              AppDesignSystem.primaryAi,
                              AppDesignSystem.primaryLight,
                              0.35,
                            )!
                                .withValues(alpha: 0.48),
                          ),
                          boxShadow: [
                            BoxShadow(
                              color: AppDesignSystem.secondaryLight.withValues(alpha: 0.35),
                              blurRadius: 8,
                              spreadRadius: -1,
                            ),
                          ],
                        ),
                      ),
                      // Main radial button + float shadow (layout-sized disc)
                      Container(
                        width: s,
                        height: s,
                        decoration: BoxDecoration(
                          shape: BoxShape.circle,
                          gradient: discGradient,
                          boxShadow: [
                            BoxShadow(
                              color: Colors.black.withValues(alpha: 0.06),
                              blurRadius: 14,
                              offset: const Offset(0, 6),
                            ),
                            BoxShadow(
                              color: AppDesignSystem.primaryDark.withValues(alpha: 0.09),
                              blurRadius: 10,
                              offset: const Offset(0, 3),
                            ),
                          ],
                        ),
                        child: Material(
                          type: MaterialType.transparency,
                          child: InkWell(
                            customBorder: const CircleBorder(),
                            splashColor: AppDesignSystem.white.withValues(alpha: 0.22),
                            highlightColor: AppDesignSystem.white.withValues(alpha: 0.07),
                            onTapDown: (_) => _press.forward(),
                            onTapCancel: () => _press.reverse(),
                            onTap: () {
                              widget.onPressed();
                              _press.reverse();
                            },
                            child: Stack(
                              clipBehavior: Clip.none,
                              alignment: Alignment.center,
                              children: [
                                // Soft inner highlight (top-lit, still flat)
                                Positioned.fill(
                                  child: IgnorePointer(
                                    child: DecoratedBox(
                                      decoration: BoxDecoration(
                                        shape: BoxShape.circle,
                                        gradient: RadialGradient(
                                          center: const Alignment(-0.35, -0.45),
                                          radius: 0.92,
                                          colors: [
                                            AppDesignSystem.white.withValues(alpha: 0.2),
                                            AppDesignSystem.white.withValues(alpha: 0),
                                          ],
                                          stops: const [0.0, 0.55],
                                        ),
                                      ),
                                    ),
                                  ),
                                ),
                                Padding(
                                  padding: EdgeInsets.all(s * 0.182),
                                  child: SvgPicture.asset(
                                    Assets.shifaAiIcon,
                                    fit: BoxFit.contain,
                                    alignment: Alignment.center,
                                    colorFilter: const ColorFilter.mode(
                                      AppDesignSystem.white,
                                      BlendMode.srcIn,
                                    ),
                                  ),
                                ),
                              ],
                            ),
                          ),
                        ),
                      ),
                    ],
                  ),
                ),
              ),
            ),
          );
        },
      ),
    );
  }
}
