import 'package:flutter/material.dart';

/// Stub for non-web platforms (mobile uses CallClient).
class DailyVideoEmbedWeb extends StatelessWidget {
  const DailyVideoEmbedWeb({
    super.key,
    required this.roomUrl,
    required this.token,
  });

  final String roomUrl;
  final String token;

  @override
  Widget build(BuildContext context) {
    return const SizedBox.shrink();
  }
}
