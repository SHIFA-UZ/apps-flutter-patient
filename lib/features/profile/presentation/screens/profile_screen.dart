import 'package:flutter/material.dart';
import 'package:shifa_patient_app_v1/core/utils/app_logger.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';
import 'package:image_picker/image_picker.dart';
import 'package:shifa_patient_app_v1/app/router.dart';
import 'package:shifa_patient_app_v1/core/localization/app_localizations.dart';
import 'package:shifa_patient_app_v1/core/localization/error_localizations.dart';
import 'package:shifa_patient_app_v1/core/network/api_providers.dart';
import 'package:shifa_patient_app_v1/core/providers/language_provider.dart';
import 'package:shifa_patient_app_v1/core/utils/image_utils.dart';
import 'package:shifa_patient_app_v1/core/widgets/shifa_button.dart';
import 'package:shifa_patient_app_v1/core/widgets/app_version_footer.dart';
import 'package:shifa_patient_app_v1/features/auth/providers/auth_provider.dart';
import 'package:shifa_patient_app_v1/features/profile/data/account_deletion_repository.dart';
import 'package:shifa_patient_app_v1/features/profile/providers/profile_provider.dart';
import 'package:dio/dio.dart';

class ProfileScreen extends ConsumerStatefulWidget {
  const ProfileScreen({super.key});

  @override
  ConsumerState<ProfileScreen> createState() => _ProfileScreenState();
}

class _ProfileScreenState extends ConsumerState<ProfileScreen> {
  @override
  void initState() {
    super.initState();
    // Load profile data on init
    WidgetsBinding.instance.addPostFrameCallback((_) {
      ref.read(profileProvider.notifier).loadProfile();
    });
  }

