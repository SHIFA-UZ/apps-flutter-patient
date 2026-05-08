import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';
import 'package:shifa_patient_app_v1/app/router.dart';
import 'package:shifa_patient_app_v1/core/localization/app_localizations.dart';
import 'package:shifa_patient_app_v1/core/theme/app_design_system.dart';
import 'package:shifa_patient_app_v1/core/widgets/shifa_button.dart';
import 'package:shifa_patient_app_v1/core/models/notification_model.dart';
import 'package:shifa_patient_app_v1/features/notifications/providers/notifications_provider.dart';
import 'package:shifa_patient_app_v1/features/notifications/services/notification_tap_handler.dart';
import 'package:shifa_patient_app_v1/features/notifications/utils/notification_localization.dart';
import 'package:shifa_patient_app_v1/features/notifications/presentation/notification_ui_helpers.dart';

/// Notifications screen for the patient app.
/// Product-grade UI: grouped by date, semantic colors, cards, filters (adapted from doctor app).
class NotificationsScreen extends ConsumerStatefulWidget {
  const NotificationsScreen({super.key});

  @override
  ConsumerState<NotificationsScreen> createState() => _NotificationsScreenState();
}

class _NotificationsScreenState extends ConsumerState<NotificationsScreen> {
  NotificationFilter _filter = NotificationFilter.all;

  @override
  void initState() {
    super.initState();
    WidgetsBinding.instance.addPostFrameCallback((_) {
      ref.read(notificationsControllerProvider).refresh();
    });
  }

