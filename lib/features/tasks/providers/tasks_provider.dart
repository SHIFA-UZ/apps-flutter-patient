// lib/features/tasks/providers/tasks_provider.dart
import 'package:flutter/foundation.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:shifa_patient_app_v1/core/models/task_model.dart';
import 'package:shifa_patient_app_v1/core/network/api_providers.dart';
import 'package:shifa_patient_app_v1/features/tasks/data/tasks_repository.dart';

class TasksLoadState {
  const TasksLoadState({
    this.tasks = const [],
    this.isLoading = false,
    this.errorMessage,
  });

  final List<RemoteCareTask> tasks;
  final bool isLoading;
  final String? errorMessage;

  TasksLoadState copyWith({
    List<RemoteCareTask>? tasks,
    bool? isLoading,
    String? errorMessage,
    bool clearError = false,
  }) {
    return TasksLoadState(
      tasks: tasks ?? this.tasks,
      isLoading: isLoading ?? this.isLoading,
      errorMessage: clearError ? null : (errorMessage ?? this.errorMessage),
    );
  }
}

class TasksController extends StateNotifier<TasksLoadState> {
  TasksController(this.ref) : super(const TasksLoadState());

  final Ref ref;

  List<RemoteCareTask> get tasks => state.tasks;

  /// Loads all patient-visible tasks in one request (backend excludes CANCELLED only).
  Future<void> loadTasks({TaskStatus? status}) async {
    state = state.copyWith(isLoading: true, clearError: true);
    final repository = ref.read(tasksRepositoryProvider);

    try {
      final loaded = await repository.getMyTasks(status: status);
      state = TasksLoadState(tasks: loaded, isLoading: false);
      debugPrint('Tasks loaded: ${loaded.length}');
    } catch (e, stackTrace) {
      debugPrint('Error loading tasks: $e');
      debugPrint('Stack trace: $stackTrace');
      state = state.copyWith(
        isLoading: false,
        errorMessage: e.toString(),
      );
    }
  }
}

final tasksRepositoryProvider = Provider<TasksRepository>((ref) {
  return TasksRepository(ref.read(apiClientProvider));
});

final tasksProvider =
    StateNotifierProvider<TasksController, TasksLoadState>((ref) {
  return TasksController(ref);
});

final taskByIdProvider = FutureProvider.family<RemoteCareTask?, int>((ref, taskId) async {
  try {
    final repository = ref.read(tasksRepositoryProvider);
    return await repository.getMyTask(taskId);
  } catch (e) {
    debugPrint('Error loading task $taskId: $e');
    return null;
  }
});

final taskCheckInsProvider = FutureProvider.family<List<TaskCheckIn>, int>((ref, taskId) async {
  try {
    final repository = ref.read(tasksRepositoryProvider);
    return await repository.getMyCheckIns(taskId);
  } catch (e) {
    debugPrint('Error loading check-ins for $taskId: $e');
    return [];
  }
});
