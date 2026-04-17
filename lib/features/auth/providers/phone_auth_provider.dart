import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:shifa_patient_app_v1/features/auth/data/phone_auth_repository.dart';

final phoneAuthRepositoryProvider = Provider<PhoneAuthRepository>((ref) => PhoneAuthRepository());
