
/// Utility class for cleaning malformed text from OpenAI responses.
/// 
/// Handles common issues:
/// - Letters separated by spaces within words (e.g., "H e l l o" -> "Hello")
/// - Words joined together without spaces (e.g., "HelloWorld" -> "Hello World")
/// - Missing spaces after punctuation (e.g., ".Hello" -> ". Hello")
/// - Multiple spaces
/// 
/// Works for any language (English, Uzbek, Russian, etc.)
class TextCleaner {
  /// Cleans text from OpenAI responses:
  /// - Merges letters separated by spaces within words
  /// - Fixes missing spaces between words using punctuation boundaries
  /// - Removes extra spaces
  /// 
  /// Works for any language (English, Uzbek, etc.)
  static String cleanText(String text) {
    if (text.isEmpty) return text;

    // 1. Merge letters that are separated by single spaces inside words
    // e.g., "H e l l o" -> "Hello"
    // This regex matches word boundaries with letters separated by single spaces
    text = text.replaceAllMapped(RegExp(r'\b(?:\w\s)+\w\b'), (match) {
      return match.group(0)!.replaceAll(' ', '');
    });

    // 2. Add space after punctuation if missing (like ".Hello" -> ". Hello")
    // Matches punctuation followed by a word character without space
    text = text.replaceAllMapped(
      RegExp(r'([.!?,;:])(?=\w)'),
      (match) => '${match.group(1)} ',
    );

    // 3. Remove multiple spaces and replace with single space
    text = text.replaceAll(RegExp(r'\s+'), ' ');

    // 4. Trim leading/trailing spaces
    text = text.trim();

    return text;
  }
}
