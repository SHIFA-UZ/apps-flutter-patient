// lib/features/tasks/providers/tasks_provider.dart
import 'package:flutter/foundation.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:shifa_patient_app_v1/core/models/task_model.dart';
import 'package:shifa_patient_app_v1/core/network/api_providers.dart';
import 'package:shifa_patient_app_v1/features/tasks/data/tasks_repository.dart';

class TasksController extends StateNotifier<List<RemoteCareTask>> {
  TasksController(this.ref) : super([]);

  final Ref ref;

  /// Loads both open (active/draft) and completed tasks so both tabs have data.
  Future<void> loadTasks({TaskStatus? status}) async {
    try {
      final repository = ref.read(tasksRepositoryProvider);
      if (status != null) {
        debugPrint('Loading tasks with status: ${status.name}');
        final tasks = await repository.getMyTasks(status: status);
        debugPrint('Loaded ${tasks.length} tasks');
        state = tasks;
      } else {
        // Load active (open) and completed in parallel so both tabs show data
        debugPrint('Loading open and completed tasks');
        final results = await Future.wait([
          repository.getMyTasks(status: null), // active + future
          repository.getMyTasks(status: TaskStatus.completed),
        ]);
        final openTasks = results[0];
        final completedTasks = results[1];
        state = [...openTasks, ...completedTasks];
        debugPrint('Loaded ${openTasks.length} open, ${completedTasks.length} completed');
      }
    } catch (e, stackTrace) {
      debugPrint('Error loading tasks: $e');
      debugPrint('Stack trace: $stackTrace');
      state = [];
    }
  }
}

final tasksRepositoryProvider = Provider<TasksRepository>((ref) {
  return TasksRepository(ref.read(apiClientProvider));
});

final tasksProvider =
    StateNotifierProvider<TasksController, List<RemoteCareTask>>((ref) {
  return TasksController(ref);
});

final taskByIdProvider = FutureProvider.family<RemoteCareTask?, int>((ref, taskId) async {
  try {
    final repository = ref.read(tasksRepositoryProvider);
    return await repository.getMyTask(taskId);
  } catch (e) {
    return null;
  }
});

final taskCheckInsProvider = FutureProvider.family<List<TaskCheckIn>, int>((ref, taskId) async {
  try {
    final repository = ref.read(tasksRepositoryProvider);
    return await repository.getMyCheckIns(taskId);
  } catch (e) {
    return [];
  }
});
