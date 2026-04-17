import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:shifa_patient_app_v1/core/providers/language_provider.dart';

/// Small language dropdown (EN, DE, UZ, RU) used e.g. on login screen.
/// Updates app locale immediately via [languageProvider].
class LanguageMiniToggle extends ConsumerStatefulWidget {
  const LanguageMiniToggle({super.key});

  @override
  ConsumerState<LanguageMiniToggle> createState() => _LanguageMiniToggleState();
}

class _LanguageMiniToggleState extends ConsumerState<LanguageMiniToggle> {
  final GlobalKey _buttonKey = GlobalKey();
  static const _teal = Color(0xFF17C3B2);

  @override
  Widget build(BuildContext context) {
    final languageState = ref.watch(languageProvider);
    final code = languageState.language.code.toUpperCase();

    return Material(
      type: MaterialType.transparency,
      child: InkWell(
        borderRadius: BorderRadius.circular(999),
        onTap: () async {
          final box = _buttonKey.currentContext?.findRenderObject() as RenderBox?;
          if (box == null) return;
          final position = box.localToGlobal(Offset.zero);
          final size = box.size;

          final selectedCode = await showMenu<String>(
            context: context,
            position: RelativeRect.fromLTRB(
              position.dx,
              position.dy + size.height,
              position.dx + size.width,
              position.dy + size.height + 200,
            ),
            items: AppLanguage.values
                .map((lang) => PopupMenuItem<String>(
                      value: lang.code,
                      child: Text(lang.code.toUpperCase()),
                    ))
                .toList(),
          );

          if (selectedCode != null) {
            final selected = AppLanguage.fromCode(selectedCode);
            await ref.read(languageProvider.notifier).setLanguage(selected);
          }
        },
        child: Container(
          key: _buttonKey,
          padding: const EdgeInsets.symmetric(horizontal: 10, vertical: 6),
          decoration: BoxDecoration(
            color: Colors.white,
            borderRadius: BorderRadius.circular(999),
            border: Border.all(color: Colors.black12),
            boxShadow: const [
              BoxShadow(
                color: Color(0x14000000),
                blurRadius: 8,
                offset: Offset(0, 2),
              ),
            ],
          ),
          child: Row(
            mainAxisSize: MainAxisSize.min,
            children: [
              Text(
                code,
                style: const TextStyle(
                  fontSize: 12,
                  fontWeight: FontWeight.w700,
                  color: Colors.black87,
                  letterSpacing: 0.3,
                ),
              ),
              const SizedBox(width: 6),
              const Icon(Icons.keyboard_arrow_down, size: 18, color: _teal),
            ],
          ),
        ),
      ),
    );
  }
}
