// Profession model with English and Uzbek translations
class ProfessionModel {
  final String english;
  final String uzbek;

  const ProfessionModel({
    required this.english,
    required this.uzbek,
  });

  // Get display text based on language - show only one language
  String getDisplayText(String? language) {
    if (language == 'uz' || language == 'uz_UZ') {
      return uzbek;
    }
    return english;
  }

  /// Shorter label for filter chips / dropdowns when the English name is long.
  String getShortDisplayText(String? language) {
    return getDisplayText(language);
  }

  // Search helper - checks both English and Uzbek
  bool matches(String query) {
    final lowerQuery = query.toLowerCase();
    return english.toLowerCase().contains(lowerQuery) ||
        uzbek.toLowerCase().contains(lowerQuery);
  }
}

class ProfessionData {
  static const List<ProfessionModel> allProfessions = [
    // 🩺 General & Primary Care
    ProfessionModel(english: 'General Practitioner (GP)', uzbek: 'Umumiy amaliyot shifokori'),
    ProfessionModel(english: 'Family Physician', uzbek: 'Oilaviy shifokor'),
    ProfessionModel(english: 'Internist (Internal Medicine)', uzbek: 'Terapevt'),
    ProfessionModel(english: 'Pediatrician', uzbek: 'Pediatr'),
    ProfessionModel(english: 'Geriatrician', uzbek: 'Geriatr (keksalar shifokori)'),

    // 🫀 Medical (Non-Surgical) Specialties
    ProfessionModel(english: 'Cardiologist', uzbek: 'Kardiolog'),
    ProfessionModel(english: 'Endocrinologist', uzbek: 'Endokrinolog'),
    ProfessionModel(english: 'Gastroenterologist', uzbek: 'Gastroenterolog'),
    ProfessionModel(english: 'Pulmonologist', uzbek: 'Pulmonolog'),
    ProfessionModel(english: 'Nephrologist', uzbek: 'Nefrolog'),
    ProfessionModel(english: 'Hematologist', uzbek: 'Gematolog'),
    ProfessionModel(english: 'Rheumatologist', uzbek: 'Revmatolog'),
    ProfessionModel(english: 'Allergist / Immunologist', uzbek: 'Allergolog / Immunolog'),
    ProfessionModel(english: 'Infectious Disease Specialist', uzbek: 'Infeksionist'),
    ProfessionModel(english: 'Oncologist', uzbek: 'Onkolog'),
    ProfessionModel(english: 'Neurologist', uzbek: 'Nevrolog'),
    ProfessionModel(english: 'Psychiatrist', uzbek: 'Psixiatr'),
    ProfessionModel(english: 'Dermatologist', uzbek: 'Dermatolog'),
    ProfessionModel(english: 'Venereologist', uzbek: 'Venerolog'),
    ProfessionModel(english: 'Phthisiatrician (TB specialist)', uzbek: 'Ftiziatr'),
    ProfessionModel(english: 'Toxicologist', uzbek: 'Toksikolog'),
    ProfessionModel(english: 'Sports Medicine Doctor', uzbek: 'Sport shifokori'),
    ProfessionModel(english: 'Occupational Medicine Specialist', uzbek: 'Mehnat tibbiyoti shifokori'),

    // 🧠 Mental Health & Behavioral
    ProfessionModel(english: 'Psychotherapist', uzbek: 'Psixoterapevt'),
    ProfessionModel(english: 'Clinical Psychologist', uzbek: 'Klinik psixolog'),
    ProfessionModel(english: 'Child Psychiatrist', uzbek: 'Bolalar psixiatri'),

    // 🩻 Diagnostic & Laboratory
    ProfessionModel(english: 'Radiologist', uzbek: 'Radiolog'),
    ProfessionModel(english: 'Ultrasound Doctor (Sonographer)', uzbek: 'UZI shifokori'),
    ProfessionModel(english: 'Pathologist', uzbek: 'Patolog'),
    ProfessionModel(english: 'Laboratory Doctor', uzbek: 'Laboratoriya shifokori'),
    ProfessionModel(english: 'Nuclear Medicine Specialist', uzbek: 'Yadro tibbiyoti shifokori'),
    ProfessionModel(english: 'Functional Diagnostics Doctor', uzbek: 'Funksional diagnostika shifokori'),

    // 🔪 Surgical Specialties
    ProfessionModel(english: 'General Surgeon', uzbek: 'Umumiy jarroh'),
    ProfessionModel(english: 'Cardiothoracic Surgeon', uzbek: 'Yurak-ko\'krak jarrohi'),
    ProfessionModel(english: 'Neurosurgeon', uzbek: 'Neyroxirurg'),
    ProfessionModel(english: 'Orthopedic Surgeon', uzbek: 'Ortoped-jarroh'),
    ProfessionModel(english: 'Trauma Surgeon', uzbek: 'Travmatolog'),
    ProfessionModel(english: 'Plastic Surgeon', uzbek: 'Plastik jarroh'),
    ProfessionModel(english: 'Vascular Surgeon', uzbek: 'Qon tomir jarrohi'),
    ProfessionModel(english: 'Pediatric Surgeon', uzbek: 'Bolalar jarrohi'),
    ProfessionModel(english: 'Oncologic Surgeon', uzbek: 'Onkojarroh'),
    ProfessionModel(english: 'Maxillofacial Surgeon', uzbek: 'Yuz-jag\' jarrohi'),
    ProfessionModel(english: 'Transplant Surgeon', uzbek: 'Transplantolog'),

    // 👶 Women's & Reproductive Health
    ProfessionModel(english: 'Gynecologist', uzbek: 'Ginekolog'),
    ProfessionModel(english: 'Obstetrician', uzbek: 'Akusher'),
    ProfessionModel(english: 'Obstetrician-Gynecologist (OB-GYN)', uzbek: 'Akusher-ginekolog'),
    ProfessionModel(english: 'Reproductive Medicine Specialist', uzbek: 'Reproduktolog'),
    ProfessionModel(english: 'Mammologist', uzbek: 'Mammolog'),

    // 👁️👂 ENT & Senses
    ProfessionModel(english: 'Ophthalmologist', uzbek: 'Oftalmolog (ko\'z shifokori)'),
    ProfessionModel(english: 'Otolaryngologist (ENT)', uzbek: 'LOR shifokori'),
    ProfessionModel(english: 'Audiologist', uzbek: 'Audiolog'),
    ProfessionModel(english: 'Phoniatrician', uzbek: 'Foniatr'),

    // 🦷 Dental Specialties
    ProfessionModel(english: 'Dentist', uzbek: 'Stomatolog'),
    ProfessionModel(english: 'Orthodontist', uzbek: 'Ortodont'),
    ProfessionModel(english: 'Oral Surgeon', uzbek: 'Og\'iz bo\'shlig\'i jarrohi'),
    ProfessionModel(english: 'Periodontist', uzbek: 'Parodontolog'),
    ProfessionModel(english: 'Prosthodontist', uzbek: 'Ortopedik stomatolog'),
    ProfessionModel(english: 'Pediatric Dentist', uzbek: 'Bolalar stomatologi'),

    // 🧒 Children & Development
    ProfessionModel(english: 'Neonatologist', uzbek: 'Neonatolog'),
    ProfessionModel(english: 'Pediatric Neurologist', uzbek: 'Bolalar nevrologi'),
    ProfessionModel(english: 'Pediatric Cardiologist', uzbek: 'Bolalar kardiologi'),
    ProfessionModel(english: 'Developmental Pediatrician', uzbek: 'Rivojlanish pediatri'),
    ProfessionModel(english: 'Speech Therapist', uzbek: 'Logoped'),

    // 🚑 Emergency & Intensive Care
    ProfessionModel(english: 'Emergency Physician', uzbek: 'Shoshilinch tibbiyot shifokori'),
    ProfessionModel(english: 'Intensivist (ICU Doctor)', uzbek: 'Reanimatolog'),
    ProfessionModel(english: 'Anesthesiologist', uzbek: 'Anesteziolog'),
    ProfessionModel(english: 'Anesthesiologist-Resuscitator', uzbek: 'Anesteziolog-reanimatolog'),

    // 🧬 Other Important Specialties
    ProfessionModel(english: 'Geneticist', uzbek: 'Genetik'),
    ProfessionModel(english: 'Epidemiologist', uzbek: 'Epidemiolog'),
    ProfessionModel(english: 'Public Health Specialist', uzbek: 'Jamoat salomatligi mutaxassisi'),
    ProfessionModel(english: 'Rehabilitation Doctor', uzbek: 'Reabilitolog'),
    ProfessionModel(english: 'Physiotherapist', uzbek: 'Fizioterapevt'),
    ProfessionModel(english: 'Manual Therapist', uzbek: 'Manual terapevt'),
    ProfessionModel(english: 'Nutritionist / Dietitian', uzbek: 'Dietolog'),
    ProfessionModel(english: 'Palliative Care Specialist', uzbek: 'Palliativ yordam shifokori'),
    ProfessionModel(english: 'Sleep Medicine Specialist', uzbek: 'Uyqu bo\'yicha mutaxassis'),
  ];