  @override
  Widget build(BuildContext context) {
    final l10n = AppLocalizations.of(context)!;
    final profileState = ref.watch(profileProvider);
    final profile = profileState.profile;

    // Sync language from profile when it's loaded
    ref.listen<ProfileState>(profileProvider, (previous, next) {
      if (next.profile?.language != null &&
          next.profile!.language!.isNotEmpty &&
          previous?.profile?.language != next.profile?.language) {
        try {
          final language = AppLanguage.fromCode(next.profile!.language!);
          ref.read(languageProvider.notifier).setLanguage(language);
        } catch (e) {
          // Invalid language code, ignore
        }
      }
    });

    final displayPhotoUrl = withCacheBuster(
      profile?.photoUrl,
      profileState.photoCacheKey,
    );

    return Scaffold(
      appBar: AppBar(
        leading: IconButton(
          icon: const Icon(Icons.arrow_back),
          onPressed: () => context.pop(),
        ),
        title: Text(l10n.profile),
      ),
      body: profileState.isLoading
          ? const Center(child: CircularProgressIndicator())
          : profileState.error != null
          ? Center(
              child: Column(
                mainAxisAlignment: MainAxisAlignment.center,
                children: [
                  const Icon(Icons.error_outline, size: 48, color: Colors.red),
                  const SizedBox(height: 16),
                  Text('${l10n.error}: ${profileState.error}'),
                  const SizedBox(height: 16),
                  ShifaPrimaryButton(
                    label: l10n.retry,
                    onPressed: () {
                      ref.read(profileProvider.notifier).loadProfile();
                    },
                    width: ButtonWidth.hug,
                  ),
                ],
              ),
            )
          : RefreshIndicator(
              onRefresh: () async {
                await ref.read(profileProvider.notifier).loadProfile();
              },
              child: SingleChildScrollView(
                padding: const EdgeInsets.all(16.0),
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.stretch,
                  children: [
                    // Profile card: avatar + name, phone, email, address
                    _buildProfileCard(context, profile, l10n, displayPhotoUrl),
                    const SizedBox(height: 16),
                    // Edit Profile button
                    ShifaSecondaryButton(
                      label: l10n.editProfile,
                      onPressed: () => context.push(AppRoutes.editProfile),
                    ),
                    const SizedBox(height: 16),
                    // Settings section
                    Text(
                      l10n.settingsTitle,
                      style: Theme.of(context).textTheme.titleMedium?.copyWith(
                        fontWeight: FontWeight.bold,
                        color: Colors.black87,
                      ),
                    ),
                    const SizedBox(height: 8),
                    Material(
                      color: Colors.white,
                      borderRadius: BorderRadius.circular(12),
                      child: Column(
                        children: [
                          _buildSettingsTile(
                            context,
                            l10n.language,
                            Icons.language_rounded,
                            subtitle: ref
                                .watch(languageProvider)
                                .language
                                .displayName,
                            onTap: () =>
                                _showLanguageSelectionDialog(context, l10n),
                          ),
                          _divider(),
                          _buildSettingsTile(
                            context,
                            l10n.appLock ?? 'App Lock',
                            Icons.lock_outline_rounded,
                            onTap: () =>
                                context.push(AppRoutes.appLockSettings),
                          ),
                          _divider(),
                          _buildSettingsTile(
                            context,
                            l10n.changePassword,
                            Icons.lock_reset_rounded,
                            onTap: () => context.push(AppRoutes.changePassword),
                          ),
                          _divider(),
                          _buildSettingsTile(
                            context,
                            l10n.privacy,
                            Icons.policy_rounded,
                            onTap: () {},
                          ),
                        ],
                      ),
                    ),
                    const SizedBox(height: 16),
                    // Account section
                    Text(
                      l10n.accountTitle,
                      style: Theme.of(context).textTheme.titleMedium?.copyWith(
                        fontWeight: FontWeight.bold,
                        color: Colors.black87,
                      ),
                    ),
                    const SizedBox(height: 8),
                    Material(
                      color: Colors.white,
                      borderRadius: BorderRadius.circular(12),
                      child: Column(
                        children: [
                          _buildAccountDangerTile(
                            context,
                            l10n.deleteAccount,
                            Icons.delete_outline_rounded,
                            onTap: () => _showDeleteAccountDialog(context),
                          ),
                          _divider(),
                          _buildAccountDangerTile(
                            context,
                            l10n.logOut,
                            Icons.logout_rounded,
                            onTap: () => _showLogOutDialog(context, l10n),
                          ),
                        ],
                      ),
                    ),
                    const AppVersionFooter(
                      padding: EdgeInsets.fromLTRB(8, 32, 8, 16),
                    ),
                  ],
                ),
              ),
            ),
    );
  }

  Widget _buildProfileCard(
    BuildContext context,
    dynamic profile,
    AppLocalizations l10n,
    String? displayPhotoUrl,
  ) {
    return Card(
      elevation: 0,
      shape: RoundedRectangleBorder(
        borderRadius: BorderRadius.circular(12),
        side: BorderSide(color: Colors.grey.shade200),
      ),
      child: Padding(
        padding: const EdgeInsets.all(16.0),
        child: Row(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Stack(
              children: [
                CircleAvatar(
                  key: ValueKey(profile?.photoUrl ?? 'no-photo'),
                  radius: 36,
                  backgroundImage: displayPhotoUrl != null
                      ? NetworkImage(displayPhotoUrl)
                      : null,
                  child:
                      profile?.photoUrl == null ||
                          (profile?.photoUrl ?? '').isEmpty
                      ? const Icon(
                          Icons.person_rounded,
                          size: 36,
                          color: Colors.white,
                        )
                      : null,
                  backgroundColor: const Color(0xFF17C3B2),
                ),
                Positioned(
                  bottom: 0,
                  right: 0,
                  child: GestureDetector(
                    onTap: _uploadPhoto,
                    child: CircleAvatar(
                      radius: 14,
                      backgroundColor: const Color(0xFF17C3B2),
                      child: const Icon(
                        Icons.camera_alt_rounded,
                        size: 16,
                        color: Colors.white,
                      ),
                    ),
                  ),
                ),
              ],
            ),
            const SizedBox(width: 16),
            Expanded(
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Text(
                    profile?.fullName ?? l10n.translate('noName'),
                    style: const TextStyle(
                      fontSize: 18,
                      fontWeight: FontWeight.bold,
                      color: Colors.black87,
                    ),
                  ),
                  if (profile?.phone != null && profile!.phone!.isNotEmpty) ...[
                    const SizedBox(height: 4),
                    Text(
                      profile!.phone!,
                      style: TextStyle(
                        fontSize: 14,
                        color: Colors.grey.shade700,
                      ),
                    ),
                  ],
                  if (profile?.email != null && profile.email!.isNotEmpty) ...[
                    const SizedBox(height: 2),
                    Text(
                      profile.email!,
                      style: TextStyle(
                        fontSize: 14,
                        color: Colors.grey.shade700,
                      ),
                    ),
                  ],
                  if (profile?.address != null &&
                      profile.address!.isNotEmpty) ...[
                    const SizedBox(height: 2),
                    Text(
                      profile.address!,
                      style: TextStyle(
                        fontSize: 14,
                        color: Colors.grey.shade700,
                      ),
                    ),
                  ],
                ],
              ),
            ),
          ],
        ),
      ),
    );
  }

  Widget _divider() {
    return Divider(
      height: 1,
      indent: 56,
      endIndent: 16,
      color: Colors.grey.shade200,
    );
  }

  Widget _buildSettingsTile(
    BuildContext context,
    String title,
    IconData icon, {
    String? subtitle,
    required VoidCallback onTap,
  }) {
    return ListTile(
      contentPadding: const EdgeInsets.symmetric(horizontal: 16, vertical: 0),
      minVerticalPadding: 0,
      visualDensity: VisualDensity.compact,
      leading: Icon(icon, size: 24, color: Colors.grey.shade700),
      title: Text(
        title,
        style: const TextStyle(
          fontSize: 16,
          fontWeight: FontWeight.w500,
          color: Colors.black87,
        ),
      ),
      subtitle: subtitle != null
          ? Text(
              subtitle,
              style: TextStyle(fontSize: 14, color: Colors.grey.shade600),
            )
          : null,
      trailing: const Icon(Icons.chevron_right_rounded, color: Colors.grey),
      onTap: onTap,
    );
  }

  Widget _buildAccountDangerTile(
    BuildContext context,
    String title,
    IconData icon, {
    required VoidCallback onTap,
  }) {
    return ListTile(
      contentPadding: const EdgeInsets.symmetric(horizontal: 16, vertical: 0),
      minVerticalPadding: 0,
      visualDensity: VisualDensity.compact,
      leading: Icon(icon, size: 24, color: Colors.red),
      title: Text(
        title,
        style: const TextStyle(
          fontSize: 16,
          fontWeight: FontWeight.w500,
          color: Colors.red,
        ),
      ),
      trailing: const Icon(
        Icons.chevron_right_rounded,
        color: Colors.red,
        size: 22,
      ),
      onTap: onTap,
    );
  }

  void _showLogOutDialog(BuildContext context, AppLocalizations l10n) {
    showDialog(
      context: context,
      builder: (BuildContext dialogContext) {
        return AlertDialog(
          title: Text(l10n.logOut),
          content: Text(l10n.signOutConfirm),
          actions: [
            TextButton(
              onPressed: () => Navigator.of(dialogContext).pop(),
              child: Text(l10n.cancel),
            ),
            TextButton(
              onPressed: () {
                Navigator.of(dialogContext).pop();
                ref.read(authStateProvider.notifier).logout();
                context.go(AppRoutes.login);
              },
              style: TextButton.styleFrom(foregroundColor: Colors.red),
              child: Text(l10n.logOut),
            ),
          ],
        );
      },
    );
  }

  void _showLanguageSelectionDialog(
    BuildContext context,
    AppLocalizations l10n,
  ) {
    final languageState = ref.watch(languageProvider);

    showDialog(
      context: context,
      builder: (dialogContext) {
        final dialogL10n = AppLocalizations.of(dialogContext)!;
        return AlertDialog(
          title: Text(dialogL10n.selectLanguage),
          content: Column(
            mainAxisSize: MainAxisSize.min,
            children: AppLanguage.values.map((language) {
              return RadioListTile<AppLanguage>(
                title: Text(language.displayName),
                value: language,
                groupValue: languageState.language,
                onChanged: (value) async {
                  if (value != null) {
                    await ref
                        .read(languageProvider.notifier)
                        .setLanguage(value);

                    // Update backend profile with new language
                    try {
                      await ref
                          .read(profileProvider.notifier)
                          .updateProfile(language: value.code);
                    } catch (e) {
                      AppLogger.error(
                        'Failed to update language in backend:',
                        e,
                      );
                    }

                    if (context.mounted) {
                      Navigator.of(context).pop();
                      // Show success message
                      final updatedL10n = AppLocalizations.of(context)!;
                      ScaffoldMessenger.of(context).showSnackBar(
                        SnackBar(
                          content: Text(
                            updatedL10n.languageChanged(value.displayName),
                          ),
                          backgroundColor: Colors.green,
                          duration: const Duration(seconds: 2),
                        ),
                      );
                    }
                  }
                },
              );
            }).toList(),
          ),
          actions: [
            TextButton(
              onPressed: () => Navigator.of(dialogContext).pop(),
              child: Text(dialogL10n.cancel),
            ),
          ],
        );
      },
    );
  }

  Future<void> _uploadPhoto() async {
    final picker = ImagePicker();
    final pickedFile = await picker.pickImage(
      source: ImageSource.gallery,
      imageQuality: 85,
    );

    if (pickedFile == null) return;

    try {
      // Show loading indicator
      if (mounted) {
        final l10n = AppLocalizations.of(context)!;
        ScaffoldMessenger.of(context).showSnackBar(
          SnackBar(
            content: Row(
              children: [
                const SizedBox(
                  width: 20,
                  height: 20,
                  child: CircularProgressIndicator(strokeWidth: 2),
                ),
                const SizedBox(width: 16),
                Text(l10n.uploading ?? 'Uploading photo...'),
              ],
            ),
            duration: const Duration(seconds: 30),
          ),
        );
      }

      final apiClient = ref.read(apiClientProvider);

      // Read file as bytes (works on all platforms)
      final bytes = await pickedFile.readAsBytes();

      // Upload file using multipart form data
      final formData = FormData.fromMap({
        'file': MultipartFile.fromBytes(bytes, filename: pickedFile.name),
      });

      // Try to upload to patient photo endpoint (if it exists)
      // Otherwise, we'll update the profile with a local path and let backend handle it
      String? photoUrl;

      try {
        // Try patient-specific photo upload endpoint first
        final response = await apiClient.dio.post(
          '/patients/me/photo',
          data: formData,
        );
        AppLogger.debug('Upload response status: ${response.statusCode}');
        AppLogger.debug('Upload response data: ${response.data}');

        if (response.statusCode == 200 && response.data != null) {
          // Handle both Map and direct string response
          if (response.data is Map) {
            photoUrl = response.data['photoUrl'] as String?;
          } else if (response.data is String) {
            photoUrl = response.data as String;
          }
          AppLogger.debug('Extracted photoUrl: $photoUrl');
        }
      } catch (e) {
        AppLogger.error('Patient photo upload error:', e);
        // If patient endpoint doesn't exist, try generic profile photo endpoint
        try {
          final response = await apiClient.dio.post(
            '/profile/photo',
            data: formData,
          );
          if (response.statusCode == 200 && response.data != null) {
            if (response.data is Map) {
              photoUrl = response.data['photoUrl'] as String?;
            } else if (response.data is String) {
              photoUrl = response.data as String;
            }
          }
        } catch (e2) {
          AppLogger.error('Profile photo upload error:', e2);
          // If no upload endpoint exists, we'll need to use a workaround
          // For now, show error and suggest using edit profile
          if (mounted) {
            final l10n = AppLocalizations.of(context)!;
            ScaffoldMessenger.of(context).hideCurrentSnackBar();
            ScaffoldMessenger.of(context).showSnackBar(
              SnackBar(
                content: Text(
                  l10n.photoUploadEndpointNotAvailable ??
                      'Photo upload endpoint not available. Please use Edit Profile to update photo.',
                ),
                backgroundColor: Colors.orange,
              ),
            );
          }
          return;
        }
      }

      // Update profile with new photo URL
      if (photoUrl != null && photoUrl.isNotEmpty) {
        AppLogger.debug('Updating profile with photoUrl: $photoUrl');
        await ref
            .read(profileProvider.notifier)
            .updateProfile(photoUrl: photoUrl);

        // Reload profile to ensure we have the latest data
        await ref.read(profileProvider.notifier).loadProfile();

        if (mounted) {
          final l10n = AppLocalizations.of(context)!;
          ScaffoldMessenger.of(context).hideCurrentSnackBar();
          ScaffoldMessenger.of(context).showSnackBar(
            SnackBar(
              content: Text(
                l10n.profilePhotoUpdatedSuccessfully ??
                    'Profile photo updated successfully',
              ),
              backgroundColor: Colors.green,
            ),
          );
          // Force rebuild to show new image
          setState(() {});
        }
      } else {
        AppLogger.debug('Photo URL is null or empty');
        if (mounted) {
          final l10n = AppLocalizations.of(context)!;
          ScaffoldMessenger.of(context).hideCurrentSnackBar();
          ScaffoldMessenger.of(context).showSnackBar(
            SnackBar(
              content: Text(
                l10n.failedToGetPhotoUrl ??
                    'Failed to get photo URL from server',
              ),
              backgroundColor: Colors.orange,
            ),
          );
        }
      }
    } catch (e) {
      if (mounted) {
        final l10n = AppLocalizations.of(context)!;
        ScaffoldMessenger.of(context).hideCurrentSnackBar();
        ScaffoldMessenger.of(context).showSnackBar(
          SnackBar(
            content: Text(
              '${l10n.failedToUploadPhoto ?? 'Failed to upload photo'}: ${userFriendlyError(l10n, e, logContext: 'Upload photo')}',
            ),
            backgroundColor: Colors.red,
          ),
        );
      }
    }
  }

  void _showDeleteAccountDialog(BuildContext context) {
    final rootContext = context;
    final l10n = AppLocalizations.of(rootContext)!;
    showDialog(
      context: rootContext,
      builder: (dialogContext) => AlertDialog(
        title: Text(l10n.deleteAccount),
        content: Text(
          l10n.translate('deleteAccountWarning'),
        ),
        actions: [
          TextButton(
            onPressed: () => Navigator.of(dialogContext).pop(),
            child: Text(l10n.cancel),
          ),
          TextButton(
            onPressed: () async {
              Navigator.of(dialogContext).pop();

              final email = ref.read(profileProvider).profile?.email?.trim();
              if (email == null || email.isEmpty) {
                ScaffoldMessenger.of(rootContext).showSnackBar(
                  SnackBar(
                    content: Text(l10n.translate('emailRequired')),
                  ),
                );
                return;
              }

              // Show blocking progress while requesting deletion challenge.
              // Use the root navigator so we can reliably dismiss it even inside nested shells.
              showDialog(
                context: rootContext,
                useRootNavigator: true,
                barrierDismissible: false,
                builder: (_) =>
                    const Center(child: CircularProgressIndicator()),
              );

              try {
                final repo = ref.read(accountDeletionRepositoryProvider);
                final res = await repo.requestDeletion().timeout(
                  const Duration(seconds: 10),
                  onTimeout: () => throw Exception(
                    l10n.translate('requestTimeout'),
                  ),
                );

                if (rootContext.mounted) {
                  Navigator.of(rootContext, rootNavigator: true).pop(); // close progress
                }
                if (!rootContext.mounted) return;

                rootContext.push(
                  AppRoutes.deleteAccountVerify,
                  extra: {'challengeId': res.challengeId, 'email': email},
                );
              } catch (e) {
                if (rootContext.mounted) {
                  Navigator.of(rootContext, rootNavigator: true).pop(); // close progress
                }
                if (!rootContext.mounted) return;
                ScaffoldMessenger.of(rootContext).showSnackBar(
                  SnackBar(
                    content: Text(
                      userFriendlyError(
                        l10n,
                        e,
                        logContext: 'Delete account request',
                      ),
                    ),
                  ),
                );
              }
            },
            child: Text(l10n.delete, style: const TextStyle(color: Colors.red)),
          ),
        ],
      ),
    );
  }
}
