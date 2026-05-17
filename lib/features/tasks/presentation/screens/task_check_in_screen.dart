// lib/features/tasks/presentation/screens/task_check_in_screen.dart
import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:shifa_patient_app_v1/core/localization/app_localizations.dart';
import 'package:shifa_patient_app_v1/core/localization/error_localizations.dart';
import 'package:shifa_patient_app_v1/core/models/task_model.dart';
import 'package:shifa_patient_app_v1/core/widgets/shifa_button.dart';
import 'package:shifa_patient_app_v1/features/tasks/providers/tasks_provider.dart';
import 'package:intl/intl.dart';

class TaskCheckInScreen extends ConsumerStatefulWidget {
  final int taskId;

  const TaskCheckInScreen({super.key, required this.taskId});

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
  /// True after submit attempt when boolean Yes/No was not chosen.
  bool _highlightBooleanRequired = false;

  @override
  void dispose() {
    _numericController.dispose();
    _textController.dispose();
    _notesController.dispose();
    super.dispose();
  }

  String _enterValueHeading(AppLocalizations l10n, String? inputLabel) {
    final label = inputLabel?.trim() ?? '';
    if (label.isEmpty) {
      return l10n.translate('checkInEnterGenericValue');
    }
    return l10n.translate('checkInEnterLabeledValue').replaceAll('{label}', label);
  }

  Future<void> _submitCheckIn(RemoteCareTask task) async {
    final l10n = AppLocalizations.of(context)!;
    if (_selectedCheckIn == null) {
      ScaffoldMessenger.of(context).showSnackBar(
        SnackBar(content: Text(l10n.pleaseSelectCheckIn ?? l10n.translate('pleaseSelectCheckIn'))),
      );
      return;
    }

    if (task.inputType == TaskInputType.boolean) {
      if (_booleanValue == null) {
        setState(() => _highlightBooleanRequired = true);
        ScaffoldMessenger.of(context).showSnackBar(
          SnackBar(content: Text(l10n.translate('checkInPleaseSelectYesOrNo'))),
        );
        return;
      }
      setState(() => _highlightBooleanRequired = false);
    } else {
      setState(() => _highlightBooleanRequired = false);
    }

    if (!_formKey.currentState!.validate()) return;

    setState(() => _isSubmitting = true);

    try {
      final repository = ref.read(tasksRepositoryProvider);

      await repository.submitCheckIn(
        taskId: widget.taskId,
        checkInId: _selectedCheckIn!.id,
        numericValue: task.inputType == TaskInputType.numeric
            ? double.tryParse(_numericController.text)
            : null,
        textValue: task.inputType == TaskInputType.text
            ? _textController.text.trim()
            : null,
        booleanValue: task.inputType == TaskInputType.boolean
            ? _booleanValue
            : null,
        notes: _notesController.text.trim().isEmpty
            ? null
            : _notesController.text.trim(),
      );

      ref.invalidate(taskByIdProvider(widget.taskId));
      ref.invalidate(taskCheckInsProvider(widget.taskId));
      ref.read(tasksProvider.notifier).loadTasks();

      if (mounted) {
        Navigator.pop(context);
        ScaffoldMessenger.of(context).showSnackBar(
          SnackBar(
            content: Text(l10n.checkInSubmittedSuccessfully ?? l10n.translate('checkInSubmittedSuccessfully')),
          ),
        );
      }
    } catch (e) {
      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
          SnackBar(
            content: Text(userFriendlyError(l10n, e, logContext: 'Task check-in submit')),
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

  Widget _buildCheckInSlotCard({
    required TaskCheckIn checkIn,
    required bool isSelected,
    required Color brand,
    required AppLocalizations l10n,
    required DateFormat dateFormat,
    required DateFormat timeFormat,
  }) {
    return Card(
      margin: const EdgeInsets.only(bottom: 8),
      color: isSelected ? brand.withOpacity(0.1) : null,
      child: RadioListTile<TaskCheckIn>(
        title: Text(dateFormat.format(checkIn.scheduledDate)),
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
            _highlightBooleanRequired = false;
          });
        },
      ),
    );
  }

