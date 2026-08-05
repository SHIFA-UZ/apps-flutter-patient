import 'package:flutter/material.dart';
import 'package:package_info_plus/package_info_plus.dart';
import 'package:shifa_patient_app_v1/core/config/app_config.dart';

/// Bottom footer showing e.g. "Shifa Bemor 1.1.7 (3 Aug 2026)".
class AppVersionFooter extends StatefulWidget {
  const AppVersionFooter({super.key, this.padding = const EdgeInsets.only(top: 24, bottom: 8)});

  final EdgeInsetsGeometry padding;

  @override
  State<AppVersionFooter> createState() => _AppVersionFooterState();
}

class _AppVersionFooterState extends State<AppVersionFooter> {
  String? _label;

  @override
  void initState() {
    super.initState();
    _load();
  }

  Future<void> _load() async {
    try {
      final info = await PackageInfo.fromPlatform();
      final version = info.version.trim().isEmpty ? '—' : info.version.trim();
      if (!mounted) return;
      setState(() {
        _label =
            '${AppConfig.appDisplayName} $version (${AppConfig.releaseDateLabel})';
      });
    } catch (_) {
      if (!mounted) return;
      setState(() {
        _label =
            '${AppConfig.appDisplayName} (${AppConfig.releaseDateLabel})';
      });
    }
  }

  @override
  Widget build(BuildContext context) {
    final text = _label;
    if (text == null) {
      return Padding(
        padding: widget.padding,
        child: const SizedBox(height: 14),
      );
    }
    return Padding(
      padding: widget.padding,
      child: Text(
        text,
        textAlign: TextAlign.center,
        style: Theme.of(context).textTheme.bodySmall?.copyWith(
              color: Colors.grey.shade600,
              fontSize: 12,
            ),
      ),
    );
  }
}
