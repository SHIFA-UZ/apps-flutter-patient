import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:shifa_patient_app_v1/core/layout/responsive_layout.dart';
import 'package:shifa_patient_app_v1/core/theme/app_design_system.dart';

void main() {
  testWidgets('ResponsiveLayout centers content on wide viewports', (tester) async {
    late double inset;
    await tester.pumpWidget(
      MaterialApp(
        home: MediaQuery(
          data: const MediaQueryData(size: Size(1200, 800)),
          child: Builder(
            builder: (context) {
              inset = ResponsiveLayout.horizontalInset(context);
              return const SizedBox.shrink();
            },
          ),
        ),
      ),
    );

    expect(inset, (1200 - ResponsiveLayout.contentMaxWidth) / 2);
    expect(inset, greaterThan(AppDesignSystem.screenPaddingH));
  });

  testWidgets('ResponsiveLayout uses minimum gutter on narrow viewports', (tester) async {
    late double inset;
    await tester.pumpWidget(
      MaterialApp(
        home: MediaQuery(
          data: const MediaQueryData(size: Size(360, 640)),
          child: Builder(
            builder: (context) {
              inset = ResponsiveLayout.horizontalInset(context);
              return const SizedBox.shrink();
            },
          ),
        ),
      ),
    );

    expect(inset, AppDesignSystem.screenPaddingH);
  });
}
