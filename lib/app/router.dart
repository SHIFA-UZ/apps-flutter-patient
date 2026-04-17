import 'dart:async';
import 'package:flutter/material.dart';
import 'package:go_router/go_router.dart';
import 'package:shifa_patient_app_v1/core/utils/app_logger.dart';
import 'package:shifa_patient_app_v1/features/auth/presentation/screens/splash_screen.dart';
import 'package:shifa_patient_app_v1/features/auth/presentation/screens/login_screen.dart';
import 'package:shifa_patient_app_v1/features/auth/presentation/screens/create_account_screen.dart';
import 'package:shifa_patient_app_v1/features/auth/presentation/screens/account_info_screen.dart';
import 'package:shifa_patient_app_v1/features/auth/presentation/screens/reset_password_screen.dart';
import 'package:shifa_patient_app_v1/features/auth/presentation/screens/change_password_screen.dart';
import 'package:shifa_patient_app_v1/features/auth/presentation/screens/confirm_doctor_to_patient_screen.dart';
import 'package:shifa_patient_app_v1/features/auth/presentation/screens/doctor_otp_verify_screen.dart';
import 'package:shifa_patient_app_v1/features/auth/presentation/screens/register_otp_verify_screen.dart';
import 'package:shifa_patient_app_v1/features/auth/presentation/screens/forgot_password_screen.dart';
import 'package:shifa_patient_app_v1/features/auth/presentation/screens/forgot_password_otp_screen.dart';
import 'package:shifa_patient_app_v1/features/auth/presentation/screens/forgot_password_new_password_screen.dart';
import 'package:shifa_patient_app_v1/features/home/presentation/screens/home_screen.dart';
import 'package:shifa_patient_app_v1/features/bookings/presentation/screens/bookings_screen.dart';
import 'package:shifa_patient_app_v1/features/documents/presentation/screens/documents_screen.dart';
import 'package:shifa_patient_app_v1/features/doctors/presentation/screens/doctors_screen.dart';
import 'package:shifa_patient_app_v1/features/profile/presentation/screens/profile_screen.dart';
import 'package:shifa_patient_app_v1/features/bookings/presentation/screens/create_booking_screen.dart';
import 'package:shifa_patient_app_v1/features/doctors/presentation/screens/doctor_profile_screen.dart';
import 'package:shifa_patient_app_v1/features/bookings/presentation/screens/appointment_details_screen.dart';
import 'package:shifa_patient_app_v1/features/bookings/presentation/screens/submit_review_screen.dart';
import 'package:shifa_patient_app_v1/features/documents/presentation/screens/document_details_screen.dart';
import 'package:shifa_patient_app_v1/features/documents/presentation/screens/document_viewer_screen.dart';
import 'package:shifa_patient_app_v1/core/models/document_model.dart';
import 'package:shifa_patient_app_v1/features/profile/presentation/screens/edit_profile_screen.dart';
import 'package:shifa_patient_app_v1/features/profile/presentation/screens/delete_account_otp_verify_screen.dart';
import 'package:shifa_patient_app_v1/features/settings/presentation/screens/app_lock_settings_screen.dart';
import 'package:shifa_patient_app_v1/features/bookings/presentation/screens/appointment_booking_flow_screen.dart';
import 'package:shifa_patient_app_v1/features/bookings/presentation/screens/confirm_booking_screen.dart';
import 'package:shifa_patient_app_v1/features/bookings/presentation/screens/video_call_screen.dart';
import 'package:shifa_patient_app_v1/features/bookings/presentation/screens/waiting_room_screen.dart';
import 'package:shifa_patient_app_v1/features/bookings/presentation/screens/sign_appointment_screen.dart';
import 'package:shifa_patient_app_v1/features/bookings/presentation/screens/visit_summary_screen.dart';
import 'package:shifa_patient_app_v1/features/chat/presentation/chat_screen.dart';
import 'package:shifa_patient_app_v1/features/notifications/presentation/screens/notifications_screen.dart';
import 'package:shifa_patient_app_v1/features/tasks/presentation/screens/tasks_screen.dart';
import 'package:shifa_patient_app_v1/features/tasks/presentation/screens/task_check_in_screen.dart';
import 'package:shifa_patient_app_v1/features/auth/providers/auth_provider.dart';
import 'package:shifa_patient_app_v1/app/main_shell.dart';
import 'package:shifa_patient_app_v1/app/persistent_bottom_bar.dart';
import 'package:shifa_patient_app_v1/core/widgets/app_lock_wrapper.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

