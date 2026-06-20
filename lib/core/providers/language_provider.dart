import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:shifa_patient_app_v1/core/utils/app_logger.dart';
import 'package:shifa_patient_app_v1/core/services/push_notification_service.dart';
import 'package:shared_preferences/shared_preferences.dart';

enum AppLanguage {
  english('en', 'English'),
  german('de', 'Deutsch'),
  uzbek('uz', 'O\'zbek'),
  russian('ru', 'Русский');

  final String code;
  final String displayName;

  const AppLanguage(this.code, this.displayName);

  static AppLanguage fromCode(String code) {
    return AppLanguage.values.firstWhere(
      (lang) => lang.code == code,
      orElse: () => AppLanguage.english,
    );
  }

  static AppLanguage fromSystemLocale(Locale locale) {
    final code = locale.languageCode.toLowerCase();
    switch (code) {
      case 'de':
        return AppLanguage.german;
      case 'uz':
        return AppLanguage.uzbek;
      case 'ru':
        return AppLanguage.russian;
      default:
        return AppLanguage.english;
    }
  }
}

class LanguageState {
  final AppLanguage language;
  final Locale locale;

  LanguageState({
    required this.language,
    required this.locale,
  });

  LanguageState copyWith({
    AppLanguage? language,
    Locale? locale,
  }) {
    return LanguageState(
      language: language ?? this.language,
      locale: locale ?? this.locale,
    );
  }
}

class LanguageNotifier extends StateNotifier<LanguageState> {
  LanguageNotifier() : super(_initialState()) {
    _loadLanguage();
  }

  static LanguageState _initialState() {
    // Default to English, will be updated when preferences are loaded
    return LanguageState(
      language: AppLanguage.english,
      locale: const Locale('en'),
    );
  }

  Future<void> _loadLanguage() async {
    try {
      final prefs = await SharedPreferences.getInstance();
      final savedLanguageCode = prefs.getString('app_language');
      
      if (savedLanguageCode != null) {
        final language = AppLanguage.fromCode(savedLanguageCode);
        state = LanguageState(
          language: language,
          locale: Locale(language.code),
        );
        await PushNotificationService().refreshLocalizationCache();
      } else {
        // Detect system language
        final systemLocale = WidgetsBinding.instance.platformDispatcher.locale;
        final language = AppLanguage.fromSystemLocale(systemLocale);
        state = LanguageState(
          language: language,
          locale: Locale(language.code),
        );
        // Save detected language
        await prefs.setString('app_language', language.code);
        await PushNotificationService().refreshLocalizationCache();
      }
    } catch (e) {
      // If error, use default
      state = LanguageState(
        language: AppLanguage.english,
        locale: const Locale('en'),
      );
    }
  }

  Future<void> setLanguage(AppLanguage language, {bool updateBackend = false}) async {
    try {
      final prefs = await SharedPreferences.getInstance();
      await prefs.setString('app_language', language.code);
      
      state = LanguageState(
        language: language,
        locale: Locale(language.code),
      );
      await PushNotificationService().refreshLocalizationCache();

      // Optionally update backend profile if updateBackend is true
      // This will be handled by the profile provider when needed
    } catch (e) {
      // Handle error silently or show a message
      AppLogger.error('Error saving language preference:', e);
    }
  }
}

final languageProvider = StateNotifierProvider<LanguageNotifier, LanguageState>((ref) {
  return LanguageNotifier();
});