  Widget _buildInlineValueForm({
    required RemoteCareTask task,
    required AppLocalizations l10n,
    required ColorScheme scheme,
  }) {
    final errorColor = scheme.error;
    final showBooleanOutline = task.inputType == TaskInputType.boolean && _highlightBooleanRequired;

    return Padding(
      padding: const EdgeInsets.only(left: 8, right: 8, bottom: 16),
      child: AnimatedContainer(
        duration: const Duration(milliseconds: 200),
        padding: const EdgeInsets.all(12),
        decoration: BoxDecoration(
          border: Border.all(
            color: showBooleanOutline ? errorColor : Colors.transparent,
            width: showBooleanOutline ? 2 : 0,
          ),
          borderRadius: BorderRadius.circular(12),
          color: scheme.surfaceContainerHighest.withValues(alpha: 0.35),
        ),
        child: Form(
          key: _formKey,
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              Text(
                _enterValueHeading(l10n, task.inputLabel),
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
                    labelText: task.inputLabel ?? l10n.translate('checkInEnterGenericValue'),
                    border: const OutlineInputBorder(),
                    hintText: l10n.exampleValue ?? 'e.g., 120/80',
                  ),
                  keyboardType: TextInputType.number,
                  onChanged: (_) => setState(() => _highlightBooleanRequired = false),
                  validator: (value) {
                    if (value == null || value.isEmpty) {
                      return l10n.translate('checkInValueRequired');
                    }
                    if (double.tryParse(value) == null) {
                      return l10n.translate('checkInValueInvalidNumber');
                    }
                    return null;
                  },
                )
              else if (task.inputType == TaskInputType.text)
                TextFormField(
                  controller: _textController,
                  decoration: InputDecoration(
                    labelText: task.inputLabel ?? l10n.translate('checkInEnterGenericValue'),
                    border: const OutlineInputBorder(),
                  ),
                  maxLines: 3,
                  onChanged: (_) => setState(() => _highlightBooleanRequired = false),
                  validator: (value) {
                    if (value == null || value.isEmpty) {
                      return l10n.translate('checkInValueRequired');
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
                        setState(() {
                          _booleanValue = value;
                          _highlightBooleanRequired = false;
                        });
                      },
                    ),
                    RadioListTile<bool>(
                      title: Text(l10n.no),
                      value: false,
                      groupValue: _booleanValue,
                      onChanged: (value) {
                        setState(() {
                          _booleanValue = value;
                          _highlightBooleanRequired = false;
                        });
                      },
                    ),
                  ],
                ),
              if (task.notesRequired || task.notesLabel != null) ...[
                const SizedBox(height: 16),
                TextFormField(
                  controller: _notesController,
                  decoration: InputDecoration(
                    labelText: task.notesLabel ?? l10n.translate('additionalNotesOptional'),
                    border: const OutlineInputBorder(),
                    hintText: l10n.additionalNotesOptional ?? l10n.translate('additionalNotesOptional'),
                  ),
                  maxLines: 3,
                  validator: task.notesRequired
                      ? (value) {
                          if (value == null || value.isEmpty) {
                            return l10n.translate('checkInNotesRequired');
                          }
                          return null;
                        }
                      : null,
                ),
              ],
              const SizedBox(height: 20),
              ShifaPrimaryButton(
                label: l10n.submitCheckIn ?? l10n.translate('submitCheckIn'),
                onPressed: _isSubmitting ? null : () => _submitCheckIn(task),
                isLoading: _isSubmitting,
              ),
            ],
          ),
        ),
      ),
    );
  }

  @override
  Widget build(BuildContext context) {
    final brand = Theme.of(context).colorScheme.primary;
    final scheme = Theme.of(context).colorScheme;
    final taskAsync = ref.watch(taskByIdProvider(widget.taskId));
    final checkInsAsync = ref.watch(taskCheckInsProvider(widget.taskId));

    final l10n = AppLocalizations.of(context)!;
    return Scaffold(
      appBar: AppBar(
        title: Text(l10n.taskCheckIn ?? l10n.translate('taskCheckIn')),
        backgroundColor: brand,
        foregroundColor: Colors.white,
      ),
      body: taskAsync.when(
        loading: () => const Center(child: CircularProgressIndicator()),
        error: (e, _) => Center(child: Text('${l10n.error}: $e')),
        data: (task) {
          if (task == null) {
            return Center(child: Text(l10n.taskNotFound ?? l10n.translate('taskNotFound')));
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
                                    valueColor: AlwaysStoppedAnimation<Color>(brand),
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
                  Text(
                    l10n.translate('selectCheckIn'),
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
                                  ? (l10n.taskCheckInNotYetAvailable ??
                                      l10n.translate('taskCheckInNotYetAvailable'))
                                  : (l10n.noPendingCheckIns ?? l10n.translate('noPendingCheckIns')),
                              textAlign: TextAlign.center,
                            ),
                          ),
                        );
                      }

                      final dateFormat = DateFormat('MMM dd, yyyy');
                      final timeFormat = DateFormat('HH:mm');

                      return Column(
                        crossAxisAlignment: CrossAxisAlignment.stretch,
                        children: [
                          for (final checkIn in pendingCheckIns) ...[
                            _buildCheckInSlotCard(
                              checkIn: checkIn,
                              isSelected: _selectedCheckIn?.id == checkIn.id,
                              brand: brand,
                              l10n: l10n,
                              dateFormat: dateFormat,
                              timeFormat: timeFormat,
                            ),
                            if (_selectedCheckIn?.id == checkIn.id)
                              _buildInlineValueForm(task: task, l10n: l10n, scheme: scheme),
                          ],
                        ],
                      );
                    },
                  ),
                ],
              ),
            ),
          );
        },
      ),
    );
  }
}