// Helper class to make AuthState listenable for GoRouter
class GoRouterRefreshStream extends ChangeNotifier {
  GoRouterRefreshStream(Stream<AuthState> stream) {
    notifyListeners();
    _subscription = stream.listen((_) => notifyListeners());
  }

  late final StreamSubscription<AuthState> _subscription;

  @override
  void dispose() {
    _subscription.cancel();
    super.dispose();
  }
}

// Helper class to make Riverpod StateNotifier listenable for GoRouter
class GoRouterRefreshNotifier extends ChangeNotifier {
  GoRouterRefreshNotifier(this._ref) {
    // Watch auth state and notify router when it changes
    try {
      _ref.listen<AuthState>(authStateProvider, (previous, next) {
        // Only notify if authentication status actually changed
        if (previous?.isAuthenticated != next.isAuthenticated) {
          notifyListeners();
        }
      });
    } catch (e) {
      AppLogger.error('Error setting up GoRouterRefreshNotifier:', e);
    }
  }

  final Ref _ref;

  // Expose ref so redirect can access it
  Ref get ref => _ref;
}

/// Allowed route prefixes. Any path not matching these is redirected to home.
const _allowedRoutePrefixes = [
  '/',
  '/login',
  '/create-account',
  '/confirm-doctor-to-patient',
  '/doctor-otp-verify',
  '/register-otp-verify',
  '/account-info',
  '/reset-password',
  '/forgot-password',
  '/forgot-password-otp',
  '/forgot-password-new-password',
  '/home',
  '/bookings',
  '/documents',
  '/doctors',
  '/account',
  '/account/delete-account-verify',
  '/chat',
  '/notifications',
  '/tasks',
];

bool _isAllowedRoute(String location) {
  if (location.isEmpty) return true;
  for (final prefix in _allowedRoutePrefixes) {
    if (prefix == '/' && (location == '/' || location == '')) return true;
    if (prefix != '/' &&
        (location == prefix || location.startsWith('$prefix/'))) {
      return true;
    }
  }
  return false;
}

class AppRoutes {
  // Splash
  static const splash = '/';

  // Auth routes
  static const login = '/login';
  static const createAccount = '/create-account';
  static const confirmDoctorToPatient = '/confirm-doctor-to-patient';
  static const doctorOtpVerify = '/doctor-otp-verify';
  static const registerOtpVerify = '/register-otp-verify';
  static const accountInfo = '/account-info';
  static const resetPassword = '/reset-password';
  static const forgotPassword = '/forgot-password';
  static const forgotPasswordOtp = '/forgot-password-otp';
  static const forgotPasswordNewPassword = '/forgot-password-new-password';

  // Main tabs
  static const home = '/home';
  static const bookings = '/bookings';
  static const documents = '/documents';
  static const doctors = '/doctors';
  static const account = '/account';
  static const chat = '/chat';
  static const notifications = '/notifications';
  static const tasks = '/tasks';
  static const taskCheckIn = '/tasks/:id/check-in';

  // Nested routes
  static const createBooking = '/bookings/create';
  static const appointmentDetails = '/bookings/:id';
  static const doctorProfile = '/doctors/:id';
  static const documentDetails = '/documents/:id';
  static const documentViewer = '/documents/viewer';
  static const editProfile = '/account/edit';
  static const changePassword = '/account/change-password';
  static const appLockSettings = '/account/app-lock';
  static const deleteAccountVerify = '/account/delete-account-verify';
  static const bookingFlow = '/bookings/flow';
  static const confirmBooking = '/bookings/confirm';
  static const videoCall = '/bookings/:id/video';
  static const waitingRoom = '/bookings/:id/waiting';
  static const signAppointment = '/bookings/:id/sign';
  static const visitSummary = '/bookings/:id/visit-summary';
}

