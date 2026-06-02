// lib/features/tasks/presentation/screens/tasks_screen.dart
import 'package:flutter/foundation.dart';
import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:shifa_patient_app_v1/core/localization/app_localizations.dart';
import 'package:shifa_patient_app_v1/core/models/task_model.dart';
import 'package:shifa_patient_app_v1/core/theme/app_design_system.dart';
import 'package:shifa_patient_app_v1/features/tasks/providers/tasks_provider.dart';
import 'package:shifa_patient_app_v1/features/tasks/presentation/screens/task_check_in_screen.dart';
import 'package:intl/intl.dart';

class TasksScreen extends ConsumerStatefulWidget {
  const TasksScreen({Key? key}) : super(key: key);

  @override
  ConsumerState<TasksScreen> createState() => _TasksScreenState();
}

class _TasksScreenState extends ConsumerState<TasksScreen>
    with SingleTickerProviderStateMixin, WidgetsBindingObserver {
  late TabController _tabController;

  @override
  void initState() {
    super.initState();
    WidgetsBinding.instance.addObserver(this);
    _tabController = TabController(length: 2, vsync: this);
    WidgetsBinding.instance.addPostFrameCallback((_) => _reloadTasks());
  }

  void _reloadTasks() {
    if (mounted) {
      ref.read(tasksProvider.notifier).loadTasks();
    }
  }

  @override
  void didChangeAppLifecycleState(AppLifecycleState state) {
    if (state == AppLifecycleState.resumed) {
      _reloadTasks();
    }
  }

  @override
  void dispose() {
    WidgetsBinding.instance.removeObserver(this);
    _tabController.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    final l10n = AppLocalizations.of(context)!;
    final brand = Theme.of(context).colorScheme.primary;
    final tasksState = ref.watch(tasksProvider);
    final tasks = tasksState.tasks;

    final openTasks = tasks
        .where((t) =>
            t.status == TaskStatus.active || t.status == TaskStatus.draft)
        .toList();
    final completedTasks = tasks
        .where((t) =>
            t.status == TaskStatus.completed ||
            t.status == TaskStatus.expired)
        .toList();

    return Scaffold(
      appBar: AppBar(
        title: Text(l10n.myTasks ?? l10n.translate('myTasks')),
        backgroundColor: brand,
        foregroundColor: Colors.white,
        bottom: TabBar(
          controller: _tabController,
          indicatorColor: Colors.white,
          labelColor: Colors.white,
          unselectedLabelColor: Colors.white70,
          tabs: [
            Tab(text: l10n.openTasks ?? l10n.translate('openTasks')),
            Tab(text: l10n.completed),
          ],
        ),
      ),
      body: tasksState.isLoading && tasks.isEmpty
          ? const Center(child: CircularProgressIndicator())
          : Column(
              children: [
                if (tasksState.errorMessage != null)
                  Material(
                    color: Colors.orange.shade50,
                    child: Padding(
                      padding: const EdgeInsets.symmetric(
                        horizontal: 16,
                        vertical: 8,
                      ),
                      child: Row(
                        children: [
                          Icon(Icons.warning_amber, color: Colors.orange.shade800),
                          const SizedBox(width: 8),
                          Expanded(
                            child: Text(
                              l10n.error,
                              style: TextStyle(color: Colors.orange.shade900),
                            ),
                          ),
                          TextButton(
                            onPressed: _reloadTasks,
                            child: Text(l10n.retry),
                          ),
                        ],
                      ),
                    ),
                  ),
                Expanded(
                  child: TabBarView(
                    controller: _tabController,
                    children: [
                      _buildTasksList(openTasks, brand),
                      _buildTasksList(completedTasks, brand),
                    ],
                  ),
                ),
              ],
            ),
    );
  }

  Widget _buildTasksList(List<RemoteCareTask> tasks, Color brand) {
    final l10n = AppLocalizations.of(context)!;
    if (tasks.isEmpty) {
      return Center(
        child: Text(l10n.noTasksFound ?? l10n.translate('noTasksFound')),
      );
    }

    return RefreshIndicator(
      onRefresh: () async {
        await ref.read(tasksProvider.notifier).loadTasks();
      },
      child: ListView.builder(
        padding: EdgeInsets.only(
          left: 16,
          right: 16,
          top: 16,
          bottom: 16 + AppDesignSystem.safeBottomWithNavBar(context),
        ),
        itemCount: tasks.length,
        itemBuilder: (context, index) {
          final task = tasks[index];
          return _TaskCard(
            task: task,
            brand: brand,
            onTap: () async {
              await Navigator.push(
                context,
                MaterialPageRoute(
                  builder: (context) => TaskCheckInScreen(taskId: task.id),
                ),
              );
              if (context.mounted) {
                ref.read(tasksProvider.notifier).loadTasks();
              }
            },
          );
        },
      ),
    );
  }
}

