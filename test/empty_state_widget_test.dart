import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:shifa_patient_app_v1/core/theme/app_design_system.dart';
import 'package:shifa_patient_app_v1/core/widgets/empty_state.dart';

void main() {
  testWidgets('EmptyState shows message and optional action', (tester) async {
    await tester.pumpWidget(
      MaterialApp(
        home: Scaffold(
          body: EmptyState(
            icon: Icons.folder_open,
            message: 'Nothing here',
            action: TextButton(onPressed: () {}, child: const Text('Go')),
          ),
        ),
      ),
    );

    expect(find.text('Nothing here'), findsOneWidget);
    expect(find.text('Go'), findsOneWidget);
    expect(find.byIcon(Icons.folder_open), findsOneWidget);
  });

  testWidgets('EmptyState scrolls with long message', (tester) async {
    final long = List.filled(40, 'word').join(' ');
    await tester.pumpWidget(
      MaterialApp(
        theme: ThemeData(
          textTheme: const TextTheme(
            bodyMedium: TextStyle(fontSize: AppDesignSystem.body2Size),
          ),
        ),
        home: Scaffold(
          body: EmptyState(icon: Icons.inbox, message: long),
        ),
      ),
    );

    expect(find.textContaining('word'), findsWidgets);
  });
}
