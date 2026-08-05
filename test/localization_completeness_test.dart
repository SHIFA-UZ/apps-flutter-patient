import 'dart:io';

import 'package:flutter_test/flutter_test.dart';

void main() {
  test('UZ and RU localization maps cover every English key', () {
    final text = File('lib/core/localization/app_localizations.dart')
        .readAsStringSync();
    final langs = _parseLangMaps(text);

    expect(langs.containsKey('en'), isTrue);
    expect(langs.containsKey('uz'), isTrue);
    expect(langs.containsKey('ru'), isTrue);

    final en = langs['en']!;
    final uz = langs['uz']!;
    final ru = langs['ru']!;

    final missingUz = en.keys.where((k) => !uz.containsKey(k)).toList()..sort();
    final missingRu = en.keys.where((k) => !ru.containsKey(k)).toList()..sort();

    expect(missingUz, isEmpty, reason: 'Missing UZ keys (${missingUz.length}): $missingUz');
    expect(missingRu, isEmpty, reason: 'Missing RU keys (${missingRu.length}): $missingRu');

    // Catch long English leftovers in RU (missing keys fall back at runtime).
    final englishRu = en.entries
        .where((e) =>
            ru[e.key] == e.value &&
            e.value.length > 24 &&
            !_allowIdentical.contains(e.key))
        .map((e) => e.key)
        .toList();
    expect(englishRu, isEmpty, reason: 'RU identical to EN: $englishRu');
  });
}

const _allowIdentical = {
  'appName',
  'shifaAiTitle',
  'documentCategory_EEG',
};

Map<String, Map<String, String>> _parseLangMaps(String text) {
  final langs = <String, Map<String, String>>{};
  final langRe = RegExp(r"'([a-z]{2})':\s*\{", multiLine: true);
  final matches = langRe.allMatches(text).toList();
  for (var i = 0; i < matches.length; i++) {
    final lang = matches[i].group(1)!;
    final start = matches[i].end;
    final next = i + 1 < matches.length
        ? matches[i + 1].start
        : text.indexOf('\n  };', start);
    final block = text.substring(start, next < 0 ? text.length : next);
    final pairs = <String, String>{};
    for (final m
        in RegExp(r"'((?:\\'|[^'])*)'\s*:\s*'((?:\\'|[^'])*)'").allMatches(block)) {
      pairs[m.group(1)!.replaceAll(r"\'", "'")] =
          m.group(2)!.replaceAll(r"\'", "'");
    }
    langs[lang] = pairs;
  }
  return langs;
}
