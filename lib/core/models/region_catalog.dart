/// Uzbekistan administrative regions (viloyatlar) for doctor discovery filters.
/// Backend may store Latin Uzbek, English geocoder names, or city names — aliases normalize matching.
class RegionCatalogEntry {
  final String canonical;
  final String uzbek;
  final String english;
  final List<String> aliases;

  const RegionCatalogEntry({
    required this.canonical,
    required this.uzbek,
    required this.english,
    this.aliases = const [],
  });

  bool matches(String value) {
    final v = value.trim().toLowerCase();
    if (v.isEmpty) return false;
    if (canonical.toLowerCase() == v) return true;
    if (uzbek.toLowerCase() == v) return true;
    if (english.toLowerCase() == v) return true;
    return aliases.any((a) => a.toLowerCase() == v);
  }
}

class UzbekistanRegions {
  static const List<RegionCatalogEntry> all = [
    RegionCatalogEntry(
      canonical: 'Toshkent shahri',
      uzbek: 'Toshkent shahri',
      english: 'Tashkent City',
      aliases: ['Toshkent', 'Tashkent', 'Tashkent City'],
    ),
    RegionCatalogEntry(
      canonical: 'Toshkent viloyati',
      uzbek: 'Toshkent viloyati',
      english: 'Tashkent Region',
      aliases: ['Tashkent Region'],
    ),
    RegionCatalogEntry(
      canonical: 'Andijon viloyati',
      uzbek: 'Andijon viloyati',
      english: 'Andijan Region',
      aliases: ['Andijon', 'Andijan'],
    ),
    RegionCatalogEntry(
      canonical: 'Buxoro viloyati',
      uzbek: 'Buxoro viloyati',
      english: 'Bukhara Region',
      aliases: ['Buxoro', 'Bukhara'],
    ),
    RegionCatalogEntry(
      canonical: "Farg'ona viloyati",
      uzbek: "Farg'ona viloyati",
      english: 'Fergana Region',
      aliases: ['Fargona', 'Fergana', "Farg'ona"],
    ),
    RegionCatalogEntry(
      canonical: 'Jizzax viloyati',
      uzbek: 'Jizzax viloyati',
      english: 'Jizzakh Region',
      aliases: ['Jizzax', 'Jizzakh'],
    ),
    RegionCatalogEntry(
      canonical: 'Xorazm viloyati',
      uzbek: 'Xorazm viloyati',
      english: 'Khorezm Region',
      aliases: ['Xorazm', 'Khorezm', 'Khiva'],
    ),
    RegionCatalogEntry(
      canonical: 'Namangan viloyati',
      uzbek: 'Namangan viloyati',
      english: 'Namangan Region',
      aliases: ['Namangan'],
    ),
    RegionCatalogEntry(
      canonical: 'Navoiy viloyati',
      uzbek: 'Navoiy viloyati',
      english: 'Navoi Region',
      aliases: ['Navoiy', 'Navoi'],
    ),
    RegionCatalogEntry(
      canonical: 'Qashqadaryo viloyati',
      uzbek: 'Qashqadaryo viloyati',
      english: 'Kashkadarya Region',
      aliases: ['Qashqadaryo', 'Kashkadarya'],
    ),
    RegionCatalogEntry(
      canonical: 'Samarqand viloyati',
      uzbek: 'Samarqand viloyati',
      english: 'Samarkand Region',
      aliases: ['Samarqand', 'Samarkand'],
    ),
    RegionCatalogEntry(
      canonical: 'Sirdaryo viloyati',
      uzbek: 'Sirdaryo viloyati',
      english: 'Syrdarya Region',
      aliases: ['Sirdaryo', 'Syrdarya'],
    ),
    RegionCatalogEntry(
      canonical: 'Surxondaryo viloyati',
      uzbek: 'Surxondaryo viloyati',
      english: 'Surkhandarya Region',
      aliases: ['Surxondaryo', 'Surkhandarya', 'Termiz'],
    ),
    RegionCatalogEntry(
      canonical: "Qoraqalpog'iston Respublikasi",
      uzbek: "Qoraqalpog'iston Respublikasi",
      english: 'Republic of Karakalpakstan',
      aliases: ['Karakalpakstan', 'Nukus', "Qoraqalpog'iston"],
    ),
  ];

  static List<String> get canonicalValues =>
      all.map((e) => e.canonical).toList(growable: false);

  static RegionCatalogEntry? findMatch(String? value) {
    if (value == null || value.trim().isEmpty) return null;
    for (final entry in all) {
      if (entry.matches(value)) return entry;
    }
    return null;
  }

  static String displayLabel(String canonicalOrAlias, String? languageCode) {
    final entry = findMatch(canonicalOrAlias);
    if (entry == null) return canonicalOrAlias;
    if (languageCode == 'uz' || languageCode == 'uz_UZ') return entry.uzbek;
    if (languageCode == 'ru') return entry.english;
    return entry.english;
  }

  /// Canonical filter value sent to the API for a stored region/city string.
  static String? canonicalFor(String? value) => findMatch(value)?.canonical;

  static List<String> sortKeysForDisplay(
    Iterable<String> keys,
    String? languageCode,
  ) {
    final list = keys.toList();
    list.sort((a, b) {
      final la = displayLabel(a, languageCode).toLowerCase();
      final lb = displayLabel(b, languageCode).toLowerCase();
      return la.compareTo(lb);
    });
    return list;
  }
}