  /// Known compound profession strings stored in the backend (not exact catalog entries).
  static const Map<String, String> compoundAliases = {
    'Dentist, Pediatric': 'Pediatric Dentist',
    'Dentist, Prosthodontist': 'Prosthodontist',
    'Oral Surgeon, Dentist': 'Oral Surgeon',
    'Dentist, Orthodontist': 'Orthodontist',
    'Dentist, Periodontist': 'Periodontist',
    'Dentist, Oral Surgeon': 'Oral Surgeon',
  };

  /// Sub-specialty fragments that appear after a comma in compound values.
  static const Map<String, String> partAliases = {
    'Pediatric': 'Pediatric Dentist',
    'Prosthodontist': 'Prosthodontist',
    'Orthodontist': 'Orthodontist',
    'Periodontist': 'Periodontist',
    'Oral Surgeon': 'Oral Surgeon',
  };

  // Find profession by English name (for backward compatibility)
  static ProfessionModel? findByEnglish(String english) {
    final trimmed = english.trim();
    if (trimmed.isEmpty) return null;
    try {
      return allProfessions.firstWhere((p) => p.english == trimmed);
    } catch (_) {
      return null;
    }
  }

  static ProfessionModel? findByEnglishFlexible(String english) {
    final trimmed = english.trim();
    if (trimmed.isEmpty) return null;

    final exact = findByEnglish(trimmed);
    if (exact != null) return exact;

    final alias = compoundAliases[trimmed];
    if (alias != null) {
      final fromAlias = findByEnglish(alias);
      if (fromAlias != null) return fromAlias;
    }

    final lower = trimmed.toLowerCase();
    for (final p in allProfessions) {
      if (p.english.toLowerCase() == lower) return p;
    }
    return null;
  }

