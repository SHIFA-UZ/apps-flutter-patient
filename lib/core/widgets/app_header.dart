import 'package:flutter/material.dart';
import 'package:shifa_patient_app_v1/core/constants/assets.dart';
import 'package:shifa_patient_app_v1/core/theme/app_design_system.dart';
import 'package:shifa_patient_app_v1/core/widgets/chat_icon_button.dart';
import 'package:shifa_patient_app_v1/core/widgets/notification_icon_button.dart';

/// Standard app header: 140px height, 24px bottom radius, 16px padding.
/// Use for Home, Bookings, Documents with consistent layout.
class AppHeader extends StatelessWidget {
  final String title;
  final String? subtitle;
  final bool showProfile;
  final bool showNotification;
  final bool showBack;
  final VoidCallback? onLogoTap;
  final VoidCallback? onProfileTap;
  final String? profilePhotoUrl;
  final VoidCallback? onNotificationTap;
  final VoidCallback? onChatTap;
  final VoidCallback? onBackTap;

  const AppHeader({
    super.key,
    required this.title,
    this.subtitle,
    this.showProfile = true,
    this.showNotification = true,
    this.showBack = false,
    this.onLogoTap,
    this.onProfileTap,
    this.profilePhotoUrl,
    this.onNotificationTap,
    this.onChatTap,
    this.onBackTap,
  });

  @override
  Widget build(BuildContext context) {
    // Get SafeArea top inset to adjust container height
    final topPadding = MediaQuery.of(context).padding.top;

    return Container(
      height: AppDesignSystem.headerHeight + topPadding,
      decoration: const BoxDecoration(
        color: AppDesignSystem.primary,
        borderRadius: BorderRadius.only(
          bottomLeft: Radius.circular(AppDesignSystem.headerRadius),
          bottomRight: Radius.circular(AppDesignSystem.headerRadius),
        ),
      ),
      child: SafeArea(
        bottom: false,
        child: Padding(
          padding: const EdgeInsets.fromLTRB(
            AppDesignSystem.headerPadding,
            8,
            AppDesignSystem.headerPadding,
            AppDesignSystem.headerPadding,
          ),
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            mainAxisAlignment: MainAxisAlignment.spaceBetween,
            children: [
              Row(
                mainAxisAlignment: MainAxisAlignment.spaceBetween,
                children: [
                  if (showBack)
                    IconButton(
                      onPressed: onBackTap ?? () => Navigator.of(context).maybePop(),
                      icon: const Icon(Icons.arrow_back_ios_new, color: Colors.white, size: 22),
                      padding: EdgeInsets.zero,
                      constraints: const BoxConstraints(minWidth: 40, minHeight: 40),
                    )
                  else
                    GestureDetector(
                      onTap: onLogoTap,
                      child: ClipRRect(
                        borderRadius: BorderRadius.circular(6),
                        child: Image.asset(Assets.shifaLogoPng, width: 28, height: 28),
                      ),
                    ),
                  Row(
                    children: [
                      if (showNotification)
                        NotificationIconButton(onPressed: onNotificationTap ?? () {}),
                      const SizedBox(width: 4),
                      ChatIconButton(onPressed: onChatTap ?? () {}),
                      if (showProfile) ...[
                        const SizedBox(width: 4),
                        IconButton(
                          onPressed: onProfileTap,
                          icon: CircleAvatar(
                            radius: 16,
                            backgroundImage: profilePhotoUrl != null && profilePhotoUrl!.isNotEmpty
                                ? NetworkImage(profilePhotoUrl!)
                                : null,
                            backgroundColor: Colors.white.withOpacity(0.2),
                            child: profilePhotoUrl == null || profilePhotoUrl!.isEmpty
                                ? const Icon(Icons.person_outline, color: Colors.white, size: 18)
                                : null,
                          ),
                          padding: EdgeInsets.zero,
                        ),
                      ],
                    ],
                  ),
                ],
              ),
              Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                mainAxisSize: MainAxisSize.min,
                children: [
                  Text(
                    title,
                    style: const TextStyle(
                      color: Colors.white,
                      fontSize: AppDesignSystem.h1Size,
                      fontWeight: FontWeight.w600,
                      height: 1.25,
                    ),
                  ),
                  if (subtitle != null && subtitle!.isNotEmpty) ...[
                    const SizedBox(height: 2),
                    Text(
                      subtitle!,
                      style: TextStyle(
                        color: Colors.white.withOpacity(0.9),
                        fontSize: AppDesignSystem.body2Size,
                        fontWeight: FontWeight.normal,
                      ),
                    ),
                  ],
                ],
              ),
            ],
          ),
        ),
      ),
    );
  }
}