  @override
  Widget build(BuildContext context) {
    final l10n = AppLocalizations.of(context)!;
    final notificationsAsync = ref.watch(notificationsProvider);
    final unreadCountAsync = ref.watch(unreadNotificationsCountProvider);
    final controller = ref.read(notificationsControllerProvider);
    final actedRequestIds = ref.watch(documentAccessRequestActedIdsProvider);
    final unreadCount = unreadCountAsync.valueOrNull ?? 0;

    return Scaffold(
      appBar: AppBar(
        leading: IconButton(
          icon: const Icon(Icons.arrow_back),
          onPressed: () {
            if (context.canPop()) {
              context.pop();
            } else {
              context.go(AppRoutes.home);
            }
          },
          tooltip: l10n.translate('back'),
        ),
        title: LayoutBuilder(
          builder: (context, constraints) {
            final titleText = unreadCount > 0
                ? '${l10n.translate('notifications')} ($unreadCount)'
                : l10n.translate('notifications');
            return FittedBox(
              fit: BoxFit.scaleDown,
              alignment: Alignment.centerLeft,
              child: Text(
                titleText,
                style: Theme.of(context).appBarTheme.titleTextStyle?.copyWith(
                      fontSize: 20,
                      fontWeight: FontWeight.bold,
                    ),
                maxLines: 1,
                overflow: TextOverflow.ellipsis,
              ),
            );
          },
        ),
        titleSpacing: 0,
        actions: [
          IconButton(
            onPressed: () async {
              await controller.markAllAsRead();
              if (mounted) {
                ScaffoldMessenger.of(context).showSnackBar(
                  SnackBar(
                    content: Text(l10n.translate('allNotificationsMarkedAsRead')),
                  ),
                );
              }
            },
            icon: const Icon(Icons.done_all, size: 22),
            tooltip: l10n.translate('markAllAsRead'),
          ),
          IconButton(
            onPressed: () => context.push(AppRoutes.account),
            icon: const Icon(Icons.settings_outlined, size: 22),
            tooltip: l10n.translate('notificationSettings'),
          ),
        ],
      ),
      body: Column(
        crossAxisAlignment: CrossAxisAlignment.stretch,
        children: [
          // Filters: All | Appointments | Documents | Tasks
          SingleChildScrollView(
            scrollDirection: Axis.horizontal,
            padding: const EdgeInsets.symmetric(horizontal: 20, vertical: 12),
            child: Row(
              children: [
                _FilterChip(
                  label: l10n.translate('notificationFilterAll'),
                  selected: _filter == NotificationFilter.all,
                  onTap: () => setState(() => _filter = NotificationFilter.all),
                ),
                const SizedBox(width: 8),
                _FilterChip(
                  label: l10n.translate('notificationFilterAppointments'),
                  selected: _filter == NotificationFilter.appointments,
                  onTap: () => setState(() => _filter = NotificationFilter.appointments),
                ),
                const SizedBox(width: 8),
                _FilterChip(
                  label: l10n.translate('notificationFilterDocuments'),
                  selected: _filter == NotificationFilter.documents,
                  onTap: () => setState(() => _filter = NotificationFilter.documents),
                ),
                const SizedBox(width: 8),
                _FilterChip(
                  label: l10n.translate('notificationFilterTasks'),
                  selected: _filter == NotificationFilter.tasks,
                  onTap: () => setState(() => _filter = NotificationFilter.tasks),
                ),
              ],
            ),
          ),
          Expanded(
            child: notificationsAsync.when(
              data: (notifications) {
                final filtered = notifications
                    .where((n) => notificationMatchesFilter(n.type, _filter))
                    .toList();
                if (filtered.isEmpty) {
                  return _EmptyState(
                    hasFilter: _filter != NotificationFilter.all,
                    l10n: l10n,
                  );
                }
                final grouped = _groupByDate(filtered, l10n);
                return RefreshIndicator(
                  onRefresh: () async => controller.refresh(),
                  child: ListView.builder(
                    padding: EdgeInsets.fromLTRB(
                      20,
                      0,
                      20,
                      AppDesignSystem.safeBottomWithNavBar(context) + 24,
                    ),
                    itemCount: grouped.length,
                    itemBuilder: (context, index) {
                      final section = grouped[index];
                      return Padding(
                        padding: const EdgeInsets.only(bottom: 24),
                        child: Column(
                          crossAxisAlignment: CrossAxisAlignment.start,
                          children: [
                            Padding(
                              padding: const EdgeInsets.only(left: 4, bottom: 12),
                              child: Text(
                                section.label,
                                style: Theme.of(context).textTheme.titleSmall?.copyWith(
                                      color: Colors.grey.shade600,
                                      fontWeight: FontWeight.w600,
                                    ),
                              ),
                            ),
                            ...section.items.map((n) => Padding(
                                  padding: const EdgeInsets.only(bottom: 12),
                                  child: _NotificationCard(
                                    notification: n,
                                    l10n: l10n,
                                    controller: controller,
                                    actedRequestIds: actedRequestIds,
                                    onTap: () async {
                                      await NotificationTapHandler.handleTap(
                                        context: context,
                                        notification: n,
                                        markAsRead: controller.markAsRead,
                                        translate: l10n.translate,
                                      );
                                    },
                                  ),
                                )),
                          ],
                        ),
                      );
                    },
                  ),
                );
              },
              loading: () => const Center(child: CircularProgressIndicator()),
              error: (error, stack) {
                return Center(
                  child: Padding(
                    padding: const EdgeInsets.all(24),
                    child: Column(
                      mainAxisAlignment: MainAxisAlignment.center,
                      children: [
                        const Icon(Icons.error_outline, size: 32, color: Colors.redAccent),
                        const SizedBox(height: 12),
                        Text(
                          l10n.translate('errorLoadingNotifications'),
                          style: Theme.of(context).textTheme.bodyMedium,
                          textAlign: TextAlign.center,
                        ),
                        const SizedBox(height: 8),
                        Text(
                          error.toString(),
                          style: Theme.of(context)
                              .textTheme
                              .bodySmall
                              ?.copyWith(color: Colors.grey.shade600),
                          textAlign: TextAlign.center,
                        ),
                        const SizedBox(height: 16),
                        ShifaSecondaryButton(
                          label: l10n.retry,
                          onPressed: () => controller.refresh(),
                          width: ButtonWidth.hug,
                        ),
                      ],
                    ),
                  ),
                );
              },
            ),
          ),
        ],
      ),
    );
  }

  List<({String label, List<NotificationModel> items})> _groupByDate(
    List<NotificationModel> items,
    AppLocalizations l10n,
  ) {
    // Sort newest first so both section order and items within a section
    // are in descending chronological order (Today → Yesterday → older).
    final sorted = [...items]
      ..sort((a, b) => b.createdAt.compareTo(a.createdAt));
    final map = <String, List<NotificationModel>>{};
    final orderedLabels = <String>[];
    for (final n in sorted) {
      final label = dateSectionLabel(n.createdAt, l10n);
      if (!map.containsKey(label)) {
        orderedLabels.add(label);
        map[label] = [];
      }
      map[label]!.add(n);
    }
    return orderedLabels.map((label) => (label: label, items: map[label]!)).toList();
  }
}

class _FilterChip extends StatelessWidget {
  final String label;
  final bool selected;
  final VoidCallback onTap;

  const _FilterChip({
    required this.label,
    required this.selected,
    required this.onTap,
  });

