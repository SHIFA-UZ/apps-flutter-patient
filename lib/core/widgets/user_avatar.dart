import 'package:flutter/material.dart';
import 'package:shifa_patient_app_v1/core/utils/image_utils.dart';

class UserAvatar extends StatelessWidget {
  final String? name;
  final String? photoUrl;
  final double radius;

  const UserAvatar({
    super.key,
    required this.name,
    required this.photoUrl,
    this.radius = 20,
  });

  String? _initials(String? raw) {
    if (raw == null) return null;
    final parts = raw.trim().split(RegExp(r'\s+'));
    if (parts.isEmpty || parts.first.isEmpty) return null;
    if (parts.length == 1) return parts.first.substring(0, 1).toUpperCase();
    return '${parts.first[0]}${parts.last[0]}'.toUpperCase();
  }

  @override
  Widget build(BuildContext context) {
    // Normalize URL to ensure localhost is replaced with 10.0.2.2 on Android
    final normalizedUrl = normalizePhotoUrl(photoUrl);
    final initials = _initials(name);
    final hasPhoto = normalizedUrl != null && normalizedUrl.isNotEmpty;
    final diameter = radius * 2;
    final urlToUse = normalizedUrl ?? '';

    return SizedBox(
      width: diameter,
      height: diameter,
      child: ClipOval(
        child: hasPhoto
            ? Image.network(
                urlToUse,
                fit: BoxFit.cover,
                cacheWidth: diameter.toInt(),
                cacheHeight: diameter.toInt(),
                loadingBuilder: (context, child, loadingProgress) {
                  if (loadingProgress == null) return child;
                  return _fallback(initials);
                },
                errorBuilder: (_, __, ___) => _fallback(initials),
              )
            : _fallback(initials),
      ),
    );
  }

  Widget _fallback(String? initials) {
    return Container(
      color: const Color(0xFFE0E0E0),
      alignment: Alignment.center,
      child: initials != null
          ? Text(
              initials,
              style: TextStyle(
                fontSize: radius * 0.75,
                fontWeight: FontWeight.w600,
                color: const Color(0xFF607D8B),
              ),
            )
          : Icon(
              Icons.person,
              size: radius,
              color: const Color(0xFF607D8B),
            ),
    );
  }
}
