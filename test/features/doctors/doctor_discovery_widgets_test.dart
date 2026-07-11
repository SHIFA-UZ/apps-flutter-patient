import 'package:flutter/material.dart';
import 'package:flutter_localizations/flutter_localizations.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:shifa_patient_app_v1/core/localization/app_localizations.dart';
import 'package:shifa_patient_app_v1/core/models/doctor_model.dart';
import 'package:shifa_patient_app_v1/features/doctors/presentation/widgets/doctor_discovery_card.dart';
import 'package:shifa_patient_app_v1/features/doctors/presentation/widgets/doctors_pagination_bar.dart';

Widget _wrap(Widget child, {Locale locale = const Locale('uz')}) {
  return MaterialApp(
    locale: locale,
    localizationsDelegates: const [
      AppLocalizations.delegate,
      GlobalMaterialLocalizations.delegate,
      GlobalWidgetsLocalizations.delegate,
      GlobalCupertinoLocalizations.delegate,
    ],
    supportedLocales: const [
      Locale('en'),
      Locale('de'),
      Locale('uz'),
      Locale('ru'),
    ],
    home: Scaffold(body: SingleChildScrollView(child: child)),
  );
}

void main() {
  group('DoctorDiscoveryCard', () {
    const doctor = DoctorModel(
      id: '1',
      firstName: 'Test',
      lastName: 'Doctor',
      fullName: 'Test Doctor',
      profession: 'Dentist',
      city: 'Toshkent',
      locationCountry: 'Uzbekistan',
      supportsOnline: true,
      supportsInPerson: true,
      nextAvailableStartAt: '2030-01-07T10:00:00.000Z',
      minPriceMinor: 12000000,
      minPriceCurrency: 'UZS',
      certificates: const ['https://example.com/cert.pdf'],
    );

    testWidgets('shows badges, book CTA, and localized weekday slot (uz)', (tester) async {
      await tester.pumpWidget(
        _wrap(
          DoctorDiscoveryCard(
            doctor: doctor,
            onBook: () {},
            onProfile: () {},
          ),
        ),
      );
      await tester.pumpAndSettle();

      expect(find.text('Onlayn'), findsOneWidget);
      expect(find.text('Klinika'), findsOneWidget);
      expect(find.textContaining('dush'), findsOneWidget);
      expect(find.text('Uchrashuv bron qilish'), findsOneWidget);
    });

    testWidgets('expand reveals profile link', (tester) async {
      await tester.pumpWidget(
        _wrap(
          DoctorDiscoveryCard(
            doctor: doctor,
            onBook: () {},
            onProfile: () {},
          ),
        ),
      );
      await tester.pumpAndSettle();

      await tester.tap(find.byIcon(Icons.expand_more));
      await tester.pumpAndSettle();

      expect(find.text('Profilni ko\'rish'), findsOneWidget);
      expect(find.text('Tasdiqlangan'), findsOneWidget);
    });
  });

  group('DoctorsPaginationBar', () {
    testWidgets('renders page controls without overflow on narrow width', (tester) async {
      await tester.binding.setSurfaceSize(const Size(320, 600));
      addTearDown(() => tester.binding.setSurfaceSize(null));

      await tester.pumpWidget(
        _wrap(
          DoctorsPaginationBar(
            currentPage: 2,
            totalPages: 5,
            totalCount: 100,
            onPageSelected: (_) {},
          ),
        ),
      );
      await tester.pumpAndSettle();

      expect(find.text('2'), findsOneWidget);
      expect(tester.takeException(), isNull);
    });

    testWidgets('hidden when only one page', (tester) async {
      await tester.pumpWidget(
        _wrap(
          DoctorsPaginationBar(
            currentPage: 1,
            totalPages: 1,
            totalCount: 10,
            onPageSelected: (_) {},
          ),
        ),
      );
      await tester.pumpAndSettle();

      expect(find.byType(DoctorsPaginationBar), findsOneWidget);
      expect(find.text('1'), findsNothing);
    });
  });
}