  @override
  Widget build(BuildContext context) {
    return Material(
      color: selected
          ? AppDesignSystem.primary.withValues(alpha: 0.15)
          : Colors.grey.shade100,
      borderRadius: BorderRadius.circular(20),
      child: InkWell(
        onTap: onTap,
        borderRadius: BorderRadius.circular(20),
        child: Padding(
          padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 10),
          child: Text(
            label,
            style: TextStyle(
              fontSize: 13,
              fontWeight: selected ? FontWeight.w600 : FontWeight.w500,
              color: selected ? AppDesignSystem.primary : Colors.grey.shade700,
            ),
          ),
        ),
      ),
    );
  }
}

class _NotificationCard extends StatelessWidget {
  final NotificationModel notification;
  final AppLocalizations l10n;
  final NotificationsController controller;
  final Set<int> actedRequestIds;
  final VoidCallback onTap;

  const _NotificationCard({
    required this.notification,
    required this.l10n,
    required this.controller,
    required this.actedRequestIds,
    required this.onTap,
  });

  @override
  Widget build(BuildContext context) {
    final isUnread = !notification.isRead;
    final style = styleForNotificationType(notification.type);
    final title = NotificationLocalization.getTitle(notification, l10n);
    final message = NotificationLocalization.getMessage(notification, l10n);
    final timeStr = formatNotificationTime(notification.createdAt, l10n);
    final isDocumentAccessRequest = notification.isDocumentAccessRequest;
    final requestId = notification.documentAccessRequestId;
    // The approve/reject buttons should appear only while the request is
    // still pending. We combine three signals so the buttons disappear as
    // soon as ANY of them indicates the decision has been made:
    //   1. server-side status (source of truth across devices/sessions),
    //   2. session-local actedRequestIds (optimistic hide before refresh),
    //   3. notification was marked read (most resolutions also mark read).
    // Falling back to "pending" treats older notifications without a status
    // field as actionable, preserving backward compat.
    final requestStatus = notification.documentAccessRequestStatus?.toLowerCase();
    final isStillPending = requestStatus == null || requestStatus == 'pending';
    final showAccessButtons = isDocumentAccessRequest &&
        requestId != null &&
        isStillPending &&
        !actedRequestIds.contains(requestId);
    // The badge is rendered only when the server confirms the resolution
    // (approved / rejected). Between the user's tap and the next refetch we
    // hide the buttons via [actedRequestIds] but skip the badge to avoid
    // mislabelling approve as reject (or vice versa) using the stale cache.
    final showResolvedBadge = isDocumentAccessRequest &&
        requestId != null &&
        (requestStatus == 'approved' || requestStatus == 'rejected');
    final isApprovedBadge = requestStatus == 'approved';

    return Material(
      color: Colors.transparent,
      child: InkWell(
        onTap: isDocumentAccessRequest ? null : onTap,
        borderRadius: BorderRadius.circular(8),
        child: Container(
          padding: const EdgeInsets.all(16),
          decoration: BoxDecoration(
            color: isUnread
                ? style.color.withValues(alpha: 0.06)
                : Theme.of(context).cardColor,
            borderRadius: BorderRadius.circular(8),
            border: Border.all(color: Colors.grey.shade200, width: 1),
            boxShadow: [
              BoxShadow(
                color: Colors.black.withValues(alpha: 0.04),
                blurRadius: 8,
                offset: const Offset(0, 2),
              ),
            ],
          ),
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              Row(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  if (isUnread)
                    Padding(
                      padding: const EdgeInsets.only(right: 10, top: 6),
                      child: Container(
                        width: 8,
                        height: 8,
                        decoration: const BoxDecoration(
                          color: Color(0xFF1976D2),
                          shape: BoxShape.circle,
                        ),
                      ),
                    ),
                  Expanded(
                    child: Column(
                      crossAxisAlignment: CrossAxisAlignment.start,
                      children: [
                        Row(
                          children: [
                            Icon(style.icon, size: 20, color: style.color),
                            const SizedBox(width: 8),
                            Expanded(
                              child: Text(
                                title,
                                style: Theme.of(context).textTheme.titleSmall?.copyWith(
                                      fontWeight: FontWeight.w600,
                                      color: style.color,
                                    ),
                                maxLines: 2,
                                overflow: TextOverflow.ellipsis,
                              ),
                            ),
                            Text(
                              timeStr,
                              style: Theme.of(context).textTheme.bodySmall?.copyWith(
                                    color: Colors.grey.shade600,
                                  ),
                            ),
                          ],
                        ),
                        const SizedBox(height: 6),
                        Text(
                          message,
                          style: Theme.of(context).textTheme.bodyMedium,
                          maxLines: 3,
                          overflow: TextOverflow.ellipsis,
                        ),
                        if (showAccessButtons) ...[
                          const SizedBox(height: 12),
                          Row(
                            mainAxisAlignment: MainAxisAlignment.end,
                            children: [
                              TextButton.icon(
                                onPressed: () async {
                                  try {
                                    await controller.rejectDocumentAccessRequest(
                                      requestId!,
                                      notification.id,
                                    );
                                    if (context.mounted) {
                                      ScaffoldMessenger.of(context).showSnackBar(
                                        SnackBar(
                                          content: Text(
                                            l10n.translate('documentAccessRejected'),
                                          ),
                                        ),
                                      );
                                    }
                                  } catch (e) {
                                    if (context.mounted) {
                                      ScaffoldMessenger.of(context).showSnackBar(
                                        SnackBar(
                                          content: Text('${l10n.error}: $e'),
                                          backgroundColor: Colors.red,
                                        ),
                                      );
                                    }
                                  }
                                },
                                icon: const Icon(Icons.close, size: 18),
                                label: Text(l10n.translate('reject')),
                              ),
                              const SizedBox(width: 8),
                              FilledButton.icon(
                                onPressed: () async {
                                  try {
                                    await controller.approveDocumentAccessRequest(
                                      requestId!,
                                      notification.id,
                                    );
                                    if (context.mounted) {
                                      ScaffoldMessenger.of(context).showSnackBar(
                                        SnackBar(
                                          content: Text(
                                            l10n.translate('documentAccessApproved'),
                                          ),
                                        ),
                                      );
                                    }
                                  } catch (e) {
                                    if (context.mounted) {
                                      ScaffoldMessenger.of(context).showSnackBar(
                                        SnackBar(
                                          content: Text('${l10n.error}: $e'),
                                          backgroundColor: Colors.red,
                                        ),
                                      );
                                    }
                                  }
                                },
                                icon: const Icon(Icons.check, size: 18),
                                label: Text(l10n.translate('approve')),
                              ),
                            ],
                          ),
                        ],
                        if (showResolvedBadge) ...[
                          const SizedBox(height: 12),
                          Align(
                            alignment: Alignment.centerRight,
                            child: Container(
                              padding: const EdgeInsets.symmetric(
                                horizontal: 10,
                                vertical: 6,
                              ),
                              decoration: BoxDecoration(
                                color: (isApprovedBadge
                                        ? Colors.green
                                        : Colors.red.shade400)
                                    .withValues(alpha: 0.12),
                                borderRadius: BorderRadius.circular(20),
                              ),
                              child: Row(
                                mainAxisSize: MainAxisSize.min,
                                children: [
                                  Icon(
                                    isApprovedBadge
                                        ? Icons.check_circle
                                        : Icons.cancel,
                                    size: 16,
                                    color: isApprovedBadge
                                        ? Colors.green.shade700
                                        : Colors.red.shade700,
                                  ),
                                  const SizedBox(width: 6),
                                  Text(
                                    isApprovedBadge
                                        ? l10n.translate('documentAccessApproved')
                                        : l10n.translate('documentAccessRejected'),
                                    style: Theme.of(context)
                                        .textTheme
                                        .bodySmall
                                        ?.copyWith(
                                          color: isApprovedBadge
                                              ? Colors.green.shade800
                                              : Colors.red.shade800,
                                          fontWeight: FontWeight.w600,
                                        ),
                                  ),
                                ],
                              ),
                            ),
                          ),
                        ],
                      ],
                    ),
                  ),
                ],
              ),
            ],
          ),
        ),
      ),
    );
  }
}

class _EmptyState extends StatelessWidget {
  final bool hasFilter;
  final AppLocalizations l10n;

  const _EmptyState({this.hasFilter = false, required this.l10n});

  @override
  Widget build(BuildContext context) {
    return Center(
      child: Padding(
        padding: const EdgeInsets.all(32),
        child: Column(
          mainAxisAlignment: MainAxisAlignment.center,
          children: [
            Icon(
              Icons.notifications_none_rounded,
              size: 80,
              color: Colors.grey.shade400,
            ),
            const SizedBox(height: 24),
            Text(
              hasFilter
                  ? l10n.translate('notificationEmptyFilter')
                  : l10n.translate('noNotifications'),
              style: Theme.of(context).textTheme.titleMedium?.copyWith(
                    fontWeight: FontWeight.w600,
                  ),
              textAlign: TextAlign.center,
            ),
            const SizedBox(height: 8),
            Text(
              hasFilter
                  ? l10n.translate('notificationEmptyFilterHint')
                  : l10n.translate('notificationEmptyBody'),
              style: Theme.of(context)
                  .textTheme
                  .bodyMedium
                  ?.copyWith(color: Colors.grey.shade600),
              textAlign: TextAlign.center,
            ),
          ],
        ),
      ),
    );
  }
}