// Router provider - will be created in app.dart
final routerProvider = Provider<GoRouter>((ref) {
  // Don't watch authState here - let the refresh notifier handle it
  // This prevents the router from being recreated on every auth state change

  // Create a refresh notifier that listens to auth state changes
  final refreshNotifier = GoRouterRefreshNotifier(ref);

  return GoRouter(
    initialLocation: AppRoutes.splash,
    refreshListenable: refreshNotifier,
    debugLogDiagnostics: true,
    // Handle unknown routes including Firebase Auth reCAPTCHA callbacks.
    // On iOS, Firebase Phone Auth opens Safari for reCAPTCHA verification,
    // then redirects back via URL scheme with path "/link". GoRouter must
    // ignore this to keep the user on the current screen (e.g. forgot-password).
    onException: (context, state, router) {
      final uri = state.uri.toString();
      if (uri.contains('/link') || uri.contains('/__/auth')) {
        // Swallow Firebase callback URLs — don't navigate anywhere.
        // Firebase SDK handles the URL internally via the URL scheme handler.
        return;
      }
      // For all other unknown routes, go to login
      router.go(AppRoutes.login);
    },
    redirect: (context, state) {
      try {
        // Get auth state inside redirect to ensure it's current
        // Use the refresh notifier's ref to access providers
        final authState = refreshNotifier.ref.read(authStateProvider);

        // Don't redirect from splash screen - let it handle navigation
        if (state.matchedLocation == AppRoutes.splash) {
          return null;
        }

        // Firebase Auth reCAPTCHA callback (iOS) — redirect to forgot-password
        // so the widget stays mounted and codeSent callback can navigate to OTP
        if (state.uri.toString().contains('/link') ||
            state.matchedLocation == '/link') {
          return AppRoutes.forgotPassword;
        }

        final isAuthenticated = authState.isAuthenticated;
        final forcePasswordReset = authState.forcePasswordReset;

        final isAuthRoute =
            state.matchedLocation == AppRoutes.login ||
            state.matchedLocation == AppRoutes.createAccount ||
            state.matchedLocation == AppRoutes.confirmDoctorToPatient ||
            state.matchedLocation == AppRoutes.doctorOtpVerify ||
            state.matchedLocation == AppRoutes.registerOtpVerify ||
            state.matchedLocation == AppRoutes.accountInfo ||
            state.matchedLocation == AppRoutes.resetPassword ||
            state.matchedLocation == AppRoutes.forgotPassword ||
            state.matchedLocation == AppRoutes.forgotPasswordOtp ||
            state.matchedLocation == AppRoutes.forgotPasswordNewPassword;

        if (!isAuthenticated && !isAuthRoute) {
          return AppRoutes.login;
        }

        // If authenticated but needs reset, force them to reset screen
        if (isAuthenticated &&
            forcePasswordReset &&
            state.matchedLocation != AppRoutes.resetPassword) {
          return AppRoutes.resetPassword;
        }

        // If authenticated and DOES NOT need reset, keep them away from reset screen
        if (isAuthenticated &&
            !forcePasswordReset &&
            state.matchedLocation == AppRoutes.resetPassword) {
          return AppRoutes.home;
        }

        // Immediately redirect authenticated users away from auth routes (except reset)
        if (isAuthenticated && !forcePasswordReset && isAuthRoute) {
          return AppRoutes.home;
        }

        // Whitelist: redirect unknown routes to home
        if (!_isAllowedRoute(state.matchedLocation)) {
          return isAuthenticated ? AppRoutes.home : AppRoutes.login;
        }

        return null;
      } catch (e) {
        AppLogger.error('Router redirect error:', e);
        // If there's an error in redirect, default to login
        return AppRoutes.login;
      }
    },
    routes: [
      // Firebase Auth reCAPTCHA callback (iOS) — redirected to /forgot-password
      GoRoute(
        path: '/link',
        builder: (context, state) => const SizedBox.shrink(),
      ),

      // Splash screen
      GoRoute(
        path: AppRoutes.splash,
        builder: (context, state) => const SplashScreen(),
      ),

      // Auth routes
      GoRoute(
        path: AppRoutes.login,
        builder: (context, state) => const LoginScreen(),
      ),
      GoRoute(
        path: AppRoutes.createAccount,
        builder: (context, state) => const CreateAccountScreen(),
      ),
      GoRoute(
        path: AppRoutes.confirmDoctorToPatient,
        builder: (context, state) {
          final extra = state.extra as Map<String, dynamic>?;
          if (extra == null) return const CreateAccountScreen();
          return ConfirmDoctorToPatientScreen(
            phone: extra['phone'] as String,
            email: extra['email'] as String?,
            doctorFirstName: extra['firstName'] as String,
            doctorLastName: extra['lastName'] as String,
          );
        },
      ),
      GoRoute(
        path: AppRoutes.doctorOtpVerify,
        builder: (context, state) => const DoctorOtpVerifyScreen(),
      ),
      GoRoute(
        path: AppRoutes.registerOtpVerify,
        builder: (context, state) => const RegisterOtpVerifyScreen(),
      ),
      GoRoute(
        path: AppRoutes.accountInfo,
        builder: (context, state) => const AccountInfoScreen(),
      ),
      GoRoute(
        path: AppRoutes.resetPassword,
        builder: (context, state) => const ResetPasswordScreen(),
      ),
      GoRoute(
        path: AppRoutes.forgotPassword,
        builder: (context, state) => const ForgotPasswordScreen(),
      ),
      GoRoute(
        path: AppRoutes.forgotPasswordOtp,
        builder: (context, state) => const ForgotPasswordOtpScreen(),
      ),
      GoRoute(
        path: AppRoutes.forgotPasswordNewPassword,
        builder: (context, state) => const ForgotPasswordNewPasswordScreen(),
      ),
      GoRoute(
        path: AppRoutes.deleteAccountVerify,
        builder: (context, state) {
          final extra = state.extra;
          if (extra is! Map<String, dynamic>) return const LoginScreen();
          final challengeId = extra['challengeId']?.toString() ?? '';
          final email = extra['email']?.toString();
          if (challengeId.isEmpty || email == null || email.trim().isEmpty) {
            return const LoginScreen();
          }
          return DeleteAccountOtpVerifyScreen(
            challengeId: challengeId,
            email: email,
          );
        },
      ),

      // Main shell with bottom navigation (IndexedStack keeps tab state)
      StatefulShellRoute.indexedStack(
        builder: (context, state, navigationShell) =>
            AppLockWrapper(child: MainShell(navigationShell: navigationShell)),
        branches: [
          StatefulShellBranch(
            routes: [
              GoRoute(
                path: AppRoutes.home,
                builder: (context, state) => const HomeScreen(),
              ),
            ],
          ),
          StatefulShellBranch(
            routes: [
              GoRoute(
                path: AppRoutes.bookings,
                builder: (context, state) => const BookingsScreen(),
                routes: [
                  GoRoute(
                    path: 'create',
                    builder: (context, state) => const CreateBookingScreen(),
                  ),
                  GoRoute(
                    path: ':id',
                    builder: (context, state) {
                      final id = state.pathParameters['id']!;
                      return AppointmentDetailsScreen(appointmentId: id);
                    },
                    routes: [
                      GoRoute(
                        path: 'waiting',
                        builder: (context, state) {
                          final id = state.pathParameters['id']!;
                          return WaitingRoomScreen(appointmentId: id);
                        },
                      ),
                      GoRoute(
                        path: 'review',
                        builder: (context, state) {
                          final id = state.pathParameters['id']!;
                          final extra = state.extra as Map<String, dynamic>?;
                          return SubmitReviewScreen(
                            doctorId: extra?['doctorId'] as String? ?? '',
                            doctorName:
                                extra?['doctorName'] as String? ?? 'Doctor',
                            appointmentId: id,
                          );
                        },
                      ),
                      GoRoute(
                        path: 'sign',
                        builder: (context, state) {
                          final id = state.pathParameters['id']!;
                          return SignAppointmentScreen(appointmentId: id);
                        },
                      ),
                      GoRoute(
                        path: 'visit-summary',
                        builder: (context, state) {
                          final id = state.pathParameters['id']!;
                          return VisitSummaryScreen(appointmentId: id);
                        },
                      ),
                    ],
                  ),
                  GoRoute(
                    path: 'flow/:doctorId',
                    builder: (context, state) {
                      final doctorId = state.pathParameters['doctorId']!;
                      final rescheduleId =
                          state.uri.queryParameters['rescheduleId'];
                      return AppointmentBookingFlowScreen(
                        doctorId: doctorId,
                        rescheduleId: rescheduleId,
                      );
                    },
                  ),
                ],
              ),
            ],
          ),
          StatefulShellBranch(
            routes: [
              GoRoute(
                path: AppRoutes.documents,
                builder: (context, state) => const DocumentsScreen(),
                routes: [
                  GoRoute(
                    path: 'viewer',
                    builder: (context, state) {
                      final document = state.extra as DocumentModel?;
                      if (document == null) {
                        return const DocumentsScreen();
                      }
                      return DocumentViewerScreen(document: document);
                    },
                  ),
                  GoRoute(
                    path: ':id',
                    builder: (context, state) {
                      final id = state.pathParameters['id']!;
                      return DocumentDetailsScreen(documentId: id);
                    },
                  ),
                ],
              ),
            ],
          ),
          StatefulShellBranch(
            routes: [
              GoRoute(
                path: AppRoutes.doctors,
                builder: (context, state) => const DoctorsScreen(),
                routes: [
                  GoRoute(
                    path: ':id',
                    builder: (context, state) {
                      final id = state.pathParameters['id']!;
                      return DoctorProfileScreen(doctorId: id);
                    },
                  ),
                ],
              ),
            ],
          ),
        ],
      ),
      // Shell so account, chat, notifications, tasks also show the persistent bottom bar
      ShellRoute(
        builder: (context, state, child) => AppLockWrapper(
          child: Scaffold(
            body: child,
            resizeToAvoidBottomInset: true,
            bottomNavigationBar: const PersistentBottomBar(),
          ),
        ),
        routes: [
          GoRoute(
            path: AppRoutes.account,
            builder: (context, state) => const ProfileScreen(),
            routes: [
              GoRoute(
                path: 'edit',
                builder: (context, state) => const EditProfileScreen(),
              ),
              GoRoute(
                path: 'change-password',
                builder: (context, state) => const ChangePasswordScreen(),
              ),
              GoRoute(
                path: 'app-lock',
                builder: (context, state) => const AppLockSettingsScreen(),
              ),
            ],
          ),
          GoRoute(
            path: AppRoutes.chat,
            builder: (context, state) => const ChatScreen(),
          ),
          GoRoute(
            path: AppRoutes.notifications,
            builder: (context, state) => const NotificationsScreen(),
          ),
          GoRoute(
            path: AppRoutes.tasks,
            builder: (context, state) => const TasksScreen(),
            routes: [
              GoRoute(
                path: ':id/check-in',
                builder: (context, state) {
                  final id = int.tryParse(state.pathParameters['id'] ?? '');
                  if (id == null) {
                    return const TasksScreen();
                  }
                  return TaskCheckInScreen(taskId: id);
                },
              ),
            ],
          ),
        ],
      ),

      // Confirm booking (separate route, no bottom nav)
      GoRoute(
        path: AppRoutes.confirmBooking,
        builder: (context, state) {
          final extra = state.extra as Map<String, dynamic>?;
          return ConfirmBookingScreen(
            doctorId: extra?['doctorId'] as String? ?? '',
            date: extra?['date'] as String? ?? '',
            time: extra?['time'] as String? ?? '',
            reason: extra?['reason'] as String?,
            isVideo: extra?['isVideo'] as bool? ?? false,
          );
        },
      ),

      // Video call (fullscreen, no bottom nav)
      GoRoute(
        path: AppRoutes.videoCall,
        builder: (context, state) {
          final id = state.pathParameters['id']!;
          return VideoCallScreen(appointmentId: id);
        },
      ),
    ],
  );
});