class _TaskCard extends StatelessWidget {
  final RemoteCareTask task;
  final Color brand;
  final VoidCallback onTap;

  const _TaskCard({
    required this.task,
    required this.brand,
    required this.onTap,
  });

  static String _localizedCategory(TaskCategory category, AppLocalizations l10n) {
    switch (category) {
      case TaskCategory.vital:
        return l10n.translate('taskCategoryVital') ?? category.name;
      case TaskCategory.exercise:
        return l10n.translate('taskCategoryExercise') ?? category.name;
      case TaskCategory.medication:
        return l10n.translate('taskCategoryMedication') ?? category.name;
      case TaskCategory.other:
        return l10n.translate('taskCategoryOther') ?? category.name;
    }
  }

  @override
  Widget build(BuildContext context) {
    final l10n = AppLocalizations.of(context)!;
    final locale = Localizations.localeOf(context);
    final dateFormat = DateFormat('MMM dd, yyyy', locale.toString());

    return Card(
      margin: const EdgeInsets.only(bottom: 12),
      child: InkWell(
        onTap: onTap,
        borderRadius: BorderRadius.circular(12),
        child: Padding(
          padding: const EdgeInsets.all(16),
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              Row(
                children: [
                  Expanded(
                    child: Text(
                      task.taskName,
                      style: const TextStyle(
                        fontSize: 18,
                        fontWeight: FontWeight.bold,
                      ),
                    ),
                  ),
                  Container(
                    padding: const EdgeInsets.symmetric(
                      horizontal: 8,
                      vertical: 4,
                    ),
                    decoration: BoxDecoration(
                      color: brand.withOpacity(0.2),
                      borderRadius: BorderRadius.circular(8),
                    ),
                    child: Text(
                      _localizedCategory(task.category, l10n).toUpperCase(),
                      style: TextStyle(
                        color: brand,
                        fontSize: 12,
                        fontWeight: FontWeight.bold,
                      ),
                    ),
                  ),
                ],
              ),
              if (task.description != null) ...[
                const SizedBox(height: 8),
                Text(
                  task.description!,
                  style: TextStyle(
                    color: Colors.grey.shade700,
                    fontSize: 14,
                  ),
                ),
              ],
              const SizedBox(height: 12),
              if (task.progress != null) ...[
                Row(
                  children: [
                    Expanded(
                      child: LinearProgressIndicator(
                        value: task.progress!.completionPercentage,
                        backgroundColor: Colors.grey.shade200,
                        valueColor: AlwaysStoppedAnimation<Color>(brand),
                      ),
                    ),
                    const SizedBox(width: 12),
                    Text(
                      '${task.progress!.completedCheckIns}/${task.progress!.totalCheckIns}',
                      style: TextStyle(
                        color: Colors.grey.shade700,
                        fontSize: 12,
                        fontWeight: FontWeight.bold,
                      ),
                    ),
                  ],
                ),
                const SizedBox(height: 8),
              ],
              Row(
                children: [
                  Icon(Icons.calendar_today, size: 16, color: Colors.grey.shade600),
                  const SizedBox(width: 4),
                  Text(
                    task.startDate.isAfter(DateTime.now())
                        ? '${l10n.starts}: ${dateFormat.format(task.startDate)}'
                        : '${l10n.started}: ${dateFormat.format(task.startDate)}',
                    style: TextStyle(
                      color: Colors.grey.shade600,
                      fontSize: 12,
                    ),
                  ),
                  if (task.startDate.isAfter(DateTime.now())) ...[
                    const SizedBox(width: 8),
                    Container(
                      padding: const EdgeInsets.symmetric(horizontal: 6, vertical: 2),
                      decoration: BoxDecoration(
                        color: Colors.orange.shade100,
                        borderRadius: BorderRadius.circular(4),
                      ),
                      child: Text(
                        l10n.upcoming,
                        style: TextStyle(
                          color: Colors.orange.shade800,
                          fontSize: 10,
                          fontWeight: FontWeight.bold,
                        ),
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
