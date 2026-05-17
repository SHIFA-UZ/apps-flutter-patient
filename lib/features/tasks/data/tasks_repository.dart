// lib/features/tasks/data/tasks_repository.dart
import 'package:dio/dio.dart';
import 'package:flutter/foundation.dart';
import 'package:shifa_patient_app_v1/core/models/task_model.dart';
import 'package:shifa_patient_app_v1/core/network/api_client.dart';

class TasksRepository {
  final ApiClient _apiClient;

  TasksRepository(this._apiClient);

  Future<List<RemoteCareTask>> getMyTasks({TaskStatus? status}) async {
    try {
      final queryParams = <String, dynamic>{};
      if (status != null) {
        queryParams['status'] = status.name.toUpperCase();
      }
      debugPrint('Fetching tasks from /tasks/my-tasks with params: $queryParams');
      final response = await _apiClient.get('/tasks/my-tasks', queryParameters: queryParams);
      debugPrint('Response status: ${response.statusCode}');
      if (response.statusCode != null && response.statusCode! >= 200 && response.statusCode! < 300) {
        final List<dynamic> data = response.data as List<dynamic>;
        debugPrint('Received ${data.length} tasks from API');
        final tasks = data
            .map((json) => RemoteCareTask.fromJson(json as Map<String, dynamic>))
            .toList();
        return tasks;
      }
      debugPrint('API returned error status: ${response.statusCode}');
      throw Exception('Failed to load tasks: ${response.statusCode}');
    } catch (e, stackTrace) {
      debugPrint('Exception in getMyTasks: $e');
      debugPrint('Stack trace: $stackTrace');
      throw Exception('Failed to load tasks: $e');
    }
  }

  Future<RemoteCareTask> getMyTask(int taskId) async {
    try {
      final response = await _apiClient.get('/tasks/my-tasks/$taskId');
      if (response.statusCode != null && response.statusCode! >= 200 && response.statusCode! < 300) {
        return RemoteCareTask.fromJson(response.data as Map<String, dynamic>);
      }
      throw Exception('Failed to load task: ${response.statusCode}');
    } catch (e) {
      throw Exception('Failed to load task: $e');
    }
  }

  Future<List<TaskCheckIn>> getMyCheckIns(int taskId) async {
    try {
      final response = await _apiClient.get('/tasks/my-tasks/$taskId/check-ins');
      if (response.statusCode != null && response.statusCode! >= 200 && response.statusCode! < 300) {
        final List<dynamic> data = response.data as List<dynamic>;
        return data
            .map((json) => TaskCheckIn.fromJson(json as Map<String, dynamic>))
            .toList();
      }
      throw Exception('Failed to load check-ins: ${response.statusCode}');
    } catch (e) {
      throw Exception('Failed to load check-ins: $e');
    }
  }

  Future<void> submitCheckIn({
    required int taskId,
    required int checkInId,
    double? numericValue,
    String? textValue,
    bool? booleanValue,
    String? notes,
  }) async {
    try {
      final body = {
        'checkInId': checkInId,
        if (numericValue != null) 'numericValue': numericValue,
        if (textValue != null) 'textValue': textValue,
        if (booleanValue != null) 'booleanValue': booleanValue,
        if (notes != null) 'notes': notes,
      };
      final response = await _apiClient.post('/tasks/my-tasks/$taskId/check-in', data: body);
      if (response.statusCode == null || response.statusCode! < 200 || response.statusCode! >= 300) {
        throw Exception('Failed to submit check-in: ${response.statusCode}');
      }
    } on DioException catch (e) {
      final msg = _shortApiErrorMessage(e);
      if (msg != null) throw Exception(msg);
      throw Exception('Failed to submit check-in: ${e.response?.statusCode}');
    } catch (e) {
      throw Exception('Failed to submit check-in: $e');
    }
  }

  /// Returns a short user-facing message from error body when available.
  static String? _shortApiErrorMessage(DioException e) {
    final data = e.response?.data;
    if (data is Map) {
      final m = data['message'];
      if (m is String && m.trim().isNotEmpty && m.length < 300) return m.trim();
    }
    if (data is String && data.trim().isNotEmpty && data.length < 300) return data.trim();
    return null;
  }
}