  /// Translates a backend profession string (English) into the active app language.
  static String translate(String english, String? languageCode) {
    final trimmed = english.trim();
    if (trimmed.isEmpty) return trimmed;

    final direct = findByEnglishFlexible(trimmed);
    if (direct != null) {
      return direct.getDisplayText(languageCode);
    }

    if (trimmed.contains(',')) {
      final parts = trimmed
          .split(',')
          .map((p) => p.trim())
          .where((p) => p.isNotEmpty)
          .toList();
      if (parts.isNotEmpty) {
        final labels = <String>[];
        final seen = <String>{};
        for (final part in parts) {
          final label = _translatePart(part, languageCode);
          if (seen.add(label)) labels.add(label);
        }
        if (labels.isNotEmpty) return labels.join(', ');
      }
    }

    return trimmed;
  }

  static String _translatePart(String part, String? languageCode) {
    final fromPartAlias = partAliases[part];
    if (fromPartAlias != null) {
      final model = findByEnglish(fromPartAlias);
      if (model != null) return model.getDisplayText(languageCode);
    }

    final flexible = findByEnglishFlexible(part);
    if (flexible != null) return flexible.getDisplayText(languageCode);

    return part;
  }

  /// Sort keys (English backend values) by localized label for filter UIs.
  static List<String> sortEnglishKeysForDisplay(
    Iterable<String> englishKeys,
    String? languageCode,
  ) {
    final list = englishKeys.toList();
    list.sort((a, b) {
      final la = translate(a, languageCode).toLowerCase();
      final lb = translate(b, languageCode).toLowerCase();
      return la.compareTo(lb);
    });
    return list;
  }

  // Find profession by any matching text (English or Uzbek)
  static List<ProfessionModel> search(String query) {
    if (query.isEmpty) return allProfessions;
    final lowerQuery = query.toLowerCase();
    return allProfessions.where((p) => p.matches(lowerQuery)).toList();
  }
}
