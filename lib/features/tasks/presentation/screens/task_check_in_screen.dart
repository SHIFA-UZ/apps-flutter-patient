// lib/features/tasks/presentation/screens/task_check_in_screen.dart
import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:shifa_patient_app_v1/core/widgets/shifa_button.dart';
import 'package:shifa_patient_app_v1/core/localization/app_localizations.dart';
import 'package:shifa_patient_app_v1/core/models/task_model.dart';
import 'package:shifa_patient_app_v1/features/tasks/providers/tasks_provider.dart';
import 'package:intl/intl.dart';

class TaskCheckInScreen extends ConsumerStatefulWidget {
  final int taskId;

  const TaskCheckInScreen({Key? key, required this.taskId}) : super(key: key);

  @override
  ConsumerState<TaskCheckInScreen> createState() => _TaskCheckInScreenState();
}

class _TaskCheckInScreenState extends ConsumerState<TaskCheckInScreen> {
  final _formKey = GlobalKey<FormState>();
  final _numericController = TextEditingController();
  final _textController = TextEditingController();
  final _notesController = TextEditingController();
  bool? _booleanValue;
  bool _isSubmitting = false;
  TaskCheckIn? _selectedCheckIn;

  @override
  void dispose() {
    _numericController.dispose();
    _textController.dispose();
    _notesController.dispose();
    super.dispose();
  }

