import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:shifa_patient_app_v1/features/copilot/data/copilot_api.dart';

final copilotApiProvider = Provider<CopilotApi>((ref) => CopilotApi());