  Future<void> _submitCheckIn() async {
    final l10n = AppLocalizations.of(context)!;
    if (_selectedCheckIn == null) {
      ScaffoldMessenger.of(context).showSnackBar(
        SnackBar(content: Text(l10n.pleaseSelectCheckIn ?? 'Please select a check-in')),
      );
      return;
    }

    if (!_formKey.currentState!.validate()) return;

    setState(() => _isSubmitting = true);

    try {
      final repository = ref.read(tasksRepositoryProvider);
      final task = await ref.read(taskByIdProvider(widget.taskId).future);

      await repository.submitCheckIn(
        taskId: widget.taskId,
        checkInId: _selectedCheckIn!.id,
        numericValue: task?.inputType == TaskInputType.numeric
            ? double.tryParse(_numericController.text)
            : null,
        textValue: task?.inputType == TaskInputType.text
            ? _textController.text.trim()
            : null,
        booleanValue: task?.inputType == TaskInputType.boolean
            ? _booleanValue
            : null,
        notes: _notesController.text.trim().isEmpty
            ? null
            : _notesController.text.trim(),
      );

      // Refresh data
      ref.invalidate(taskByIdProvider(widget.taskId));
      ref.invalidate(taskCheckInsProvider(widget.taskId));
      ref.read(tasksProvider.notifier).loadTasks();

      if (mounted) {
        Navigator.pop(context);
        ScaffoldMessenger.of(context).showSnackBar(
          SnackBar(content: Text(l10n.checkInSubmittedSuccessfully ?? 'Check-in submitted successfully')),
        );
      }
    } catch (e) {
      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
          SnackBar(
            content: Text('${l10n.failedToSubmit ?? 'Failed to submit'}: $e'),
            backgroundColor: Colors.red,
          ),
        );
      }
    } finally {
      if (mounted) {
        setState(() => _isSubmitting = false);
      }
    }
  }

  @override
  Widget build(BuildContext context) {
    final brand = Theme.of(context).colorScheme.primary;
    final taskAsync = ref.watch(taskByIdProvider(widget.taskId));
    final checkInsAsync = ref.watch(taskCheckInsProvider(widget.taskId));

    final l10n = AppLocalizations.of(context)!;
    return Scaffold(
      appBar: AppBar(
        title: Text(l10n.taskCheckIn ?? 'Task Check-in'),
        backgroundColor: brand,
        foregroundColor: Colors.white,
      ),
      body: taskAsync.when(
        loading: () => const Center(child: CircularProgressIndicator()),
        error: (e, _) => Center(child: Text('${l10n.error}: $e')),
        data: (task) {
          if (task == null) {
            return Center(child: Text(l10n.taskNotFound ?? 'Task not found'));
          }

          return RefreshIndicator(
            onRefresh: () async {
              ref.invalidate(taskByIdProvider(widget.taskId));
              ref.invalidate(taskCheckInsProvider(widget.taskId));
            },
            child: SingleChildScrollView(
              padding: const EdgeInsets.all(16),
              physics: const AlwaysScrollableScrollPhysics(),
              child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                // Task Info
                Card(
                  child: Padding(
                    padding: const EdgeInsets.all(16),
                    child: Column(
                      crossAxisAlignment: CrossAxisAlignment.start,
                      children: [
                        Text(
                          task.taskName,
                          style: const TextStyle(
                            fontSize: 20,
                            fontWeight: FontWeight.bold,
                          ),
                        ),
                        if (task.description != null) ...[
                          const SizedBox(height: 8),
                          Text(
                            task.description!,
                            style: TextStyle(color: Colors.grey.shade700),
                          ),
                        ],
                        if (task.progress != null) ...[
                          const SizedBox(height: 16),
                          Row(
                            children: [
                              Expanded(
                                child: LinearProgressIndicator(
                                  value: task.progress!.completionPercentage,
                                  backgroundColor: Colors.grey.shade200,
                                  valueColor:
                                      AlwaysStoppedAnimation<Color>(brand),
                                ),
                              ),
                              const SizedBox(width: 12),
                              Text(
                                '${task.progress!.completedCheckIns}/${task.progress!.totalCheckIns}',
                                style: const TextStyle(
                                  fontWeight: FontWeight.bold,
                                ),
                              ),
                            ],
                          ),
                        ],
                      ],
                    ),
                  ),
                ),
                const SizedBox(height: 24),

                // Select Check-in
                Text(
                  'Select Check-in',
                  style: TextStyle(
                    fontSize: 18,
                    fontWeight: FontWeight.bold,
                    color: Colors.grey.shade800,
                  ),
                ),
                const SizedBox(height: 12),
                checkInsAsync.when(
                  loading: () => const Center(
                    child: Padding(
                      padding: EdgeInsets.all(32),
                      child: CircularProgressIndicator(),
                    ),
                  ),
                  error: (e, _) => Center(child: Text('${l10n.error}: $e')),
                  data: (checkIns) {
                    final now = DateTime.now();
                    // Only allow check-ins in the past or within 10 minutes in the future
                    final pendingCheckIns = checkIns
                        .where((c) =>
                            c.status == CheckInStatus.pending &&
                            c.isWithinAllowedCheckInWindow(now))
                        .toList();
                    final anyPending = checkIns.any((c) => c.status == CheckInStatus.pending);

                    if (pendingCheckIns.isEmpty) {
                      return Center(
                        child: Padding(
                          padding: const EdgeInsets.all(32),
                          child: Text(
                            anyPending
                                ? (l10n.taskCheckInNotYetAvailable ?? 'No check-ins due yet. You can submit from 10 minutes before the scheduled time.')
                                : (l10n.noPendingCheckIns ?? 'No pending check-ins'),
                            textAlign: TextAlign.center,
                          ),
                        ),
                      );
                    }

                    return Column(
                      children: pendingCheckIns.map((checkIn) {
                        final isSelected = _selectedCheckIn?.id == checkIn.id;
                        final dateFormat = DateFormat('MMM dd, yyyy');
                        final timeFormat = DateFormat('HH:mm');

                        return Card(
                          margin: const EdgeInsets.only(bottom: 8),
                          color: isSelected
                              ? brand.withOpacity(0.1)
                              : null,
                          child: RadioListTile<TaskCheckIn>(
                            title: Text(
                              dateFormat.format(checkIn.scheduledDate),
                            ),
                            subtitle: checkIn.scheduledTime != null
                                ? Text(
                                    timeFormat.format(DateTime(
                                      2000,
                                      1,
                                      1,
                                      checkIn.scheduledTime!.hour,
                                      checkIn.scheduledTime!.minute,
                                    )),
                                  )
                                : null,
                            value: checkIn,
                            groupValue: _selectedCheckIn,
                            onChanged: (value) {
                              setState(() {
                                _selectedCheckIn = value;
                                _numericController.clear();
                                _textController.clear();
                                _notesController.clear();
                                _booleanValue = null;
                              });
                            },
                          ),
                        );
                      }).toList(),
                    );
                  },
                ),
                const SizedBox(height: 24),

                // Input Form
                if (_selectedCheckIn != null) ...[
                  Form(
                    key: _formKey,
                    child: Column(
                      crossAxisAlignment: CrossAxisAlignment.start,
                      children: [
                        Text(
                          'Enter ${task.inputLabel ?? "value"}',
                          style: TextStyle(
                            fontSize: 18,
                            fontWeight: FontWeight.bold,
                            color: Colors.grey.shade800,
                          ),
                        ),
                        const SizedBox(height: 12),
                        if (task.inputType == TaskInputType.numeric)
                          TextFormField(
                            controller: _numericController,
                            decoration: InputDecoration(
                              labelText: task.inputLabel ?? 'Value',
                              border: const OutlineInputBorder(),
                              hintText: l10n.exampleValue ?? 'e.g., 120/80',
                            ),
                            keyboardType: TextInputType.number,
                            validator: (value) {
                              if (value == null || value.isEmpty) {
                                return 'Please enter a value';
                              }
                              if (double.tryParse(value) == null) {
                                return 'Please enter a valid number';
                              }
                              return null;
                            },
                          )
                        else if (task.inputType == TaskInputType.text)
                          TextFormField(
                            controller: _textController,
                            decoration: InputDecoration(
                              labelText: task.inputLabel ?? 'Value',
                              border: const OutlineInputBorder(),
                            ),
                            maxLines: 3,
                            validator: (value) {
                              if (value == null || value.isEmpty) {
                                return 'Please enter a value';
                              }
                              return null;
                            },
                          )
                        else if (task.inputType == TaskInputType.boolean)
                          Column(
                            children: [
                              RadioListTile<bool>(
                                title: Text(l10n.yes),
                                value: true,
                                groupValue: _booleanValue,
                                onChanged: (value) {
                                  setState(() => _booleanValue = value);
                                },
                              ),
                              RadioListTile<bool>(
                                title: Text(l10n.no),
                                value: false,
                                groupValue: _booleanValue,
                                onChanged: (value) {
                                  setState(() => _booleanValue = value);
                                },
                              ),
                            ],
                          ),
                        if (task.notesRequired || task.notesLabel != null) ...[
                          const SizedBox(height: 16),
                          TextFormField(
                            controller: _notesController,
                            decoration: InputDecoration(
                              labelText: task.notesLabel ?? 'Notes',
                              border: const OutlineInputBorder(),
                              hintText: l10n.additionalNotesOptional ?? 'Additional notes (optional)',
                            ),
                            maxLines: 3,
                            validator: task.notesRequired
                                ? (value) {
                                    if (value == null || value.isEmpty) {
                                      return 'Notes are required';
                                    }
                                    return null;
                                  }
                                : null,
                          ),
                        ],
                        const SizedBox(height: 24),
                        ShifaPrimaryButton(
                          label: l10n.submitCheckIn ?? 'Submit Check-in',
                          onPressed: _isSubmitting ? null : _submitCheckIn,
                          isLoading: _isSubmitting,
                        ),
                      ],
                    ),
                  ),
                ],
              ],
            ),
            ),
          );
        },
      ),
    );
  }
}
