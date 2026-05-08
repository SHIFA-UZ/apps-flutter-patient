import 'package:flutter/material.dart';
import 'package:shifa_patient_app_v1/core/models/profession_model.dart';

class AppLocalizations {
  final Locale locale;

  AppLocalizations(this.locale);

  static AppLocalizations? of(BuildContext context) {
    return Localizations.of<AppLocalizations>(context, AppLocalizations);
  }

  static const LocalizationsDelegate<AppLocalizations> delegate =
      _AppLocalizationsDelegate();

  static final Map<String, Map<String, String>> _localizedValues = {
    'en': {
      // Common
      'appName': 'Shifa Patient',
      'hello': 'Hello',
      'patient': 'Patient',
      'doctor': 'Doctor',
      'loading': 'Loading...',
      'error': 'Error',
      'retry': 'Retry',
      'cancel': 'Cancel',
      'ok': 'OK',
      'skip': 'Skip',
      'save': 'Save',
      'delete': 'Delete',
      'deleteDocumentConfirmation':
          'Are you sure you want to delete this document?',
      'documentDeleted': 'Document deleted',
      'edit': 'Edit',
      'back': 'Back',
      'next': 'Next',
      'complete': 'Complete',
      'submit': 'Submit',
      'close': 'Close',
      'yes': 'Yes',
      'no': 'No',

      // Auth
      'login': 'Login',
      'signIn': 'Sign In',
      'createAccount': 'Create Account',
      'existingDoctorTitle': 'Existing doctor account found',
      'existingDoctorMessage':
          'You already have a doctor account for {{name}}. Create your patient account to use the patient app; you will use your doctor password to log in. A verification code will be sent to your phone.',
      'createPatientAccount': 'Create patient account',
      'accountAlreadyExists': 'Account already exists',
      'existingPatientMessage':
          'An account with this email or phone already exists. Use Forgot Password to sign in, or enter a different email and phone number.',
      'doctorPatientAccountMessage':
          'You will use your doctor account password to log in. A verification code will be sent to your phone and email.',
      'sendVerificationCode': 'Send verification code',
      'verificationCodeSent':
          'Verification codes sent to your phone and email.',
      'enterPhoneCode': 'Enter code from SMS',
      'enterEmailCode': 'Enter code from email',
      'verifyAndCreate': 'Verify and create account',
      'invalidVerificationCode': 'Invalid or expired code',
      'resendCode': 'Resend code',
      'resendCodeIn': 'Resend code in {{time}}',
      'codeSentAgain': 'Verification code sent again',
      'codeExpiresIn': 'Code expires in {{time}}',
      'phoneOrEmail': 'Phone Number or Email',
      'password': 'Password',
      'confirmPassword': 'Confirm Password',
      'continueWithGoogle': 'Continue with Google',
      'forgotPassword': 'Forgot Password?',
      'forgotPasswordEnterPhone':
          'Enter your phone number. We will send you a one-time code to reset your password.',
      'forgotPasswordEnterEmail':
          'Enter your email address. We will send you a verification code to reset your password.',
      'otpSentToEmail': 'A 6-digit code has been sent to {email}. Check your inbox.',
      'emailRequired': 'Email address is required',
      'noAccountWithPhone': 'No account found with this phone number.',
      'sendCode': 'Send code',
      'verify': 'Verify',
      'enterNewPassword': 'Enter your new password and confirm it.',
      'passwordResetSuccess': 'Password reset successfully.',
      'loginAttemptsRemaining':
          '{{count}} login attempt(s) remaining before temporary lockout.',
      'accountLockedTryAgainIn':
          'Too many failed attempts. Account temporarily locked. Please try again in {{minutes}} minute(s).',

      // Profile
      'profile': 'Profile',
      'editProfile': 'Edit Profile',
      'language': 'Language',
      'preferences': 'Preferences',
      'settingsTitle': 'Settings',
      'accountTitle': 'Account',
      'privacy': 'Privacy',
      'deleteAccount': 'Delete my Account',
      'deleteAccountWarning':
          'This action is irreversible. Your personal identifiers (phone/email/name) will be removed. Some medical data may be retained as required by law, but it will not be accessible from a new account.',
      'deleteAccountVerifyTitle': 'Verify to delete account',
      'deleteAccountOtpSubtitle': 'Enter the 6-digit code sent to your email.',
      'verificationCode': 'Verification code',
      'confirmDeletion': 'Confirm deletion',
      'accountDeletedSuccess': 'Your account has been successfully deleted',
      'logOut': 'Log Out',
      'signOutConfirm': 'Are you sure you want to sign out?',
      'birthDate': 'Birth Date',
      'birthdate': 'Birthdate',
      'phoneNumber': 'Phone Number',
      'email': 'Email',
      'address': 'Address',
      'name': 'Name',
      'firstName': 'First name',
      'lastName': 'Last name',
      'surname': 'Surname',
      'gender': 'Gender',
      'male': 'Male',
      'female': 'Female',
      'other': 'Other',
      'uploadPhoto': 'Upload Photo',
      'selectLanguage': 'Select Language',
      'languageChanged': 'Language changed to',

      // Home
      'home': 'Home',
      'upcomingAppointments': 'Upcoming Appointments',
      'doctorsNearMe': 'Doctors near me',
      'noUpcomingAppointments': 'No upcoming appointments',
      'noDoctorsAvailable': 'No doctors available',
      'noDoctorsInCloseDistance': 'No doctors in close distance',
      'myTasks': 'My Tasks',
      'remoteCareTasks': 'Remote Care Tasks',
      'viewAndCompleteAssignedTasks': 'View and complete your assigned tasks',
      'openTasks': 'Open Tasks',
      'viewTasks': 'View tasks',
      'useMyLocation': 'Use my location',
      'getCurrentLocation': 'Get Current Location',
      'latitude': 'Latitude',
      'longitude': 'Longitude',

      // Shifa AI (co-pilot)
      'shifaAiTitle': 'Shifa AI',
      'shifaAiCardSubtitle':
          'Your co-pilot — ask questions, describe symptoms, find doctors',
      'shifaAiDisclaimer':
          'Shifa AI provides general information only. It is not a substitute for professional medical advice, diagnosis, or treatment.',
      'copilotInputHint': 'Type a message…',
      'copilotSuggestDoctors': 'Suggest doctors',
      'copilotSuggestedDoctors': 'Suggested doctors',
      'copilotNoSuggestedDoctors':
          'No matching doctors found. Try the Doctors tab to search.',
      'copilotBookWithDoctor': 'Book',
      'copilotViewProfile': 'Profile',
      'copilotTranscribeError': 'Could not transcribe audio',
      'copilotContinueToBooking':
          'You will choose date and time on the next screen. Continue?',
      'copilotBookingTitle': 'Book appointment',
      'copilotBookManual': 'Choose slot',
      'copilotAutoBook': 'Auto-book',
      'copilotAutoBookExplainer':
          'Shifa will book the nearest available slot to your preferred date and time. You must confirm consent below.',
      'copilotPreferredDate': 'Preferred date',
      'copilotPreferredTime': 'Preferred time',
      'copilotConsentAutoBook': 'I allow Shifa to book this appointment on my behalf',
      'copilotAutoBookSubmit': 'Confirm auto-booking',
      'copilotBookedSuccess': 'Appointment booked. Check Bookings for details.',
      'copilotBookedViaAiReason': 'Booked via Shifa AI',
      'copilotConfirmBookFromChatTitle': 'Confirm booking from chat',
      'copilotConfirmBookFromChatExplainer':
          'Shifa will book the nearest available slot to the time you discussed. This uses your earlier agreement in this conversation.',
      'copilotNoProviderOnPlatform':
          'Shifa does not list a doctor on the platform who matches these symptoms yet. You can browse all doctors in the Doctors tab or try again later as more providers join. If you feel very unwell, seek urgent in-person care or emergency services.',
      'copilotNextSlot': 'Next slot:',
      'copilotConfidence': 'Confidence:',
      'copilotConfidenceHigh': 'High',
      'copilotConfidenceMedium': 'Medium',
      'copilotConfidenceLow': 'Low',
      'copilotThinking': 'Thinking…',

      // Bookings
      'bookings': 'Bookings',
      'upcoming': 'Upcoming',
      'past': 'Past',
      'createBooking': 'Create Booking',
      'noAppointmentsFound': 'No appointments found',
      'appointmentDetails': 'Appointment Details',
      'dateAndTime': 'Date and Time',
      'location': 'Location',
      'about': 'About',
      'services': 'Services',
      'certificates': 'Certificates',
      'contacts': 'Contacts',
      'selectLocationOnMap': 'Select location on map',
      'country': 'Country',
      'region': 'Region',
      'district': 'District',
      'city': 'City',
      'postalCode': 'Postal Code',
      'streetAddress': 'Street Address',
      'enterStreetAddress': 'Enter street address, building name, floor, etc.',
      'streetAddressHelper':
          'You can edit this field to add building details, floor, room number, etc.',
      'couldNotGetAddressDetails':
          'Could not get address details. Please try selecting a different location.',
      'locationServicesDisabled':
          'Location services are disabled. Please enable them.',
      'locationPermissionDenied': 'Location permissions are denied.',
      'locationPermissionDeniedForever':
          'Location permissions are permanently denied. Please enable them in settings.',
      'microphonePermissionDenied':
          'Microphone access is needed to record voice messages.',
      'cameraPermissionDenied': 'Camera access is needed to take photos.',
      'permissionNeeded': 'Permission needed',
      'permissionRationaleCamera':
          'Shifa needs camera access to take profile photos and photos for documents or chat.',
      'permissionRationaleMicrophone':
          'Shifa needs microphone access for voice messages in chat, for Shifa AI voice input, and for video calls with your doctor.',
      'permissionRationaleLocation':
          'Shifa needs location access so you can select your address on the map when filling your profile.',
      'permissionRationaleNotifications':
          'Shifa needs notification permission to alert you about appointments, messages, and documents.',
      'errorGettingCurrentLocation': 'Error getting current location',
      'selectedLocation': 'Selected Location',
      'reasonForVisit': 'Reason for visit',
      'selectDate': 'Select date',
      'availableTimes': 'Available times',
      'selectLocation': 'Select location',
      'primary': 'Primary',
      'noAvailableTimeSlots': 'No available time slots for this date',
      'errorLoadingSlots': 'Error loading slots',
      'loadingDocument': 'Loading document...',
      'unsupportedDocumentType':
          'This file type cannot be displayed in the app. Supported: PDF and images (JPG, PNG, WebP).',
      'download': 'Download',
      'downloadStarted': 'Download started',
      'reasonForVisitOptional': 'Reason for Visit (Optional)',
      'videoConsultation': 'Video consultation',
      'haveYourAppointment': 'Have your appointment via video call',
      'optional': 'Optional',
      'describeYourReason': 'Describe your reason for the visit',
      'confirm': 'Confirm',
      'appointmentSlotBooked': 'Appointment slot booked',
      'appointmentRescheduledSuccessfully':
          'Appointment rescheduled successfully',
      'payNow': 'Pay now',
      'paymentPendingBadge': 'PAYMENT PENDING',
      'paymentCouldNotStart': 'Could not start payment. Please try again.',
      'paymentCouldNotStartWithError': 'Payment could not be started: {{error}}',
      'paymentCompletedAppointmentConfirmed':
          'Payment completed. Appointment is confirmed.',
      'completePayment': 'Complete payment',
      'paymentPendingTitle': 'Payment pending',
      'paymentPendingMessage':
          'Your appointment is created and waiting for payment confirmation.',
      'currentPaymentStatus': 'Current payment status: {{status}}',
      'checking': 'Checking...',
      'checkPaymentStatus': 'Check payment status',
      'continuePayment': 'Continue payment',
      'backToBookings': 'Back to bookings',
      'couldNotRefreshPaymentStatus':
          'Could not refresh payment status. Please try again.',
      'paymentStillPendingConfirmBooking':
          'Payment is still pending. Complete payment to confirm this booking.',
      'joinVideoCall': 'Join Video Call',
      'viewVisitSummary': 'View Visit Summary',
      'leaveReview': 'Leave a Review',
      'yourRating': 'Your rating',
      'thankYouForYourRating': 'Thank you for your rating.',
      'stars': 'stars',
      'changeBooking': 'Change Booking',
      'cancelBooking': 'Cancel Booking',
      'cancelAppointment': 'Cancel Appointment',
      'areYouSureCancel': 'Are you sure you want to cancel this appointment?',
      'appointmentCancelledSuccessfully': 'Appointment cancelled successfully',
      'errorCancellingAppointment': 'Error cancelling appointment',
      'contactDoctor': 'Contact Doctor',
      'callDoctor': 'Call Doctor',
      'emailDoctor': 'Email Doctor',
      'appointmentLessThan48Hours': 'Appointment is less than 48 hours away',
      'contactDoctorDirectly':
          'To change this appointment, please contact the doctor directly.',

      // Doctors
      'doctors': 'Doctors',
      'myDoctors': 'My Doctors',
      'recommended': 'Recommended',
      'sortBy': 'Sort by',
      'sortByDistance': 'Distance',
      'sortByRating': 'Rating',
      'sortByReviews': 'Reviews',
      'filterBy': 'Filter',
      'filterByRegion': 'Region',
      'filterBySpecialty': 'Specialty',
      'allRegions': 'All regions',
      'allSpecialties': 'All specialties',
      'gettingYourLocation': 'Getting your location…',
      'usingCurrentLocation': 'Using current location for distance',
      'couldNotGetLocationUsingProfile':
          'Could not get location. Using profile address.',
      'bookAppointment': 'Book Appointment',
      'reviews': 'Reviews',
      'noReviews': 'No reviews yet',
      'writeReview': 'Write a Review',
      'rating': 'Rating',
      'comment': 'Comment',
      'commentOptional': 'Comment (Optional)',
      'submitReview': 'Submit Review',
      'shareExperience': 'Share your experience...',
      'howWasYourExperience': 'How was your experience?',
      'contactInformation': 'Contact Information',
      'specializations': 'Specializations',
      'furtherInformation': 'Further Information',
      'noRatingsYet': 'No ratings yet',
      'dentist': 'Dentist',
      'checkUp': 'Check Up',
      'cardiologist': 'Cardiologist',
      'generalpractitioner': 'General Practitioner',
      'therapist': 'Therapist',
      'pediatrician': 'Pediatrician',
      'dermatologist': 'Dermatologist',
      'ophthalmologist': 'Ophthalmologist',
      'neurologist': 'Neurologist',
      'surgeon': 'Surgeon',
      'gynecologist': 'Gynecologist',
      'urologist': 'Urologist',
      'psychiatrist': 'Psychiatrist',
      'orthopedist': 'Orthopedist',
      'pulmonologist': 'Pulmonologist',
      'endocrinologist': 'Endocrinologist',
      'gastroenterologist': 'Gastroenterologist',
      'otolaryngologist': 'ENT Specialist',
      'general practitioner': 'General Practitioner',
      'ent specialist': 'ENT Specialist',

      // Documents
      'documents': 'Documents',
      'searchDocuments': 'Search documents...',
      'upload': 'Upload',
      'uploadFailed': 'Upload failed',
      'documentUploadedSuccessfully': 'Document uploaded successfully',
      'noDocuments': 'No documents available',
      'noDocumentsFound': 'No documents found',
      'documentTitle': 'Document Title',
      'enterDocumentTitle': 'Enter document title',
      'openDocument': 'Open Document',
      'takePhoto': 'Take Photo',
      'chooseFromGallery': 'Choose from Gallery',
      'uploadFile': 'Upload File',
      'addedByDoctor': 'Added by doctor',
      'addedByYou': 'Added by you',
      // Document categories (optional tag)
      'documentCategoryLabel': 'Document type',
      'documentCategorySelect': 'Select a type (optional)',
      'documentCategoryHint':
          'Optional. Tagging helps your doctors quickly understand what kind of document this is.',
      'documentCategory_BLOOD_TEST': 'Blood test',
      'documentCategory_URINE_TEST': 'Urine test',
      'documentCategory_STOOL_TEST': 'Stool test',
      'documentCategory_LAB_RESULT': 'Lab result',
      'documentCategory_MRI': 'MRI',
      'documentCategory_CT_SCAN': 'CT scan',
      'documentCategory_XRAY': 'X-ray',
      'documentCategory_ULTRASOUND': 'Ultrasound',
      'documentCategory_MAMMOGRAPHY': 'Mammography',
      'documentCategory_ECG': 'ECG',
      'documentCategory_EEG': 'EEG',
      'documentCategory_ENDOSCOPY': 'Endoscopy',
      'documentCategory_BIOPSY': 'Biopsy',
      'documentCategory_PATHOLOGY': 'Pathology',
      'documentCategory_IMAGING_OTHER': 'Other imaging',
      'documentCategory_PRESCRIPTION': 'Prescription',
      'documentCategory_VACCINATION_RECORD': 'Vaccination record',
      'documentCategory_DISCHARGE_SUMMARY': 'Discharge summary',
      'documentCategory_REFERRAL': 'Referral',
      'documentCategory_HOSPITAL_REPORT': 'Hospital report',
      'documentCategory_ALLERGY_REPORT': 'Allergy report',
      'documentCategory_OTHER_MEDICAL': 'Other medical result',

      // App Lock
      'appLock': 'App Lock',
      'unlockApp': 'Unlock App',
      'unlockShifa': 'Unlock Shifa',
      'enableBiometricForAppSecurity':
          'Enable Face ID / Biometric for app security?',
      'enableBiometricPrompt':
          'Secure your medical data with biometric authentication',
      'skipBiometric': 'Skip',
      'lockAfterInactivity': 'Lock after inactivity',
      'enterPinOrUseBiometric':
          'Enter your PIN or use biometric authentication',
      'enterPinToUnlock': 'Enter your PIN to unlock',
      'useBiometric': 'Use Biometric',
      'enableAppLock': 'Enable App Lock',
      'appLockEnabled': 'App will lock when you close it',
      'appLockDisabled': 'App will not lock',
      'pinCode': 'PIN Code',
      'setUpPin': 'Set Up PIN',
      'setUpPinRequired': 'You need to set up a PIN code to enable app lock.',
      'setUp': 'Set Up',
      'setUpPinDescription': 'Create a PIN code to secure your app',
      'changePin': 'Change PIN',
      'changePinDescription': 'Update your PIN code',
      'clearPin': 'Clear PIN',
      'clearPinDescription': 'Remove PIN and disable app lock',
      'clearPinConfirmation':
          'Are you sure you want to clear your PIN? This will disable app lock.',
      'clear': 'Clear',
      'pinSetSuccessfully': 'PIN set successfully',
      'pinChangedSuccessfully': 'PIN changed successfully',
      'pinCleared': 'PIN cleared',
      'pinsDoNotMatch': 'PINs do not match',
      'enterPin': 'Enter PIN',
      'enterCurrentPin': 'Enter Current PIN',
      'enterNewPin': 'Enter New PIN',
      'confirmPin': 'Confirm PIN',
      'confirmNewPin': 'Confirm New PIN',
      'reEnterPin': 'Re-enter your PIN',
      'incorrectPin': 'Incorrect PIN',
      'pinLengthRequirement': 'PIN must be 4-6 digits',
      'tryAgainInSeconds': 'Try again in {{seconds}} seconds',
      'forgotPinLogOut': 'Forgot PIN? Log out',
      'biometricAuthentication': 'Biometric Authentication',
      'enableBiometric': 'Enable Biometric',
      'biometricEnabled': 'Use fingerprint or face ID to unlock',
      'biometricDisabled': 'Biometric authentication is disabled',
      'setUpPinFirst': 'Set up PIN first to enable biometric authentication',
      'enterPinCode': 'Enter your PIN code',
      'createAPinCode': 'Create a PIN code to secure your app',
      'biometricAuthenticationFailed': 'Biometric authentication failed',
      'authenticationError': 'Authentication error',

      // Chat
      'chat': 'Chat',
      'messages': 'Messages',
      'searchDoctors': 'Search doctors...',
      'typeMessage': 'Type a message...',
      'send': 'Send',
      'noConversations': 'No conversations yet',
      'isTyping': 'is typing',
      'selectConversation': 'Select a conversation',
      'attachFile': 'Attach file',
      'selectImage': 'Select Image',
      'recordVoice': 'Record Voice Message',
      'voiceMessage': 'Voice Message',
      'sendVoice': 'Send Voice',
      'compressingImage': 'Compressing image...',
      'uploadingFile': 'Uploading file...',
      'errorUploadingFile': 'Error uploading file',
      'errorRecordingVoice': 'Error recording voice',
      'selectDocument': 'Select Document',

      // Notifications
      'notifications': 'Notifications',
      'newAppointmentScheduled': 'New appointment scheduled',
      'newAppointmentScheduledTitle': 'New Appointment Scheduled',
      'appointmentCancelledTitle': 'Appointment Cancelled',
      'appointmentReminderTitle': 'Appointment Reminder',
      'appointmentChangedTitle': 'Appointment Changed',
      'notificationMessageScheduled':
          'Doctor has scheduled an appointment for you. Check the details in your appointments.',
      'notificationMessageCancelled':
          'Your appointment has been cancelled. Please make another appointment if needed.',
      'notificationMessageReminder':
          'You have an upcoming appointment. Please be on time.',
      'notificationMessageChanged':
          'Your appointment has been rescheduled. Check your appointments for the new date and time.',
      'doctorHasScheduled': 'Doctor has scheduled an appointment for you',
      'noNotifications': 'No notifications',
      'notificationCannotOpen':
          'This notification could not be opened. Missing information.',
      'signatureRequestedTitle': 'Signature Requested',
      'signatureRequestedMessage':
          '{doctorName} is requesting your signature for the appointment summary.',
      'visitSummaryReadyTitle': 'Visit summary is ready',
      'visitSummaryReadyMessage': 'Your after-visit summary is now available.',
      'chatNewMessageTitle': 'New message',
      'taskReminderTitle': 'Task reminder',
      'taskAssignedTitle': 'Task assigned',
      'taskCancelledTitle': 'Task cancelled',

      // Status
      'confirmed': 'Confirmed',
      'cancelled': 'Cancelled',
      'completed': 'Completed',
      'pending': 'Pending',

      // Video Call
      'videoCall': 'Video Call',
      'callDuration': 'Call Duration',

      // Account Info
      'accountInformation': 'Account Information',
      'dateOfBirth': 'Date of Birth',

      // Profile Image
      'profileImage': 'Profile Image',
      'addProfilePhoto': 'Add Profile Photo (Optional)',
      'addPhotoLater': 'You can add a photo later from your profile settings',
      'removePhoto': 'Remove Photo',
      'completeRegistration': 'Complete Registration',

      // Registration
      'step1': 'Step 1',
      'step2': 'Step 2',
      'step3': 'Step 3',
      'accountCreated': 'Account created successfully!',
      'registrationFailed': 'Registration failed',

      // Errors
      'required': 'Required',
      'passwordsDoNotMatch': 'Passwords do not match',
      'invalidEmail': 'Please enter a valid email address',
      'passwordTooShort': 'Password must be at least 8 characters',
      'passwordTooLong': 'Password must be at most 128 characters',
      'minimum6Characters': 'Minimum 6 characters',
      'passwordRequirementMinLength': 'At least 8 characters',
      'passwordRequirementMaxLength': 'At most 128 characters',
      'passwordRequirementUppercase': 'One uppercase letter',
      'passwordRequirementLowercase': 'One lowercase letter',
      'passwordRequirementDigit': 'One number',
      'passwordRequirementSpecialChar':
          'One special character (!@#\$%^&* etc.)',
      'invalidPhone': 'Please enter a valid phone number',
      'resetPassword': 'Reset Password',
      'changePassword': 'Change Password',
      'currentPassword': 'Current Password',
      'mustChangePassword': 'You must change your password on first login.',
      'newPassword': 'New Password',
      'savePassword': 'Save & Continue',
      'passwordChangedSuccess': 'Password changed successfully.',
      'uploading': 'Uploading photo...',
      'photoUploadEndpointNotAvailable':
          'Photo upload endpoint not available. Please use Edit Profile to update photo.',
      'profilePhotoUpdatedSuccessfully': 'Profile photo updated successfully',
      'failedToGetPhotoUrl': 'Failed to get photo URL from server',
      'failedToUploadPhoto': 'Failed to upload photo',
      'failedToUpdateProfile': 'Failed to update profile',
      'deleteAccountConfirmation':
          'Are you sure you want to delete your account? This action cannot be undone.',
      'deleteAccountComingSoon': 'Delete account functionality coming soon',
      'couldNotReadFileBytes': 'Could not read file bytes',
      'pleaseSelectCheckIn': 'Please select a check-in',
      'checkInSubmittedSuccessfully': 'Check-in submitted successfully',
      'failedToSubmit': 'Failed to submit',
      'taskCheckIn': 'Task Check-in',
      'taskNotFound': 'Task not found',
      'noPendingCheckIns': 'No pending check-ins',
      'taskCheckInNotYetAvailable':
          'No check-ins due yet. You can submit from 10 minutes before the scheduled time.',
      'exampleValue': 'e.g., 120/80',
      'additionalNotesOptional': 'Additional notes (optional)',
      'submitCheckIn': 'Submit Check-in',
      'markAllAsRead': 'Mark all as read',
      'allNotificationsMarkedAsRead': 'All notifications marked as read',
      'errorLoadingNotifications': 'Error loading notifications',
      'approve': 'Approve',
      'reject': 'Reject',
      'documentAccessApproved': 'Access granted',
      'documentAccessRejected': 'Access request rejected',
      'today': 'Today',
      'timeYesterday': 'Yesterday {time}',
      'notificationFilterAll': 'All',
      'notificationFilterAppointments': 'Appointments',
      'notificationFilterDocuments': 'Documents',
      'notificationFilterTasks': 'Tasks',
      'notificationSettings': 'Settings',
      'notificationEmptyFilter': 'No notifications in this category',
      'notificationEmptyFilterHint': 'Try another filter or check back later.',
      'notificationEmptyBody':
          'You\'ll see appointment updates, documents and tasks here.',
      'noName': 'No Name',
      'navigationError': 'Navigation Error',
      'locationLabel': 'Location',
      'goToSplash': 'Go to Splash',
      'failedToSend': 'Failed to send',
      'chatImage': 'Chat Image',
      'failedToUploadImage': 'Failed to upload image',
      'slideUpToCancel': 'Slide up to cancel',
      'slideLeftToCancel': 'Slide left to cancel',
      'seconds': 'seconds',
      'sec': 'sec',
      'failedToStartChat': 'Failed to start chat',
      'noDoctorsFound': 'No doctors found',
      'imageUploadComingSoon':
          'Image upload functionality coming soon. Please use image URL for now.',
      'couldNotOpenMapApplication': 'Could not open map application',
      'goBack': 'Go Back',
      'certificate': 'Certificate',
      'openInMaps': 'Open in Maps',
      'appointmentNotFound': 'Appointment not found',
      'errorOpeningDocument': 'Error opening document',
      'noMessages': 'No messages',
      'refresh': 'Refresh',
      'waitingForDoctor': 'Waiting for Doctor',
      'join': 'Join',
      'view': 'View',
      'clinicAddress': 'Clinic Address',
      'documentAccessRequestTitle': 'Document access request',
      'documentAccessRequestMessage':
          '{requesterName} requested access to {fileName} for {patientName}.',
      'taskCategoryVital': 'Vital',
      'taskCategoryExercise': 'Exercise',
      'taskCategoryMedication': 'Medication',
      'taskCategoryOther': 'Other',
      'photo': 'Photo',
      'information': 'Information',
      'pleaseCompleteAllRequiredFields': 'Please complete all required fields',
      'cannotMakePhoneCall': 'Cannot make phone call to',
      'cannotSendEmail': 'Cannot send email to',
      'errorParsingAppointmentData': 'Error parsing appointment data',
      'unknownDoctor': 'Unknown Doctor',
      'failedToStartVideoCall': 'Failed to start video call',
      'videoCallConnectionTimeout':
          'Connection timed out. Please check your internet and try again.',
      'videoCallEnded': 'Video call ended',
      'callErrorOccurred': 'Call error occurred',
      'waitingForParticipants': 'Waiting for participants...',
      'failedToLoadDoctor': 'Failed to load doctor',
      'newAppointmentBookedButFailedToCancelOld':
          'New appointment booked, but failed to cancel old one',
      'failedToBookAppointment': 'Failed to book appointment',
      'enterYourAddress': 'Enter your address',
      'date': 'Date',
      'passwordRequired': 'Password is required',
      'pleaseConfirmPassword': 'Please confirm your password',
      'phoneNumberRequired': 'Phone number is required',
      'addProfilePhotoOptional': 'Add Profile Photo (Optional)',
      'youCanAddPhotoLater':
          'You can add a photo later from your profile settings',
      'emailOptional': 'Email (Optional)',
      'noBiographyAvailable': 'No biography available',
      'noServicesAvailable': 'No services available',
      'noCertificatesAvailable': 'No certificates available',
      'noContactInformationAvailable': 'No contact information available',
      'errorLoadingReviews': 'Error loading reviews',
      'justNow': 'Just now',
      'yesterday': 'Yesterday',
      'minutesAgo': '%s minutes ago',
      'minuteAgo': '1 minute ago',
      'hoursAgo': '%s hours ago',
      'hourAgo': '1 hour ago',
      'daysAgo': '%s days ago',
      'dayAgo': '1 day ago',
      'starts': 'Starts',
      'started': 'Started',
      'document': 'Document',
      'videoCallReady': 'Video call ready',
      'videoCallYouCanJoinNow': 'You can join your video call now.',
      'clickBelowToJoinCall': 'Click below to join the call',
      'signAppointmentSummary': 'Sign Appointment Summary',
      'visitSummaryTitle': 'Visit Summary',
      'visitSummaryPreparing': 'Your visit summary is being prepared.',
      'visitSummaryWhatHappened': 'What happened today',
      'visitSummaryCarePlan': 'Your care plan',
      'visitSummaryMedications': 'Medications',
      'visitSummaryMissedDose': 'Missed dose',
      'visitSummaryRedFlags': 'Warning signs',
      'visitSummaryNextSteps': 'Next steps',
      'visitSummaryAskTitle': 'Ask about this summary',
      'visitSummaryAskHint': 'Type your question',
      'visitSummaryChecklistReminderTitle': 'Care plan reminder',
      'visitSummaryReminderCreated': 'Reminder created',
      'visitSummarySources': 'Sources',
      'remindMe': 'Remind me',
      'visitSummaryQuickReminderTitle': 'Set reminder',
      'visitSummaryQuick15Min': 'In 15 minutes',
      'visitSummaryTonight': 'Tonight (20:00)',
      'visitSummaryTomorrowMorning': 'Tomorrow morning (09:00)',
      'visitSummaryCustom': 'Custom date & time',
      'appointmentSummaryPreview': 'Appointment Summary',
      'confirmAppointmentSummaryReflectsDiscussion':
          'I confirm that this appointment summary reflects our discussion.',
      'yourSignature': 'Your signature',
      'pleaseSignAbove': 'Please sign in the box above.',
      'signatureSubmittedSuccess': 'Signature submitted successfully.',
      'time': 'Time',
      'reason': 'Reason',
      'errorSaving': 'Error saving',
      'signatureAlreadySubmitted': 'Signature already submitted',
      'appInitializationError': 'App Initialization Error',
      'pleaseRestartApp': 'Please restart the app',
      'notificationChannelName': 'Shifa Patient Notifications',
      'notificationChannelDescription':
          'Notifications for messages, appointments, and tasks',
      'notificationChannelAppointmentsName': 'Appointment Notifications',
      'notificationChannelAppointmentsDescription':
          'Notifications for appointment updates',
      // Backend / API errors (Auth, Security, Video, fallbacks)
      'errorNoAccountFound': 'No account found',
      'errorNoDoctorAccountFound': 'No doctor account found',
      'errorNoPatientAccountFound': 'No patient account found',
      'errorNoAdminAccountFound': 'No admin account found',
      'errorDoctorProfileNotFound': 'Doctor profile not found',
      'errorEmailOtpRequiredWhenEmailProvided':
          'Email OTP required when email is provided',
      'errorEmailVerificationCodeRequired': 'Email verification code required',
      'errorInvalidOrExpiredEmailVerificationCode':
          'Invalid or expired email verification code',
      'errorInvalidPhoneNumber': 'Invalid phone number',
      'errorAccountIsDisabled': 'Account is disabled',
      'errorNotADoctorAccount': 'Not a doctor account',
      'errorMissingBearerToken': 'Missing Bearer token',
      'errorFirebaseVerificationNotConfigured':
          'Firebase verification not configured',
      'errorInvalidOrExpiredToken': 'Invalid or expired token',
      'errorAccessRestrictedToDoctors': 'Access restricted to doctors',
      'errorYourAccountHasBeenBlocked': 'Your account has been blocked',
      'errorInvalidKey': 'Invalid key',
      'errorKeyAlreadyUsed': 'Key already used',
      'errorUsernameRequired': 'Username is required',
      'errorPasswordRequired': 'Password is required',
      'errorInvalidCredentials':
          'No account found with this phone number or email. Check your entry or create an account.',
      'errorAccountLocked': 'Account is locked',
      'errorInvalidCredentialsPasswordMismatch':
          'Incorrect password. Please try again or reset your password.',
      'errorCreatePatientAccountFirst':
          'This is a doctor account. To sign in here, tap "Create Account" to set up a patient account linked to your doctor login.',
      'errorAccessDeniedThisAppRequiresRole':
          'Access denied: This app requires \${requiredRole.name} role',
      'errorPhoneVerificationNotConfigured':
          'Phone verification not configured',
      'errorInvalidOrExpiredPhoneVerification':
          'Invalid or expired phone verification',
      'errorPhoneNumberNotFoundInVerification':
          'Phone number not found in verification',
      'errorInvalidPhoneInVerification': 'Invalid phone in verification',
      'errorPhoneNumberDoesNotMatchVerification':
          'Phone number does not match verification',
      'errorEmailAlreadyRegistered': 'Email already registered',
      'errorPhoneAlreadyRegistered': 'Phone already registered',
      'errorPatientWithPhoneAlreadyExists':
          'Patient with this phone number already exists',
      'errorPhoneNumberNotFound': 'Phone number not found',
      'errorUserNotFound': 'User not found',
      'errorSessionInvalid': 'Session invalid',
      'errorSessionExpiredOrSignedOut': 'Session expired or signed out',
      'errorInvalidToken': 'Invalid token',
      'errorTooManyRequests': 'Too many requests. Please try again later.',
      'errorPatientProfileNotFoundForUser':
          'Patient profile not found for user \${user.id}. Please fill your profile with phone or email.',
      'errorAuthenticationRequired': 'Authentication required',
      'errorAppointmentNotFound': 'Appointment not found: {{id}}',
      'errorVideoCallNotYetAvailable':
          'Video call is not yet available. You can join 5 minutes before the appointment start.',
      'errorVideoCallHasEnded':
          'Video call has ended. The join window closes 15 minutes after the appointment end.',
      'errorAppointmentDoesNotBelongToDoctor':
          'Appointment does not belong to this doctor',
      'errorAppointmentDoesNotHavePatientAssigned':
          'Appointment does not have a patient assigned. Please ensure the appointment is linked to your profile.',
      'errorAppointmentDoesNotBelongToPatient':
          'Appointment does not belong to this patient',
      'errorFailedToRetrievePatientProfile':
          'Failed to retrieve patient profile: {{detail}}',
      'errorFailedToGetOrCreateRoom':
          'Failed to get or create room: {{detail}}',
      'errorUnableToDetermineUserIdentity': 'Unable to determine user identity',
      'errorUnableToDetermineUserName': 'Unable to determine user name',
      'errorUserNameCannotBeBlank': 'User name cannot be blank',
      'errorFailedToGenerateToken': 'Failed to generate token',
      'errorFailedToGenerateTokenDetail':
          'Failed to generate token: {{detail}}',
      'errorFailedToGenerateVideoToken':
          'Failed to generate video token: {{detail}}',
      'errorDailyApiKeyNotConfigured':
          'Daily.co API key is not configured. Please set DAILY_API_KEY environment variable in Railway.',
      'errorNoTokenReceivedFromServer': 'No token received from server',
      'errorNoTokenReceived': 'No token received',
      'errorInvalidResponseFromServer':
          'Invalid response from server: {{code}}',
      'errorLoginFailed': 'Login failed',
      'errorNetworkError': 'Network error: {{type}}',
      'errorFailedToSendEmailCode': 'Failed to send email code',
      'errorFailedToCreatePatientAccount': 'Failed to create patient account',
      'errorRegistrationFailed': 'Registration failed',
      'errorFailedToResetPassword': 'Failed to reset password',
      'errorFailedToChangePassword': 'Failed to change password',
      'errorUnknownError': 'Unknown error',
      'errorStatusCode': 'Error {{code}}',
      'errorSomethingWentWrong': 'Something went wrong',
      'errorSessionExpiredPleaseStartAgain':
          'Session expired. Please start again.',
    },
    'de': {
      // Common
      'appName': 'Shifa Patient',
      'hello': 'Hallo',
      'patient': 'Patient',
      'doctor': 'Arzt',
      'loading': 'Lädt...',
      'error': 'Fehler',
      'retry': 'Wiederholen',
      'cancel': 'Abbrechen',
      'save': 'Speichern',
      'delete': 'Löschen',
      'deleteDocumentConfirmation':
          'Möchten Sie dieses Dokument wirklich löschen?',
      'documentDeleted': 'Dokument gelöscht',
      'edit': 'Bearbeiten',
      'back': 'Zurück',
      'next': 'Weiter',
      'complete': 'Fertig',
      'submit': 'Absenden',
      'close': 'Schließen',
      'yes': 'Ja',
      'no': 'Nein',

      // Auth
      'login': 'Anmeldung',
      'signIn': 'Anmelden',
      'createAccount': 'Konto erstellen',
      'existingDoctorTitle': 'Bestehendes Arztkonto gefunden',
      'existingDoctorMessage':
          'Sie haben bereits ein Arztkonto für {{name}}. Bestätigen Sie unten, um Ihr Patientenkonto zu erstellen.',
      'createPatientAccount': 'Patientenkonto erstellen',
      'phoneOrEmail': 'Telefonnummer oder E-Mail',
      'password': 'Passwort',
      'confirmPassword': 'Passwort bestätigen',
      'continueWithGoogle': 'Mit Google fortfahren',
      'forgotPassword': 'Passwort vergessen?',
      'loginAttemptsRemaining':
          'Noch {{count}} Anmeldeversuch(e) vor zeitweiliger Sperre.',
      'accountLockedTryAgainIn':
          'Zu viele Fehlversuche. Konto vorübergehend gesperrt. Bitte in {{minutes}} Minute(n) erneut versuchen.',
      'verifyAndCreate': 'Bestätigen und Konto erstellen',
      'invalidVerificationCode': 'Ungültiger oder abgelaufener Code',
      'noAccountWithPhone': 'Kein Konto mit dieser Telefonnummer gefunden.',
      'resendCode': 'Code erneut senden',
      'resendCodeIn': 'Code erneut senden in {{time}}',
      'codeSentAgain': 'Bestätigungscode erneut gesendet',
      'codeExpiresIn': 'Code läuft ab in {{time}}',
      'forgotPasswordEnterEmail':
          'Geben Sie Ihre E-Mail-Adresse ein. Wir senden Ihnen einen Bestätigungscode zum Zurücksetzen des Passworts.',
      'otpSentToEmail': 'Ein 6-stelliger Code wurde an {email} gesendet. Prüfen Sie Ihren Posteingang.',
      'emailRequired': 'E-Mail-Adresse ist erforderlich',

      // Profile
      'profile': 'Profil',
      'editProfile': 'Profil bearbeiten',
      'language': 'Sprache',
      'preferences': 'Einstellungen',
      'settingsTitle': 'Einstellungen',
      'accountTitle': 'Konto',
      'privacy': 'Datenschutz',
      'deleteAccount': 'Mein Konto löschen',
      'deleteAccountWarning':
          'Diese Aktion ist irreversibel. Ihre persönlichen Identifikatoren (Telefon/E-Mail/Name) werden entfernt. Einige medizinische Daten können gesetzlich vorgeschrieben gespeichert bleiben, sind aber von einem neuen Konto aus nicht zugänglich.',
      'deleteAccountVerifyTitle': 'Zum Löschen verifizieren',
      'deleteAccountOtpSubtitle':
          'Geben Sie den 6-stelligen Code ein, der an Ihre E-Mail gesendet wurde.',
      'verificationCode': 'Bestätigungscode',
      'confirmDeletion': 'Löschen bestätigen',
      'accountDeletedSuccess': 'Ihr Konto wurde erfolgreich gelöscht',
      'logOut': 'Abmelden',
      'signOutConfirm': 'Möchten Sie sich wirklich abmelden?',
      'birthDate': 'Geburtsdatum',
      'phoneNumber': 'Telefonnummer',
      'email': 'E-Mail',
      'address': 'Adresse',
      'name': 'Vorname',
      'surname': 'Nachname',
      'gender': 'Geschlecht',
      'male': 'Männlich',
      'female': 'Weiblich',
      'other': 'Andere',
      'uploadPhoto': 'Foto hochladen',
      'selectLanguage': 'Sprache auswählen',
      'languageChanged': 'Sprache geändert zu',

      // Home
      'upcomingAppointments': 'Bevorstehende Termine',
      'doctorsNearMe': 'Ärzte in meiner Nähe',
      'noUpcomingAppointments': 'Keine bevorstehenden Termine',
      'noDoctorsAvailable': 'Keine Ärzte verfügbar',
      'noDoctorsInCloseDistance': 'Keine Ärzte in der Nähe',
      'viewTasks': 'Aufgaben anzeigen',
      'useMyLocation': 'Meinen Standort verwenden',

      // Shifa AI (co-pilot)
      'shifaAiTitle': 'Shifa AI',
      'shifaAiCardSubtitle':
          'Ihr Co-Pilot — Fragen, Symptome, Ärzte finden',
      'shifaAiDisclaimer':
          'Shifa AI bietet nur allgemeine Informationen. Es ersetzt keine professionelle medizinische Beratung, Diagnose oder Behandlung.',
      'copilotInputHint': 'Nachricht eingeben…',
      'copilotSuggestDoctors': 'Ärzte vorschlagen',
      'copilotSuggestedDoctors': 'Vorgeschlagene Ärzte',
      'copilotNoSuggestedDoctors':
          'Keine passenden Ärzte gefunden. Nutzen Sie die Arztsuche.',
      'copilotBookWithDoctor': 'Buchen',
      'copilotViewProfile': 'Profil',
      'copilotTranscribeError': 'Sprache konnte nicht transkribiert werden',
      'copilotContinueToBooking':
          'Auf der nächsten Seite wählen Sie Datum und Uhrzeit. Fortfahren?',
      'copilotBookingTitle': 'Termin buchen',
      'copilotBookManual': 'Slot wählen',
      'copilotAutoBook': 'Auto buchen',
      'copilotAutoBookExplainer':
          'Shifa bucht den nächstgelegenen freien Termin zu Ihrem Wunschdatum und Ihrer Wunschzeit. Bitte stimmen Sie unten zu.',
      'copilotPreferredDate': 'Wunschdatum',
      'copilotPreferredTime': 'Wunschzeit',
      'copilotConsentAutoBook': 'Ich erlaube Shifa, diesen Termin in meinem Namen zu buchen',
      'copilotAutoBookSubmit': 'Auto-Buchung bestätigen',
      'copilotBookedSuccess': 'Termin gebucht. Details unter Termine.',
      'copilotBookedViaAiReason': 'Gebucht über Shifa AI',
      'copilotConfirmBookFromChatTitle': 'Buchung aus dem Chat bestätigen',
      'copilotConfirmBookFromChatExplainer':
          'Shifa bucht den nächstgelegenen freien Termin zu der besprochenen Zeit. Dies nutzt Ihre vorherige Zustimmung in diesem Gespräch.',
      'copilotNoProviderOnPlatform':
          'Shifa listet derzeit keinen Arzt auf der Plattform, der zu diesen Symptomen passt. Nutzen Sie die Arztsuche oder versuchen Sie es später erneut. Bei starker Verschlechterung suchen Sie bitte ärztliche Notfallversorgung auf.',
      'copilotNextSlot': 'Nächster Termin:',
      'copilotConfidence': 'Vertrauen:',
      'copilotConfidenceHigh': 'Hoch',
      'copilotConfidenceMedium': 'Mittel',
      'copilotConfidenceLow': 'Niedrig',
      'copilotThinking': 'Denke nach…',

      'locationPermissionDenied': 'Standortberechtigung verweigert.',
      'locationPermissionDeniedForever':
          'Standortberechtigung dauerhaft verweigert. Bitte in den Einstellungen aktivieren.',
      'microphonePermissionDenied':
          'Mikrofonzugriff wird für Sprachnachrichten benötigt.',
      'cameraPermissionDenied': 'Kamerazugriff wird für Fotos benötigt.',
      'permissionNeeded': 'Berechtigung erforderlich',
      'permissionRationaleCamera':
          'Shifa benötigt Kamerazugriff für Profilfotos, Dokumente und Chat.',
      'permissionRationaleMicrophone':
          'Shifa benötigt Mikrofonzugriff für Sprachnachrichten und Videoanrufe mit Ihrem Arzt.',
      'permissionRationaleLocation':
          'Shifa benötigt Standortzugriff, damit Sie Ihre Adresse auf der Karte auswählen können.',
      'permissionRationaleNotifications':
          'Shifa benötigt Benachrichtigungsberechtigung für Termine, Nachrichten und Dokumente.',

      // Bookings
      'bookings': 'Termine',
      'upcoming': 'Bevorstehend',
      'past': 'Vergangen',
      'createBooking': 'Termin erstellen',
      'noAppointmentsFound': 'Keine Termine gefunden',
      'appointmentDetails': 'Termindetails',
      'dateAndTime': 'Datum und Uhrzeit',
      'location': 'Standort',
      'reasonForVisit': 'Grund für den Besuch',
      'joinVideoCall': 'Videoanruf beitreten',
      'viewVisitSummary': 'Besuchsübersicht anzeigen',
      'leaveReview': 'Bewertung abgeben',
      'yourRating': 'Ihre Bewertung',
      'thankYouForYourRating': 'Vielen Dank für Ihre Bewertung.',
      'stars': 'Sterne',
      'changeBooking': 'Termin ändern',
      'cancelBooking': 'Termin stornieren',
      'cancelAppointment': 'Termin stornieren',
      'areYouSureCancel':
          'Sind Sie sicher, dass Sie diesen Termin stornieren möchten?',
      'appointmentCancelledSuccessfully': 'Termin erfolgreich storniert',
      'appointmentRescheduledSuccessfully': 'Termin erfolgreich verschoben',
      'payNow': 'Jetzt bezahlen',
      'paymentPendingBadge': 'ZAHLUNG AUSSTEHEND',
      'paymentCouldNotStart':
          'Zahlung konnte nicht gestartet werden. Bitte versuchen Sie es erneut.',
      'paymentCouldNotStartWithError':
          'Zahlung konnte nicht gestartet werden: {{error}}',
      'paymentCompletedAppointmentConfirmed':
          'Zahlung abgeschlossen. Termin ist bestätigt.',
      'completePayment': 'Zahlung abschließen',
      'paymentPendingTitle': 'Zahlung ausstehend',
      'paymentPendingMessage':
          'Ihr Termin wurde erstellt und wartet auf Zahlungsbestätigung.',
      'currentPaymentStatus': 'Aktueller Zahlungsstatus: {{status}}',
      'checking': 'Prüfe...',
      'checkPaymentStatus': 'Zahlungsstatus prüfen',
      'continuePayment': 'Zahlung fortsetzen',
      'backToBookings': 'Zurück zu Terminen',
      'couldNotRefreshPaymentStatus':
          'Zahlungsstatus konnte nicht aktualisiert werden. Bitte erneut versuchen.',
      'paymentStillPendingConfirmBooking':
          'Die Zahlung ist noch ausstehend. Schließen Sie die Zahlung ab, um diese Buchung zu bestätigen.',
      'errorCancellingAppointment': 'Fehler beim Stornieren des Termins',
      'contactDoctor': 'Arzt kontaktieren',
      'callDoctor': 'Arzt anrufen',
      'emailDoctor': 'Arzt E-Mail senden',
      'appointmentLessThan48Hours':
          'Termin ist weniger als 48 Stunden entfernt',
      'contactDoctorDirectly':
          'Um diesen Termin zu ändern, kontaktieren Sie bitte den Arzt direkt.',

      // Doctors
      'doctors': 'Ärzte',
      'myDoctors': 'Meine Ärzte',
      'recommended': 'Empfohlen',
      'bookAppointment': 'Termin buchen',
      'reviews': 'Bewertungen',
      'noReviews': 'Noch keine Bewertungen',
      'writeReview': 'Bewertung schreiben',
      'rating': 'Bewertung',
      'comment': 'Kommentar',
      'commentOptional': 'Kommentar (optional)',
      'submitReview': 'Bewertung absenden',
      'shareExperience': 'Teilen Sie Ihre Erfahrung...',
      'howWasYourExperience': 'Wie war Ihre Erfahrung?',
      'contactInformation': 'Kontaktinformationen',
      'specializations': 'Spezialisierungen',
      'furtherInformation': 'Weitere Informationen',
      'noRatingsYet': 'Noch keine Bewertungen',

      // Documents
      'documents': 'Dokumente',
      'searchDocuments': 'Dokumente suchen...',
      'upload': 'Hochladen',
      'uploadFailed': 'Hochladen fehlgeschlagen',
      'documentUploadedSuccessfully': 'Dokument erfolgreich hochgeladen',
      'noDocuments': 'Keine Dokumente verfügbar',
      'noDocumentsFound': 'Keine Dokumente gefunden',
      'documentTitle': 'Dokumenttitel',
      'enterDocumentTitle': 'Dokumenttitel eingeben',
      'openDocument': 'Dokument öffnen',
      'addedByDoctor': 'Vom Arzt hinzugefügt',
      'addedByYou': 'Von Ihnen hinzugefügt',

      // Chat
      'chat': 'Chat',
      'messages': 'Nachrichten',
      'searchDoctors': 'Ärzte suchen...',
      'typeMessage': 'Nachricht eingeben...',
      'send': 'Senden',
      'noConversations': 'Noch keine Unterhaltungen',

      // Status
      'confirmed': 'Bestätigt',
      'cancelled': 'Storniert',
      'completed': 'Abgeschlossen',
      'pending': 'Ausstehend',

      // Video Call
      'videoCall': 'Videoanruf',
      'videoCallYouCanJoinNow': 'Sie können jetzt Ihrem Videoanruf beitreten.',
      'callDuration': 'Anrufdauer',

      // Account Info
      'accountInformation': 'Kontoinformationen',
      'dateOfBirth': 'Geburtsdatum',

      // Profile Image
      'profileImage': 'Profilbild',
      'addProfilePhoto': 'Profilfoto hinzufügen (Optional)',
      'addPhotoLater':
          'Sie können später ein Foto über Ihre Profileinstellungen hinzufügen',
      'removePhoto': 'Foto entfernen',
      'completeRegistration': 'Registrierung abschließen',

      // Registration
      'step1': 'Schritt 1',
      'step2': 'Schritt 2',
      'step3': 'Schritt 3',
      'accountCreated': 'Konto erfolgreich erstellt!',
      'registrationFailed': 'Registrierung fehlgeschlagen',

      // Errors
      'required': 'Erforderlich',
      'passwordsDoNotMatch': 'Passwörter stimmen nicht überein',
      'invalidEmail': 'Bitte geben Sie eine gültige E-Mail-Adresse ein',
      'passwordTooShort': 'Passwort muss mindestens 6 Zeichen lang sein',
      'minimum6Characters': 'Mindestens 6 Zeichen',
      'invalidPhone': 'Bitte geben Sie eine gültige Telefonnummer ein',
      'failedToUpdateProfile': 'Profil konnte nicht aktualisiert werden',
      'signatureAlreadySubmitted': 'Unterschrift bereits übermittelt',
      'appInitializationError': 'App-Initialisierungsfehler',
      'pleaseRestartApp': 'Bitte starten Sie die App neu',
      'notificationChannelName': 'Shifa Patient Benachrichtigungen',
      'notificationChannelDescription':
          'Benachrichtigungen für Nachrichten, Termine und Aufgaben',
      'notificationChannelAppointmentsName': 'Termin-Benachrichtigungen',
      'notificationChannelAppointmentsDescription':
          'Benachrichtigungen zu Terminänderungen',
      // Notifications (missing before; add to avoid EN fallback)
      'notifications': 'Benachrichtigungen',
      'noNotifications': 'Keine Benachrichtigungen',
      'markAllAsRead': 'Alle als gelesen markieren',
      'allNotificationsMarkedAsRead':
          'Alle Benachrichtigungen als gelesen markiert',
      'errorLoadingNotifications': 'Fehler beim Laden der Benachrichtigungen',
      'today': 'Heute',
      'yesterday': 'Gestern',
      'timeYesterday': 'Gestern {time}',
      'notificationFilterAll': 'Alle',
      'notificationFilterAppointments': 'Termine',
      'notificationFilterDocuments': 'Dokumente',
      'notificationFilterTasks': 'Aufgaben',
      'notificationSettings': 'Einstellungen',
      'notificationEmptyFilter': 'Keine Benachrichtigungen in dieser Kategorie',
      'notificationEmptyFilterHint':
          'Versuchen Sie einen anderen Filter oder schauen Sie später noch einmal vorbei.',
      'notificationEmptyBody':
          'Hier sehen Sie Updates zu Terminen, Dokumenten und Aufgaben.',
      'visitSummaryTitle': 'Besuchsübersicht',
      'visitSummaryPreparing': 'Ihre Besuchszusammenfassung wird vorbereitet.',
      'visitSummaryWhatHappened': 'Was heute passiert ist',
      'visitSummaryCarePlan': 'Ihr Behandlungsplan',
      'visitSummaryMedications': 'Medikamente',
      'visitSummaryMissedDose': 'Vergessene Dosis',
      'visitSummaryRedFlags': 'Warnzeichen',
      'visitSummaryNextSteps': 'Nächste Schritte',
      'visitSummaryAskTitle': 'Frage zur Zusammenfassung',
      'visitSummaryAskHint': 'Geben Sie Ihre Frage ein',
      'visitSummaryChecklistReminderTitle': 'Erinnerung an Behandlungsplan',
      'visitSummaryReminderCreated': 'Erinnerung erstellt',
      'visitSummarySources': 'Quellen',
      'remindMe': 'Erinnere mich',
      'visitSummaryQuickReminderTitle': 'Erinnerung festlegen',
      'visitSummaryQuick15Min': 'In 15 Minuten',
      'visitSummaryTonight': 'Heute Abend (20:00)',
      'visitSummaryTomorrowMorning': 'Morgen fruh (09:00)',
      'visitSummaryCustom': 'Eigenes Datum und Uhrzeit',
      'visitSummaryReadyTitle': 'Besuchszusammenfassung ist bereit',
      'visitSummaryReadyMessage':
          'Ihre Nachbesuchs-Zusammenfassung ist jetzt verfügbar.',
      'taskReminderTitle': 'Aufgaben-Erinnerung',
      'taskAssignedTitle': 'Aufgabe zugewiesen',
      'taskCancelledTitle': 'Aufgabe storniert',

      // Backend / API errors (Auth, Security, Video, fallbacks)
      'errorNoAccountFound': 'Konto nicht gefunden',
      'errorNoDoctorAccountFound': 'Arztkonto nicht gefunden',
      'errorNoPatientAccountFound': 'Patientenkonto nicht gefunden',
      'errorNoAdminAccountFound': 'Admin-Konto nicht gefunden',
      'errorDoctorProfileNotFound': 'Arztprofil nicht gefunden',
      'errorEmailOtpRequiredWhenEmailProvided':
          'Bei Angabe einer E-Mail ist ein Bestätigungscode (OTP) erforderlich',
      'errorEmailVerificationCodeRequired':
          'E-Mail-Bestätigungscode erforderlich',
      'errorInvalidOrExpiredEmailVerificationCode':
          'Ungültiger oder abgelaufener E-Mail-Bestätigungscode',
      'errorInvalidPhoneNumber': 'Ungültige Telefonnummer',
      'errorAccountIsDisabled': 'Konto ist deaktiviert',
      'errorNotADoctorAccount': 'Kein Arztkonto',
      'errorMissingBearerToken': 'Bearer-Token fehlt',
      'errorFirebaseVerificationNotConfigured':
          'Firebase-Verifizierung nicht konfiguriert',
      'errorInvalidOrExpiredToken':
          'Ungültiges oder abgelaufenes Token',
      'errorAccessRestrictedToDoctors':
          'Zugriff ist Ärzten vorbehalten',
      'errorYourAccountHasBeenBlocked': 'Ihr Konto wurde gesperrt',
      'errorInvalidKey': 'Ungültiger Schlüssel',
      'errorKeyAlreadyUsed': 'Schlüssel bereits verwendet',
      'errorUsernameRequired': 'Benutzername ist erforderlich',
      'errorPasswordRequired': 'Passwort ist erforderlich',
      'errorInvalidCredentials':
          'Kein Konto mit dieser Telefonnummer oder E-Mail gefunden. Prüfen Sie Ihre Eingabe oder erstellen Sie ein Konto.',
      'errorAccountLocked': 'Konto ist gesperrt',
      'errorInvalidCredentialsPasswordMismatch':
          'Falsches Passwort. Bitte erneut versuchen oder Passwort zurücksetzen.',
      'errorCreatePatientAccountFirst':
          'Dies ist ein Arztkonto. Um sich hier anzumelden, tippen Sie auf „Konto erstellen" und legen Sie ein Patientenkonto an, das mit Ihrem Arztkonto verknüpft ist.',
      'errorAccessDeniedThisAppRequiresRole':
          'Zugriff verweigert: Diese App erfordert die Rolle \${requiredRole.name}',
      'errorPhoneVerificationNotConfigured':
          'Telefonverifizierung nicht konfiguriert',
      'errorInvalidOrExpiredPhoneVerification':
          'Ungültige oder abgelaufene Telefonverifizierung',
      'errorPhoneNumberNotFoundInVerification':
          'Telefonnummer in der Verifizierung nicht gefunden',
      'errorInvalidPhoneInVerification':
          'Ungültige Telefonnummer in der Verifizierung',
      'errorPhoneNumberDoesNotMatchVerification':
          'Telefonnummer stimmt nicht mit der Verifizierung überein',
      'errorEmailAlreadyRegistered': 'E-Mail bereits registriert',
      'errorPhoneAlreadyRegistered': 'Telefonnummer bereits registriert',
      'errorPatientWithPhoneAlreadyExists':
          'Ein Patient mit dieser Telefonnummer existiert bereits.',
      'errorPhoneNumberNotFound': 'Telefonnummer nicht gefunden',
      'errorUserNotFound': 'Benutzer nicht gefunden',
      'errorSessionInvalid': 'Sitzung ungültig',
      'errorSessionExpiredOrSignedOut':
          'Sitzung abgelaufen oder abgemeldet',
      'errorInvalidToken': 'Ungültiges Token',
      'errorTooManyRequests':
          'Zu viele Anfragen. Bitte später erneut versuchen.',
      'errorAuthenticationRequired': 'Anmeldung erforderlich',
      'errorNoTokenReceivedFromServer': 'Kein Token vom Server erhalten',
      'errorLoginFailed': 'Anmeldung fehlgeschlagen',
      'errorFailedToSendEmailCode':
          'Senden des E-Mail-Codes fehlgeschlagen',
      'errorNoTokenReceived': 'Kein Token erhalten',
      'errorFailedToCreatePatientAccount':
          'Patientenkonto konnte nicht erstellt werden',
      'errorRegistrationFailed': 'Registrierung fehlgeschlagen',
      'errorFailedToResetPassword':
          'Passwort konnte nicht zurückgesetzt werden',
      'errorFailedToChangePassword':
          'Passwort konnte nicht geändert werden',
      'errorUnknownError': 'Unbekannter Fehler',
      'errorSomethingWentWrong': 'Etwas ist schiefgelaufen',
      'errorSessionExpiredPleaseStartAgain':
          'Sitzung abgelaufen. Bitte neu starten.',
    },
    'uz': {
      // Common
      'appName': 'Shifa Patient',
      'hello': 'Salom',
      'patient': 'Bemor',
      'doctor': 'Shifokor',
      'loading': 'Yuklanmoqda...',
      'error': 'Xato',
      'retry': 'Qayta urinish',
      'cancel': 'Bekor qilish',
      'save': 'Saqlash',
      'delete': 'O\'chirish',
      'deleteDocumentConfirmation': 'Ushbu hujjatni o\'chirmoqchimisiz?',
      'documentDeleted': 'Hujjat o\'chirildi',
      'edit': 'Tahrirlash',
      'back': 'Orqaga',
      'next': 'Keyingi',
      'complete': 'Tugallash',
      'submit': 'Yuborish',
      'close': 'Yopish',
      'yes': 'Ha',
      'no': 'Yo\'q',
      'ok': 'OK',
      'skip': 'O\'tkazib yuborish',

      // Auth
      'login': 'Kirish',
      'signIn': 'Tizimga kirish',
      'createAccount': 'Hisob yaratish',
      'existingDoctorTitle': 'Mavjud shifokor hisobi topildi',
      'existingDoctorMessage':
          '{{name}} uchun sizda allaqachon shifokor hisobi bor. Bemor hisobini yaratish uchun quyida tasdiqlang.',
      'createPatientAccount': 'Bemor hisobini yaratish',
      'accountAlreadyExists': 'Hisob allaqachon mavjud',
      'existingPatientMessage':
          'Ushbu email yoki telefon raqami bilan hisob mavjud. Kirish uchun Parolni unutdingizmi? dan foydalaning yoki boshqa email va telefon raqamini kiriting.',
      'doctorPatientAccountMessage':
          'Shifokor hisobi parolingiz bilan kirasiz. Telefoningiz va emailingizga tasdiqlash kodi yuboriladi.',
      'sendVerificationCode': 'Tasdiqlash kodini yuborish',
      'verificationCodeSent':
          'Tasdiqlash kodlari telefoningiz va emailingizga yuborildi.',
      'enterPhoneCode': 'SMS dan kodni kiriting',
      'enterEmailCode': 'Emaildan kodni kiriting',
      'phoneOrEmail': 'Telefon raqami yoki Email',
      'password': 'Parol',
      'confirmPassword': 'Parolni tasdiqlash',
      'continueWithGoogle': 'Google bilan davom etish',
      'forgotPassword': 'Parolni unutdingizmi?',
      'loginAttemptsRemaining':
          'Vaqtincha bloklashdan oldin yana {{count}} marta urinish qoldi.',
      'accountLockedTryAgainIn':
          'Juda ko\'p noto\'g\'ri urinishlar. Hisob vaqtincha bloklandi. Iltimos, {{minutes}} daqiqadan keyin qayta urinib ko\'ring.',
      'verifyAndCreate': 'Tasdiqlash va hisob yaratish',
      'invalidVerificationCode': 'Noto\'g\'ri yoki muddati o\'tgan kod',
      'resendCode': 'Kodni qayta yuborish',
      'resendCodeIn': 'Kodni {{time}} da qayta yuborish',
      'codeSentAgain': 'Tasdiqlash kodi qayta yuborildi',
      'codeExpiresIn': 'Kod {{time}} da tugaydi',
      'forgotPasswordEnterPhone':
          'Telefon raqamingizni kiriting. Parolni tiklash uchun bir martalik kod yuboramiz.',
      'forgotPasswordEnterEmail':
          'Email manzilingizni kiriting. Parolni tiklash uchun tasdiqlash kodi yuboramiz.',
      'otpSentToEmail': '6 xonali kod {email} manziliga yuborildi. Pochtangizni tekshiring.',
      'emailRequired': 'Email manzil talab qilinadi',
      'noAccountWithPhone': 'Bu telefon raqam bilan hisob topilmadi.',
      'sendCode': 'Kodni yuborish',
      'verify': 'Tasdiqlash',
      'enterNewPassword': 'Yangi parolingizni kiriting va tasdiqlang.',
      'passwordResetSuccess': 'Parol muvaffaqiyatli tiklandi.',

      // Profile
      'profile': 'Profil',
      'editProfile': 'Profilni tahrirlash',
      'language': 'Til',
      'preferences': 'Sozlamalar',
      'settingsTitle': 'Sozlamalar',
      'accountTitle': 'Hisob',
      'privacy': 'Maxfiylik',
      'deleteAccount': 'Mening akkauntimni o\'chirish',
      'deleteAccountWarning':
          'Bu amalni qaytarib bo\'lmaydi. Shaxsiy identifikatorlaringiz (telefon/email/ism) o\'chirib tashlanadi. Ba\'zi tibbiy ma\'lumotlar qonun talabiga ko\'ra saqlanishi mumkin, ammo yangi akkauntdan ko\'rinmaydi.',
      'deleteAccountVerifyTitle': 'Akkauntni o\'chirishni tasdiqlash',
      'deleteAccountOtpSubtitle':
          'Elektron pochtangizga yuborilgan 6 xonali kodni kiriting.',
      'verificationCode': 'Tasdiqlash kodi',
      'confirmDeletion': 'O\'chirishni tasdiqlash',
      'accountDeletedSuccess': 'Akkauntingiz muvaffaqiyatli o\'chirildi',
      'logOut': 'Chiqish',
      'signOutConfirm': 'Chiqishni xohlaysizmi?',
      'birthDate': 'Tug\'ilgan sana',
      'birthdate': 'Tug\'ilgan sana',
      'phoneNumber': 'Telefon raqami',
      'email': 'Elektron pochta',
      'address': 'Manzil',
      'name': 'Ism',
      'firstName': 'Ism',
      'lastName': 'Familiya',
      'surname': 'Familiya',
      'gender': 'Jins',
      'male': 'Erkak',
      'female': 'Ayol',
      'other': 'Boshqa',
      'uploadPhoto': 'Rasm yuklash',
      'selectLanguage': 'Tilni tanlash',
      'languageChanged': 'Til o\'zgartirildi',
      'profileUpdatedSuccessfully': 'Profil muvaffaqiyatli yangilandi',
      'failedToUpdateProfile': 'Profilni yangilashda xatolik',
      'discardChanges': 'O\'zgarishlarni bekor qilish',
      'saveChanges': 'O\'zgarishlarni saqlash',

      // Home
      'home': 'Bosh sahifa',
      'upcomingAppointments': 'Kutilayotgan uchrashuvlar',
      'doctorsNearMe': 'Yaqinimdagi shifokorlar',
      'noUpcomingAppointments': 'Kutilayotgan uchrashuvlar yo\'q',
      'noDoctorsAvailable': 'Shifokorlar mavjud emas',
      'noDoctorsInCloseDistance': 'Yaqin atrofda shifokorlar yo\'q',
      'myTasks': 'Mening vazifalarim',
      'remoteCareTasks': 'Masofaviy vazifalar',
      'viewAndCompleteAssignedTasks':
          'Sizga tayinlangan vazifalarni ko\'ring va bajaring',
      'openTasks': 'Ochiq vazifalar',
      'viewTasks': 'Vazifalarni ko\'rish',
      'useMyLocation': 'Joylashuvimdan foydalanish',
      'getCurrentLocation': 'Joriy joylashuvni olish',
      'latitude': 'Kenglik',
      'longitude': 'Uzunlik',

      // Shifa AI (co-pilot)
      'shifaAiTitle': 'Shifa AI',
      'shifaAiCardSubtitle':
          'Yordamchingiz — savollar, simptomlar, shifokorlarni topish',
      'shifaAiDisclaimer':
          'Shifa AI faqat umumiy ma\'lumot beradi. U professional tibbiy maslahat, tashxis yoki davolarning o\'rnini bosmaydi.',
      'copilotInputHint': 'Xabar yozing…',
      'copilotSuggestDoctors': 'Shifokorlarni tavsiya qilish',
      'copilotSuggestedDoctors': 'Tavsiya etilgan shifokorlar',
      'copilotNoSuggestedDoctors':
          'Mos shifokorlar topilmadi. Shifokorlar bo\'limidan qidiring.',
      'copilotBookWithDoctor': 'Bron qilish',
      'copilotViewProfile': 'Profil',
      'copilotTranscribeError': 'Ovozni matnga aylantirib bo\'lmadi',
      'copilotContinueToBooking':
          'Keyingi sahifada sana va vaqtni tanlaysiz. Davom etasizmi?',
      'copilotBookingTitle': 'Uchrashuv bron qilish',
      'copilotBookManual': 'Vaqt tanlash',
      'copilotAutoBook': 'Avto-bron',
      'copilotAutoBookExplainer':
          'Shifa siz tanlagan sana va vaqtga eng yaqin bo\'sh slotni bron qiladi. Pastdagi rozilikni belgilang.',
      'copilotPreferredDate': 'Afzal qilingan sana',
      'copilotPreferredTime': 'Afzal qilingan vaqt',
      'copilotConsentAutoBook': 'Shifaning mening nomimdan bron qilishiga ruxsat beraman',
      'copilotAutoBookSubmit': 'Avto-bronni tasdiqlash',
      'copilotBookedSuccess': 'Uchrashuv bronlandi. Tafsilotlar «Qabullar»da.',
      'copilotBookedViaAiReason': 'Shifa AI orqali bron qilindi',
      'copilotConfirmBookFromChatTitle': 'Chatdan bronni tasdiqlash',
      'copilotConfirmBookFromChatExplainer':
          'Shifa suhbatda kelishilgan vaqtga eng yaqin bo\'sh slotni bron qiladi. Bu suhbatdagi oldingi roziligingiz asosida amalga oshiriladi.',
      'copilotNoProviderOnPlatform':
          'Hozircha Shifa platformasida ushbu simptomlarga mos shifokor profili yo\'q. Barcha shifokorlarni «Shifokorlar» bo\'limidan ko\'rishingiz yoki keyinroq qayta urinishingiz mumkin. Ahvol og\'irlashsa, tez tibbiy yordam yoki favqulodda xizmatga murojaat qiling.',
      'copilotNextSlot': 'Keyingi slot:',
      'copilotConfidence': 'Ishonch:',
      'copilotConfidenceHigh': 'Yuqori',
      'copilotConfidenceMedium': 'O\'rtacha',
      'copilotConfidenceLow': 'Past',
      'copilotThinking': 'O\'ylayapman…',

      // Bookings
      'bookings': 'Qabullar',
      'upcoming': 'Kutilayotgan',
      'past': 'O\'tgan',
      'createBooking': 'Uchrashuv yaratish',
      'noAppointmentsFound': 'Uchrashuvlar topilmadi',
      'appointmentDetails': 'Uchrashuv tafsilotlari',
      'dateAndTime': 'Sana va vaqt',
      'location': 'Manzil',
      'about': 'Haqida',
      'services': 'Xizmatlar',
      'certificates': 'Sertifikatlar',
      'contacts': 'Aloqa',
      'selectLocationOnMap': 'Xaritada manzilni tanlash',
      'country': 'Mamlakat',
      'region': 'Viloyat',
      'district': 'Tuman',
      'city': 'Shahar',
      'postalCode': 'Pochta indeksi',
      'streetAddress': 'Ko\'cha manzili',
      'enterStreetAddress':
          'Ko\'cha manzili, bino nomi, qavat va hokazolarni kiriting',
      'streetAddressHelper':
          'Siz bu maydonni binoning tafsilotlari, qavat, xona raqami va boshqalarni qo\'shish uchun tahrirlashingiz mumkin.',
      'couldNotGetAddressDetails':
          'Manzil tafsilotlarini olishning imkoni bo\'lmadi. Iltimos, boshqa manzilni tanlang.',
      'locationServicesDisabled':
          'Joylashuv xizmatlari o\'chirilgan. Iltimos, ularni yoqing.',
      'locationPermissionDenied': 'Joylashuv ruxsatlari rad etildi.',
      'locationPermissionDeniedForever':
          'Joylashuv ruxsatlari doimiy ravishda rad etildi. Iltimos, sozlamalarda yoqing.',
      'microphonePermissionDenied':
          'Ovozli xabarlar yozish uchun mikrofon kerak.',
      'cameraPermissionDenied': 'Fotosuratlar uchun kamera kerak.',
      'permissionNeeded': 'Ruxsat kerak',
      'permissionRationaleCamera':
          'Shifa profil fotosuratlari, hujjatlar va chat uchun kamera kerak.',
      'permissionRationaleMicrophone':
          'Shifa chat uchun ovozli xabarlar va shifokor bilan video qo\'ng\'iroqlar uchun mikrofon kerak.',
      'permissionRationaleLocation':
          'Shifa profil to\'ldirishda manzilni xaritada tanlash uchun joylashuv kerak.',
      'permissionRationaleNotifications':
          'Shifa uchrashuvlar, xabarlar va hujjatlar haqida xabar berish uchun bildirishnomalar kerak.',
      'errorGettingCurrentLocation': 'Joriy joylashuvni olishda xatolik',
      'selectedLocation': 'Tanlangan joylashuv',
      'reasonForVisit': 'Tashrif sababi',
      'selectDate': 'Sanani tanlang',
      'availableTimes': 'Mavjud vaqtlar',
      'selectLocation': 'Joyni tanlang',
      'primary': 'Asosiy',
      'noAvailableTimeSlots': 'Bu sana uchun bo\'sh vaqtlar mavjud emas',
      'errorLoadingSlots': 'Vaqt bo\'shliqlarini yuklashda xatolik',
      'reasonForVisitOptional': 'Tashrif sababi (ixtiyoriy)',
      'videoConsultation': 'Video qabul',
      'view': 'Ko\'rish',
      'clinicAddress': 'Klinika manzili',
      'documentAccessRequestTitle': 'Hujjatga kirish so\'rovi',
      'documentAccessRequestMessage':
          '{requesterName} "{fileName}" hujjatiga {patientName} uchun kirish so\'radi.',
      'taskCategoryVital': 'Vital',
      'taskCategoryExercise': 'Mashq',
      'taskCategoryMedication': 'Dori',
      'taskCategoryOther': 'Boshqa',
      'photo': 'Rasm',
      'haveYourAppointment': 'Qabulni video qo\'ng\'iroq orqali o\'tkazing',
      'optional': 'Ixtiyoriy',
      'describeYourReason': 'Tashrif sababini tavsiflang',
      'confirm': 'Tasdiqlash',
      'appointmentSlotBooked': 'Qabul vaqti band qilindi',
      'appointmentRescheduledSuccessfully':
          'Qabul muvaffaqiyatli qayta rejalashtirildi',
      'payNow': 'Hozir to\'lash',
      'paymentPendingBadge': 'TO\'LOV KUTILMOQDA',
      'paymentCouldNotStart':
          'To\'lovni boshlab bo\'lmadi. Iltimos, qayta urinib ko\'ring.',
      'paymentCouldNotStartWithError':
          'To\'lovni boshlab bo\'lmadi: {{error}}',
      'paymentCompletedAppointmentConfirmed':
          'To\'lov yakunlandi. Uchrashuv tasdiqlandi.',
      'completePayment': 'To\'lovni yakunlash',
      'paymentPendingTitle': 'To\'lov kutilmoqda',
      'paymentPendingMessage':
          'Uchrashuv yaratildi va to\'lov tasdiqlanishini kutmoqda.',
      'currentPaymentStatus': 'Joriy to\'lov holati: {{status}}',
      'checking': 'Tekshirilmoqda...',
      'checkPaymentStatus': 'To\'lov holatini tekshirish',
      'continuePayment': 'To\'lovni davom ettirish',
      'backToBookings': 'Qabullarga qaytish',
      'couldNotRefreshPaymentStatus':
          'To\'lov holatini yangilab bo\'lmadi. Iltimos, qayta urinib ko\'ring.',
      'paymentStillPendingConfirmBooking':
          'To\'lov hali kutilmoqda. Ushbu bronni tasdiqlash uchun to\'lovni yakunlang.',
      'joinVideoCall': 'Video qo\'ng\'iroqqa qo\'shilish',
      'viewVisitSummary': 'Qabul xulosasini ko\'rish',
      'leaveReview': 'Sharh qoldirish',
      'yourRating': 'Sizning bahoingiz',
      'thankYouForYourRating': 'Baholaganingiz uchun rahmat.',
      'stars': 'yulduz',
      'changeBooking': 'Uchrashuvni o\'zgartirish',
      'cancelBooking': 'Uchrashuvni bekor qilish',
      'cancelAppointment': 'Uchrashuvni bekor qilish',
      'areYouSureCancel': 'Ushbu uchrashuvni bekor qilishni xohlaysizmi?',
      'appointmentCancelledSuccessfully':
          'Uchrashuv muvaffaqiyatli bekor qilindi',
      'errorCancellingAppointment': 'Uchrashuvni bekor qilishda xatolik',
      'contactDoctor': 'Shifokor bilan bog\'lanish',
      'callDoctor': 'Shifokorni qo\'ng\'iroq qilish',
      'emailDoctor': 'Shifokorga email yuborish',
      'appointmentLessThan48Hours': 'Uchrashuv 48 soatdan kam vaqt ichida',
      'contactDoctorDirectly':
          'Uchrashuvni o\'zgartirish uchun shifokor bilan to\'g\'ridan-to\'g\'ri bog\'laning.',

      // Doctors
      'doctors': 'Shifokorlar',
      'myDoctors': 'Mening shifokorlarim',
      'recommended': 'Tavsiya etilgan',
      'sortBy': 'Saralash',
      'sortByDistance': 'Masofa',
      'sortByRating': 'Reyting',
      'sortByReviews': 'Sharhlar',
      'filterBy': 'Filtr',
      'filterByRegion': 'Viloyat',
      'filterBySpecialty': 'Mutaxassislik',
      'allRegions': 'Barcha viloyatlar',
      'allSpecialties': 'Barcha mutaxassisliklar',
      'gettingYourLocation': 'Joylashuvingiz olinmoqda…',
      'usingCurrentLocation': 'Masofa uchun joriy joylashuv ishlatilmoqda',
      'couldNotGetLocationUsingProfile':
          'Joylashuv olinmadi. Profil manzili ishlatilmoqda.',
      'bookAppointment': 'Uchrashuv bron qilish',
      'reviews': 'Sharhlar',
      'noReviews': 'Hali sharhlar yo\'q',
      'writeReview': 'Sharh yozish',
      'rating': 'Baholash',
      'comment': 'Izoh',
      'commentOptional': 'Izoh (ixtiyoriy)',
      'submitReview': 'Sharhni yuborish',
      'shareExperience': 'Tajribangizni baham ko\'ring...',
      'howWasYourExperience': 'Tajribangiz qanday bo\'ldi?',
      'contactInformation': 'Aloqa ma\'lumotlari',
      'specializations': 'Mutaxassisliklar',
      'furtherInformation': 'Qo\'shimcha ma\'lumot',
      'noRatingsYet': 'Hali baholashlar yo\'q',
      'dentist': 'Tish shifokori',
      'checkUp': 'Ko\'rik',
      'cardiologist': 'Kardiolog',
      'generalpractitioner': 'Shifokor-terapevt',
      'therapist': 'Terapevt',
      'pediatrician': 'Pediatr',
      'dermatologist': 'Dermatolog',
      'ophthalmologist': 'Oftalmolog',
      'neurologist': 'Nevrolog',
      'surgeon': 'Jarroh',
      'gynecologist': 'Ginekolog',
      'urologist': 'Urolog',
      'psychiatrist': 'Psixiatr',
      'orthopedist': 'Ortoped',
      'pulmonologist': 'Pulmonolog',
      'endocrinologist': 'Endokrinolog',
      'gastroenterologist': 'Gastroenterolog',
      'otolaryngologist': 'LOR shifokori',
      'general practitioner': 'Shifokor-terapevt',
      'ent specialist': 'LOR shifokori',

      // Documents
      'documents': 'Hujjatlar',
      'searchDocuments': 'Hujjatlarni qidirish',
      'upload': 'Yuklash',
      'noDocuments': 'Hujjatlar mavjud emas',
      'noDocumentsFound': 'Hujjatlar topilmadi',
      'documentTitle': 'Hujjat nomi',
      'enterDocumentTitle': 'Hujjat nomini kiriting',
      'documentUploadedSuccessfully': 'Hujjat muvaffaqiyatli yuklandi',
      'uploadFailed': 'Yuklash muvaffaqiyatsiz',
      'openDocument': 'Hujjatni ochish',
      'takePhoto': 'Rasmga olish',
      // Document categories (optional tag)
      'documentCategoryLabel': 'Hujjat turi',
      'documentCategorySelect': 'Turini tanlang (ixtiyoriy)',
      'documentCategoryHint':
          'Ixtiyoriy. Belgilash shifokorlaringizga hujjat turi haqida tezroq tushuncha berishga yordam beradi.',
      'documentCategory_BLOOD_TEST': 'Qon tahlili',
      'documentCategory_URINE_TEST': 'Siydik tahlili',
      'documentCategory_STOOL_TEST': 'Najas tahlili',
      'documentCategory_LAB_RESULT': 'Laboratoriya natijasi',
      'documentCategory_MRI': 'MRT',
      'documentCategory_CT_SCAN': 'KT skaneri',
      'documentCategory_XRAY': 'Rentgen',
      'documentCategory_ULTRASOUND': 'UTT',
      'documentCategory_MAMMOGRAPHY': 'Mammografiya',
      'documentCategory_ECG': 'EKG',
      'documentCategory_EEG': 'EEG',
      'documentCategory_ENDOSCOPY': 'Endoskopiya',
      'documentCategory_BIOPSY': 'Biopsiya',
      'documentCategory_PATHOLOGY': 'Patologiya',
      'documentCategory_IMAGING_OTHER': 'Boshqa tasvirlash',
      'documentCategory_PRESCRIPTION': 'Retsept',
      'documentCategory_VACCINATION_RECORD': 'Emlash hujjati',
      'documentCategory_DISCHARGE_SUMMARY': 'Chiqarish xulosasi',
      'documentCategory_REFERRAL': 'Yo\'llanma',
      'documentCategory_HOSPITAL_REPORT': 'Shifoxona xulosasi',
      'documentCategory_ALLERGY_REPORT': 'Allergiya xulosasi',
      'documentCategory_OTHER_MEDICAL': 'Boshqa tibbiy natija',
      'chooseFromGallery': 'Galereyadan tanlash',
      'uploadFile': 'Fayl yuklash',
      'addedByDoctor': 'Shifokor qo\'shgan',
      'addedByYou': 'Siz qo\'shgansiz',

      // Chat
      'chat': 'Suhbat',
      'messages': 'Xabarlar',
      'searchDoctors': 'Shifokorlarni qidirish...',
      'typeMessage': 'Xabar yozing...',
      'send': 'Yuborish',
      'noConversations': 'Hali suhbatlar yo\'q',
      'isTyping': 'yozmoqda',
      'selectConversation': 'Suhbatni tanlang',
      'attachFile': 'Fayl biriktirish',
      'selectImage': 'Rasm tanlash',
      'recordVoice': 'Ovozli xabar yozish',
      'voiceMessage': 'Ovozli xabar',
      'sendVoice': 'Ovozli xabarni yuborish',
      'compressingImage': 'Rasm siqilmoqda...',
      'uploadingFile': 'Fayl yuklanmoqda...',
      'errorUploadingFile': 'Fayl yuklashda xatolik',
      'errorRecordingVoice': 'Ovoz yozishda xatolik',
      'selectDocument': 'Hujjat tanlash',

      // Notifications
      'notifications': 'Bildirishnomalar',
      'newAppointmentScheduled': 'Yangi qabul rejalashtirildi',
      'newAppointmentScheduledTitle': 'Yangi qabul rejalashtirildi',
      'appointmentCancelledTitle': 'Qabul bekor qilindi',
      'appointmentReminderTitle': 'Qabul eslatmasi',
      'appointmentChangedTitle': 'Qabul o\'zgartirildi',
      'notificationMessageScheduled':
          'Shifokor siz uchun qabul belgiladi. Tafsilotlarni Qabullar bo\'limida ko\'ring.',
      'notificationMessageCancelled':
          'Qabulingiz bekor qilindi. Kerak bo\'lsa, qayta qabul bron qiling.',
      'notificationMessageReminder':
          'Sizda yaqin keladigan qabul bor. Iltimos, vaqtida keling.',
      'notificationMessageChanged':
          'Qabulingiz boshqa vaqtga qoldirildi. Yangi sana va vaqtni Qabullar bo\'limida ko\'ring.',
      'doctorHasScheduled': 'Shifokor siz uchun qabul belgiladi',
      'taskReminderTitle': 'Vazifa eslatmasi',
      'taskAssignedTitle': 'Vazifa biriktirildi',
      'taskCancelledTitle': 'Vazifa bekor qilindi',

      // App Lock
      'appLock': 'Ilova qulfi',
      'unlockApp': 'Ilovani ochish',
      'unlockShifa': 'Shifani ochish',
      'enableBiometricForAppSecurity':
          'Ilova xavfsizligi uchun Face ID / Biometrikani yoqishni xohlaysizmi?',
      'enableBiometricPrompt':
          'Tibbiy ma\'lumotlaringizni biometrik autentifikatsiya bilan himoyalang',
      'skipBiometric': 'O\'tkazib yuborish',
      'lockAfterInactivity': 'Faolsizlikdan keyin qulflash',
      'enterPinOrUseBiometric':
          'PIN-kodingizni kiriting yoki biometrik autentifikatsiyadan foydalaning',
      'enterPinToUnlock': 'Ochish uchun PIN-kodingizni kiriting',
      'useBiometric': 'Biometrikadan foydalanish',
      'enableAppLock': 'Ilova qulfini yoqish',
      'appLockEnabled': 'Ilova qulflanadi',
      'appLockDisabled': 'Ilova qulflanmaydi',
      'pinCode': 'PIN-kod',
      'setUpPin': 'PIN-kodni sozlash',
      'setUpPinRequired':
          'Ilova qulfini yoqish uchun PIN-kod sozlashingiz kerak.',
      'setUp': 'Sozlash',
      'setUpPinDescription': 'Ilovani himoyalash uchun PIN-kod yarating',
      'changePin': 'PIN-kodni o\'zgartirish',
      'changePinDescription': 'PIN-kodingizni yangilang',
      'clearPin': 'PIN-kodni o\'chirish',
      'clearPinDescription': 'PIN-kodni o\'chirish va ilova qulfini o\'chirish',
      'clearPinConfirmation':
          'PIN-kodingizni o\'chirishni xohlaysizmi? Bu ilova qulfini o\'chiradi.',
      'clear': 'Tozalash',
      'pinSetSuccessfully': 'PIN-kod muvaffaqiyatli o\'rnatildi',
      'pinChangedSuccessfully': 'PIN-kod muvaffaqiyatli o\'zgartirildi',
      'pinCleared': 'PIN-kod o\'chirildi',
      'pinsDoNotMatch': 'PIN-kodlar mos kelmadi',
      'enterPin': 'PIN-kodni kiriting',
      'enterCurrentPin': 'Joriy PIN-kodni kiriting',
      'enterNewPin': 'Yangi PIN-kodni kiriting',
      'confirmPin': 'PIN-kodni tasdiqlang',
      'confirmNewPin': 'Yangi PIN-kodni tasdiqlang',
      'reEnterPin': 'PIN-kodingizni qayta kiriting',
      'incorrectPin': 'Noto\'g\'ri PIN-kod',
      'pinLengthRequirement': 'PIN 4-6 raqamdan iborat bo\'lishi kerak',
      'tryAgainInSeconds': '{{seconds}} soniyadan keyin qayta urinib ko\'ring',
      'forgotPinLogOut': 'PIN-ni unutdingizmi? Chiqish',
      'biometricAuthentication': 'Biometrik autentifikatsiya',
      'enableBiometric': 'Biometrik autentifikatsiyani yoqish',
      'biometricEnabled': 'Ochish uchun barmoq izi yoki yuz ID dan foydalaning',
      'biometricDisabled': 'Biometrik autentifikatsiya o\'chirilgan',
      'setUpPinFirst':
          'Biometrik autentifikatsiyani yoqish uchun avval PIN-kodni sozlang',
      'enterPinCode': 'PIN-kodingizni kiriting',
      'createAPinCode': 'Ilovani himoyalash uchun PIN-kod yarating',
      'biometricAuthenticationFailed':
          'Biometrik autentifikatsiya muvaffaqiyatsiz',
      'authenticationError': 'Autentifikatsiya xatosi',
      'uploading': 'Rasm yuklanmoqda...',
      'photoUploadEndpointNotAvailable':
          'Rasm yuklash endpoint mavjud emas. Iltimos, rasmni yangilash uchun Profilni tahrirlashdan foydalaning.',
      'profilePhotoUpdatedSuccessfully':
          'Profil rasmi muvaffaqiyatli yangilandi',
      'failedToGetPhotoUrl': 'Serverdan rasm URL manzilini olishda xatolik',
      'failedToUploadPhoto': 'Rasmni yuklashda xatolik',
      'deleteAccountConfirmation':
          'Hisobingizni o\'chirishni xohlaysizmi? Bu amalni qaytarib bo\'lmaydi.',
      'deleteAccountComingSoon': 'Hisobni o\'chirish funksiyasi tez orada',
      'couldNotReadFileBytes': 'Fayl baytlarini o\'qib bo\'lmadi',
      'pleaseSelectCheckIn': 'Iltimos, tekshiruvni tanlang',
      'checkInSubmittedSuccessfully': 'Tekshiruv muvaffaqiyatli yuborildi',
      'failedToSubmit': 'Yuborishda xatolik',
      'taskCheckIn': 'Vazifa tekshiruvi',
      'taskNotFound': 'Vazifa topilmadi',
      'noPendingCheckIns': 'Kutilayotgan tekshiruvlar yo\'q',
      'taskCheckInNotYetAvailable':
          'Hali tekshiruvlar kutilmoqda. Rejalashtirilgan vaqtdan 10 daqiqa oldin yuborishingiz mumkin.',
      'exampleValue': 'Masalan: 120/80',
      'additionalNotesOptional': 'Qo\'shimcha izohlar (ixtiyoriy)',
      'submitCheckIn': 'Tekshiruvni yuborish',
      'markAllAsRead': 'Barchasini o\'qilgan deb belgilash',
      'allNotificationsMarkedAsRead':
          'Barcha bildirishnomalar o\'qilgan deb belgilandi',
      'errorLoadingNotifications': 'Bildirishnomalarni yuklashda xatolik',
      'approve': 'Tasdiqlash',
      'reject': 'Rad etish',
      'documentAccessApproved': 'Kirish ruxsat etildi',
      'documentAccessRejected': 'Kirish so\'rovi rad etildi',
      'noNotifications': 'Bildirishnomalar yo\'q',
      'notificationCannotOpen':
          'Bildirishnoma ochilmadi. Ma\'lumot yetarli emas.',
      'signatureRequestedTitle': 'Imzo so\'raldi',
      'signatureRequestedMessage':
          '{doctorName} sizdan qabul xulosasi uchun imzo so\'ramoqda.',
      'chatNewMessageTitle': 'Yangi xabar',
      'visitSummaryReadyTitle': 'Qabul xulosasi tayyor',
      'visitSummaryReadyMessage': 'Qabuldan keyingi xulosangiz tayyor.',
      'today': 'Bugun',
      'timeYesterday': 'Kecha {time}',
      'notificationFilterAll': 'Barchasi',
      'notificationFilterAppointments': 'Qabullar',
      'notificationFilterDocuments': 'Hujjatlar',
      'notificationFilterTasks': 'Vazifalar',
      'notificationSettings': 'Sozlamalar',
      'notificationEmptyFilter': 'Bu toifada bildirishnomalar yo\'q',
      'notificationEmptyFilterHint':
          'Boshqa filtrni tanlang yoki keyinroq qayta tekshiring.',
      'notificationEmptyBody':
          'Bu yerda qabullar, hujjatlar va vazifalar bo\'yicha yangiliklarni ko\'rasiz.',
      'noTasksFound': 'Vazifalar topilmadi',

      // Status
      'confirmed': 'Tasdiqlangan',
      'cancelled': 'Bekor qilingan',
      'completed': 'Tugallangan',
      'pending': 'Kutilmoqda',

      // Video Call
      'videoCall': 'Video qo\'ng\'iroq',
      'callDuration': 'Qo\'ng\'iroq davomiyligi',

      // Account Info
      'accountInformation': 'Hisob ma\'lumotlari',
      'dateOfBirth': 'Tug\'ilgan sana',

      // Profile Image
      'profileImage': 'Profil rasmi',
      'addProfilePhoto': 'Profil rasmini qo\'shish (Ixtiyoriy)',
      'addPhotoLater':
          'Rasmni keyinroq profil sozlamalaridan qo\'shishingiz mumkin',
      'removePhoto': 'Rasmni olib tashlash',
      'completeRegistration': 'Ro\'yxatdan o\'tishni tugallash',

      // Registration
      'step1': '1-qadam',
      'step2': '2-qadam',
      'step3': '3-qadam',
      'accountCreated': 'Hisob muvaffaqiyatli yaratildi!',
      'registrationFailed': 'Ro\'yxatdan o\'tish muvaffaqiyatsiz',

      // Errors
      'required': 'Majburiy',
      'passwordsDoNotMatch': 'Parollar mos kelmaydi',
      'invalidEmail': 'Iltimos, to\'g\'ri email manzil kiriting',
      'passwordTooShort': 'Parol kamida 8 belgidan iborat bo\'lishi kerak',
      'passwordTooLong': 'Parol 128 belgidan oshmasligi kerak',
      'minimum6Characters': 'Kamida 6 belgi',
      'passwordRequirementMinLength': 'Kamida 8 belgi',
      'passwordRequirementMaxLength': '128 belgidan oshmasligi kerak',
      'passwordRequirementUppercase': 'Kamida bitta bosh harf',
      'passwordRequirementLowercase': 'Kamida bitta kichik harf',
      'passwordRequirementDigit': 'Kamida bitta raqam',
      'passwordRequirementSpecialChar':
          'Kamida bitta maxsus belgi (!@#\$%^&* va hokazo)',
      'invalidPhone': 'Iltimos, to\'g\'ri telefon raqam kiriting',
      'resetPassword': 'Parolni tiklash',
      'changePassword': 'Parolni o\'zgartirish',
      'currentPassword': 'Joriy parol',
      'mustChangePassword':
          'Birinchi marta kirganingizda parolni o\'zgartirishingiz kerak.',
      'newPassword': 'Yangi parol',
      'savePassword': 'Saqlash va davom etish',
      'passwordChangedSuccess': 'Parol muvaffaqiyatli o\'zgartirildi.',
      'noName': 'Ism yo\'q',
      'navigationError': 'Navigatsiya xatosi',
      'locationLabel': 'Manzil',
      'goToSplash': 'Bosh sahifaga',
      'failedToSend': 'Yuborishda xatolik',
      'chatImage': 'Chat rasmi',
      'failedToUploadImage': 'Rasm yuklashda xatolik',
      'slideUpToCancel': 'Bekor qilish uchun yuqoriga siljiting',
      'slideLeftToCancel': 'Bekor qilish uchun chapga siljiting',
      'seconds': 'soniya',
      'sec': 'sek',
      'failedToStartChat': 'Chatni boshlashda xatolik',
      'noDoctorsFound': 'Shifokorlar topilmadi',
      'imageUploadComingSoon':
          'Rasm yuklash tez orada qo\'shiladi. Hozircha rasm URL manzilidan foydalaning.',
      'couldNotOpenMapApplication': 'Xarita ilovasini ochib bo\'lmadi',
      'goBack': 'Orqaga',
      'certificate': 'Sertifikat',
      'openInMaps': 'Xaritada ochish',
      'appointmentNotFound': 'Uchrashuv topilmadi',
      'errorOpeningDocument': 'Hujjatni ochishda xatolik',
      'noMessages': 'Xabarlar yo\'q',
      'refresh': 'Yangilash',
      'waitingForDoctor': 'Shifokor kutilmoqda',
      'join': 'Qo\'shilish',
      'information': 'Ma\'lumot',
      'pleaseCompleteAllRequiredFields':
          'Iltimos, barcha majburiy maydonlarni to\'ldiring',
      'cannotMakePhoneCall': 'Qo\'ng\'iroq qilish imkonsiz',
      'cannotSendEmail': 'Email yuborish imkonsiz',
      'errorParsingAppointmentData':
          'Uchrashuv ma\'lumotlarini o\'qishda xatolik',
      'unknownDoctor': 'Noma\'lum shifokor',
      'failedToStartVideoCall': 'Video qo\'ng\'iroqni boshlashda xatolik',
      'videoCallConnectionTimeout':
          'Ulanish vaqti tugadi. Internetingizni tekshirib, qayta urinib ko\'ring.',
      'videoCallEnded': 'Video qo\'ng\'iroq tugadi',
      'callErrorOccurred': 'Qo\'ng\'iroqda xatolik',
      'waitingForParticipants': 'Ishtirokchilar kutilmoqda...',
      'failedToLoadDoctor': 'Shifokor ma\'lumotlarini yuklashda xatolik',
      'newAppointmentBookedButFailedToCancelOld':
          'Yangi uchrashuv band qilindi, lekin eskisini bekor qilish muvaffaqiyatsiz',
      'failedToBookAppointment': 'Uchrashuvni bron qilishda xatolik',
      'enterYourAddress': 'Manzilingizni kiriting',
      'date': 'Sana',
      'passwordRequired': 'Parol kiritilishi shart',
      'pleaseConfirmPassword': 'Iltimos, parolingizni tasdiqlang',
      'phoneNumberRequired': 'Telefon raqami kiritilishi shart',
      'addProfilePhotoOptional': 'Profil rasmini qo\'shish (ixtiyoriy)',
      'youCanAddPhotoLater':
          'Profil sozlamalaridan keyinroq rasm qo\'shishingiz mumkin',
      'emailOptional': 'Email (ixtiyoriy)',
      'noBiographyAvailable': 'Tarjimai hol mavjud emas',
      'noServicesAvailable': 'Xizmatlar mavjud emas',
      'noCertificatesAvailable': 'Sertifikatlar mavjud emas',
      'noContactInformationAvailable': 'Aloqa ma\'lumotlari mavjud emas',
      'errorLoadingReviews': 'Sharhlarni yuklashda xatolik',
      'loadingDocument': 'Hujjat yuklanmoqda...',
      'unsupportedDocumentType':
          'Ushbu fayl turi ilovada ko\'rsatilmaydi. Qo\'llab-quvvatlanadi: PDF va rasmlar (JPG, PNG, WebP).',
      'download': 'Yuklab olish',
      'downloadStarted': 'Yuklab olish boshlandi',
      'justNow': 'Hozir',
      'yesterday': 'Kecha',
      'minutesAgo': '%s daqiqa oldin',
      'minuteAgo': '1 daqiqa oldin',
      'hoursAgo': '%s soat oldin',
      'hourAgo': '1 soat oldin',
      'daysAgo': '%s kun oldin',
      'dayAgo': '1 kun oldin',
      'starts': 'Boshlanadi',
      'started': 'Boshlandi',
      'document': 'Hujjat',
      'videoCallReady': 'Video qo\'ng\'iroq tayyor',
      'videoCallYouCanJoinNow':
          'Endi video qo\'ng\'iroqqa qo\'shilishingiz mumkin.',
      'clickBelowToJoinCall':
          'Qo\'ng\'iroqqa qo\'shilish uchun quyidagi tugmani bosing',
      'signAppointmentSummary': 'Qabul xulosasini imzolash',
      'visitSummaryTitle': 'Qabul xulosasi',
      'visitSummaryPreparing': 'Qabul xulosangiz tayyorlanmoqda.',
      'visitSummaryWhatHappened': 'Bugun nimalar bo\'ldi',
      'visitSummaryCarePlan': 'Sizning rejangiz',
      'visitSummaryMedications': 'Dori-darmonlar',
      'visitSummaryMissedDose': 'Doza o\'tkazib yuborilsa',
      'visitSummaryRedFlags': 'Xavf belgilar',
      'visitSummaryNextSteps': 'Keyingi qadamlar',
      'visitSummaryAskTitle': 'Xulosa bo\'yicha savol bering',
      'visitSummaryAskHint': 'Savolingizni yozing',
      'visitSummaryChecklistReminderTitle': 'Reja bo\'yicha eslatma',
      'visitSummaryReminderCreated': 'Eslatma yaratildi',
      'visitSummarySources': 'Manbalar',
      'remindMe': 'Eslat',
      'visitSummaryQuickReminderTitle': 'Eslatma o\'rnating',
      'visitSummaryQuick15Min': '15 daqiqadan so\'ng',
      'visitSummaryTonight': 'Bugun kechqurun (20:00)',
      'visitSummaryTomorrowMorning': 'Ertaga ertalab (09:00)',
      'visitSummaryCustom': 'Sana va vaqtni tanlash',
      'appointmentSummaryPreview': 'Qabul xulosasi',
      'confirmAppointmentSummaryReflectsDiscussion':
          'Ushbu qabul xulosasi suhbatimizni aks ettirishini tasdiqlayman.',
      'yourSignature': 'Sizning imzoingiz',
      'pleaseSignAbove': 'Iltimos, yuqoridagi maydoncha da imzo qo\'ying.',
      'signatureSubmittedSuccess': 'Imzo muvaffaqiyatli yuborildi.',
      'time': 'Vaqt',
      'reason': 'Sabab',
      'errorSaving': 'Saqlashda xatolik',
      'signatureAlreadySubmitted': 'Imzo allaqachon yuborilgan',
      'appInitializationError': 'Ilova ishga tushirishda xato',
      'pleaseRestartApp': 'Iltimos, ilovani qayta ishga tushiring',
      'notificationChannelName': 'Shifa Patient bildirishnomalari',
      'notificationChannelDescription':
          'Xabarlar, uchrashuvlar va vazifalar haqida bildirishnomalar',
      'notificationChannelAppointmentsName': 'Uchrashuv bildirishnomalari',
      'notificationChannelAppointmentsDescription':
          'Uchrashuv yangilanishlari haqida bildirishnomalar',
      // Backend / API errors (Auth, Security, Video, fallbacks)
      'errorNoAccountFound': 'Hisob topilmadi',
      'errorNoDoctorAccountFound': 'Shifokor hisobi topilmadi',
      'errorNoPatientAccountFound': 'Bemor hisobi topilmadi',
      'errorNoAdminAccountFound': 'Admin hisobi topilmadi',
      'errorDoctorProfileNotFound': 'Shifokor profili topilmadi',
      'errorEmailOtpRequiredWhenEmailProvided':
          'Email kiritilganda tasdiqlash kodi (OTP) talab qilinadi',
      'errorEmailVerificationCodeRequired':
          'Email tasdiqlash kodi talab qilinadi',
      'errorInvalidOrExpiredEmailVerificationCode':
          'Email tasdiqlash kodi noto\'g\'ri yoki muddati tugagan',
      'errorInvalidPhoneNumber': 'Telefon raqami noto\'g\'ri',
      'errorAccountIsDisabled': 'Hisob o\'chirilgan',
      'errorNotADoctorAccount': 'Bu shifokor hisobi emas',
      'errorMissingBearerToken': 'Bearer token mavjud emas',
      'errorFirebaseVerificationNotConfigured':
          'Firebase tasdiqlash sozlanmagan',
      'errorInvalidOrExpiredToken': 'Token noto\'g\'ri yoki muddati tugagan',
      'errorAccessRestrictedToDoctors': 'Kirish faqat shifokorlar uchun',
      'errorYourAccountHasBeenBlocked': 'Hisobingiz bloklangan',
      'errorInvalidKey': 'Kalit noto\'g\'ri',
      'errorKeyAlreadyUsed': 'Kalit allaqachon ishlatilgan',
      'errorUsernameRequired': 'Foydalanuvchi nomi talab qilinadi',
      'errorPasswordRequired': 'Parol talab qilinadi',
      'errorInvalidCredentials':
          'Bu telefon raqami yoki email bilan hisob topilmadi. Ma\'lumotlarni tekshiring yoki yangi hisob yarating.',
      'errorAccountLocked': 'Hisob bloklangan',
      'errorInvalidCredentialsPasswordMismatch':
          'Parol noto\'g\'ri. Qaytadan urinib ko\'ring yoki parolni tiklang.',
      'errorCreatePatientAccountFirst':
          'Bu shifokor hisobi. Bu yerga kirish uchun "Hisob yaratish" tugmasini bosing va shifokor hisobingizga bog\'langan bemor hisobini yarating.',
      'errorAccessDeniedThisAppRequiresRole':
          'Kirish rad etildi: Bu ilova \${requiredRole.name} rolini talab qiladi',
      'errorPhoneVerificationNotConfigured': 'Telefon tasdiqlash sozlanmagan',
      'errorInvalidOrExpiredPhoneVerification':
          'Telefon tasdiqlash noto\'g\'ri yoki muddati tugagan',
      'errorPhoneNumberNotFoundInVerification':
          'Tasdiqlashda telefon raqami topilmadi',
      'errorInvalidPhoneInVerification': 'Tasdiqlashdagi telefon noto\'g\'ri',
      'errorPhoneNumberDoesNotMatchVerification':
          'Telefon raqami tasdiqlash ma\'lumotiga mos emas',
      'errorEmailAlreadyRegistered': 'Email allaqachon ro\'yxatdan o\'tgan',
      'errorPhoneAlreadyRegistered':
          'Telefon raqami allaqachon ro\'yxatdan o\'tgan',
      'errorPatientWithPhoneAlreadyExists':
          'Ushbu telefon raqamiga ega bemor allaqachon mavjud',
      'errorPhoneNumberNotFound': 'Telefon raqami topilmadi',
      'errorUserNotFound': 'Foydalanuvchi topilmadi',
      'errorSessionInvalid': 'Sessiya noto\'g\'ri',
      'errorSessionExpiredOrSignedOut':
          'Sessiya muddati tugagan yoki tizimdan chiqilgan',
      'errorInvalidToken': 'Token noto\'g\'ri',
      'errorTooManyRequests':
          'Juda ko\'p so\'rov yuborildi. Iltimos, keyinroq urinib ko\'ring.',
      'errorPatientProfileNotFoundForUser':
          '\${user.id} foydalanuvchi uchun bemor profili topilmadi. Iltimos, telefon yoki email bilan profilingizni to\'ldiring.',
      'errorAuthenticationRequired': 'Autentifikatsiya talab qilinadi',
      'errorAppointmentNotFound': 'Qabul topilmadi: {{id}}',
      'errorVideoCallNotYetAvailable':
          'Video qo\'ng\'iroq hali mavjud emas. Uchrashuv boshlanishidan 5 daqiqa oldin qo\'shilish mumkin.',
      'errorVideoCallHasEnded':
          'Video qo\'ng\'iroq tugagan. Uchrashuv tugaganidan 15 daqiqa o\'tib kirish yopiladi.',
      'errorAppointmentDoesNotBelongToDoctor':
          'Bu qabul ushbu shifokorga tegishli emas',
      'errorAppointmentDoesNotHavePatientAssigned':
          'Bu qabulga bemor tayinlanmagan. Iltimos, qabul profilingizga ulanganligini tekshiring.',
      'errorAppointmentDoesNotBelongToPatient':
          'Bu qabul ushbu bemorga tegishli emas',
      'errorFailedToRetrievePatientProfile':
          'Bemor profilini olishda xato: {{detail}}',
      'errorFailedToGetOrCreateRoom':
          'Xonani olish yoki yaratishda xato: {{detail}}',
      'errorUnableToDetermineUserIdentity': 'Foydalanuvchi aniqlanmadi',
      'errorUnableToDetermineUserName': 'Foydalanuvchi nomi aniqlanmadi',
      'errorUserNameCannotBeBlank':
          'Foydalanuvchi nomi bo\'sh bo\'lishi mumkin emas',
      'errorFailedToGenerateToken': 'Token yaratishda xato',
      'errorFailedToGenerateTokenDetail': 'Token yaratishda xato: {{detail}}',
      'errorFailedToGenerateVideoToken':
          'Video token yaratishda xato: {{detail}}',
      'errorDailyApiKeyNotConfigured':
          'Daily.co API kaliti sozlanmagan. Railway da DAILY_API_KEY o\'rnating.',
      'errorNoTokenReceivedFromServer': 'Serverdan token olinmadi',
      'errorNoTokenReceived': 'Token olinmadi',
      'errorInvalidResponseFromServer': 'Serverdan noto\'g\'ri javob: {{code}}',
      'errorLoginFailed': 'Kirish muvaffaqiyatsiz',
      'errorNetworkError': 'Tarmoq xatosi: {{type}}',
      'errorFailedToSendEmailCode': 'Email kodi yuborilmadi',
      'errorFailedToCreatePatientAccount': 'Bemor hisobi yaratilmadi',
      'errorRegistrationFailed': 'Ro\'yxatdan o\'tish muvaffaqiyatsiz',
      'errorFailedToResetPassword': 'Parolni tiklash muvaffaqiyatsiz',
      'errorFailedToChangePassword': 'Parolni o\'zgartirib bo\'lmadi',
      'errorUnknownError': 'Noma\'lum xato',
      'errorStatusCode': 'Xato {{code}}',
      'errorSomethingWentWrong': 'Nimadir noto\'g\'ri bajarildi',
      'errorSessionExpiredPleaseStartAgain':
          'Sessiya tugadi. Iltimos, qaytadan boshlang.',
    },
    'ru': {
      // Common
      'appName': 'Shifa Patient',
      'hello': 'Привет',
      'patient': 'Пациент',
      'doctor': 'Врач',
      'loading': 'Загрузка...',
      'error': 'Ошибка',
      'retry': 'Повторить',
      'cancel': 'Отмена',
      'save': 'Сохранить',
      'delete': 'Удалить',
      'deleteDocumentConfirmation':
          'Вы уверены, что хотите удалить этот документ?',
      'documentDeleted': 'Документ удалён',
      'edit': 'Редактировать',
      'back': 'Назад',
      'next': 'Далее',
      'complete': 'Завершить',
      'submit': 'Отправить',
      'close': 'Закрыть',
      'yes': 'Да',
      'no': 'Нет',

      // Auth
      'login': 'Вход',
      'signIn': 'Войти',
      'createAccount': 'Создать аккаунт',
      'existingDoctorTitle': 'Найден аккаунт врача',
      'existingDoctorMessage':
          'У вас уже есть аккаунт врача для {{name}}. Подтвердите ниже, чтобы создать аккаунт пациента.',
      'createPatientAccount': 'Создать аккаунт пациента',
      'phoneOrEmail': 'Номер телефона или Email',
      'password': 'Пароль',
      'confirmPassword': 'Подтвердите пароль',
      'continueWithGoogle': 'Продолжить с Google',
      'forgotPassword': 'Забыли пароль?',
      'loginAttemptsRemaining':
          'Осталось {{count}} попыток входа до временной блокировки.',
      'accountLockedTryAgainIn':
          'Слишком много неудачных попыток. Вход временно заблокирован. Повторите попытку через {{minutes}} мин.',
      'verifyAndCreate': 'Подтвердить и создать аккаунт',
      'invalidVerificationCode': 'Неверный или истёкший код',
      'noAccountWithPhone': 'Аккаунт с этим номером телефона не найден.',
      'resendCode': 'Отправить код снова',
      'resendCodeIn': 'Отправить код снова через {{time}}',
      'codeSentAgain': 'Код подтверждения отправлен снова',
      'codeExpiresIn': 'Код истекает через {{time}}',
      'forgotPasswordEnterEmail':
          'Введите ваш email. Мы отправим вам код для сброса пароля.',
      'otpSentToEmail': '6-значный код отправлен на {email}. Проверьте почту.',
      'emailRequired': 'Требуется адрес электронной почты',

      // Profile
      'profile': 'Профиль',
      'editProfile': 'Редактировать профиль',
      'language': 'Язык',
      'preferences': 'Настройки',
      'settingsTitle': 'Настройки',
      'accountTitle': 'Аккаунт',
      'privacy': 'Конфиденциальность',
      'deleteAccount': 'Удалить мой аккаунт',
      'deleteAccountWarning':
          'Это действие необратимо. Ваши личные идентификаторы (телефон/почта/имя) будут удалены. Некоторые медицинские данные могут храниться по закону, но не будут доступны из нового аккаунта.',
      'deleteAccountVerifyTitle': 'Подтвердите удаление',
      'deleteAccountOtpSubtitle':
          'Введите 6-значный код, отправленный на вашу электронную почту.',
      'verificationCode': 'Код подтверждения',
      'confirmDeletion': 'Подтвердить удаление',
      'accountDeletedSuccess': 'Ваш аккаунт успешно удалён',
      'logOut': 'Выйти',
      'signOutConfirm': 'Вы действительно хотите выйти?',
      'birthDate': 'Дата рождения',
      'phoneNumber': 'Номер телефона',
      'email': 'Email',
      'address': 'Адрес',
      'name': 'Имя',
      'surname': 'Фамилия',
      'gender': 'Пол',
      'male': 'Мужской',
      'female': 'Женский',
      'other': 'Другое',
      'uploadPhoto': 'Загрузить фото',
      'selectLanguage': 'Выберите язык',
      'languageChanged': 'Язык изменен на',
      'profileUpdatedSuccessfully': 'Профиль успешно обновлен',
      'failedToUpdateProfile': 'Не удалось обновить профиль',
      'discardChanges': 'Отменить изменения',
      'saveChanges': 'Сохранить изменения',

      // Home
      'upcomingAppointments': 'Предстоящие приемы',
      'doctorsNearMe': 'Врачи рядом со мной',
      'noUpcomingAppointments': 'Нет предстоящих приемов',
      'noDoctorsAvailable': 'Врачи недоступны',
      'noDoctorsInCloseDistance': 'Нет врачей поблизости',
      'viewTasks': 'Просмотр задач',
      'useMyLocation': 'Использовать моё местоположение',

      // Shifa AI (co-pilot)
      'shifaAiTitle': 'Shifa AI',
      'shifaAiCardSubtitle':
          'Ваш помощник — вопросы, симптомы, поиск врачей',
      'shifaAiDisclaimer':
          'Shifa AI даёт только общую информацию. Это не замена профессиональной медицинской консультации, диагностики или лечения.',
      'copilotInputHint': 'Введите сообщение…',
      'copilotSuggestDoctors': 'Подобрать врачей',
      'copilotSuggestedDoctors': 'Рекомендованные врачи',
      'copilotNoSuggestedDoctors':
          'Подходящих врачей не найдено. Попробуйте поиск во вкладке Врачи.',
      'copilotBookWithDoctor': 'Запись',
      'copilotViewProfile': 'Профиль',
      'copilotTranscribeError': 'Не удалось распознать речь',
      'copilotContinueToBooking':
          'На следующем экране вы выберете дату и время. Продолжить?',
      'copilotBookingTitle': 'Записаться на приём',
      'copilotBookManual': 'Выбрать время',
      'copilotAutoBook': 'Авто-запись',
      'copilotAutoBookExplainer':
          'Shifa запишет на ближайший свободный слот к выбранной дате и времени. Подтвердите согласие ниже.',
      'copilotPreferredDate': 'Желаемая дата',
      'copilotPreferredTime': 'Желаемое время',
      'copilotConsentAutoBook': 'Разрешаю Shifa записать меня от моего имени',
      'copilotAutoBookSubmit': 'Подтвердить авто-запись',
      'copilotBookedSuccess': 'Запись создана. Подробности в разделе Записи.',
      'copilotBookedViaAiReason': 'Запись через Shifa AI',
      'copilotConfirmBookFromChatTitle': 'Подтвердить запись из чата',
      'copilotConfirmBookFromChatExplainer':
          'Shifa запишет на ближайший свободный слот к обсуждённому времени. Это использует ваше согласие из этого разговора.',
      'copilotNoProviderOnPlatform':
          'На платформе Shifa пока нет врача, подходящего под эти симптомы. Загляните во вкладку «Врачи» или попробуйте позже. При ухудшении состояния обратитесь за срочной помощью или в скорую.',
      'copilotNextSlot': 'Ближайшее время:',
      'copilotConfidence': 'Уверенность:',
      'copilotConfidenceHigh': 'Высокая',
      'copilotConfidenceMedium': 'Средняя',
      'copilotConfidenceLow': 'Низкая',
      'copilotThinking': 'Думаю…',

      'locationPermissionDenied': 'Доступ к местоположению запрещён.',
      'locationPermissionDeniedForever':
          'Доступ к местоположению постоянно запрещён. Включите в настройках.',
      'microphonePermissionDenied':
          'Доступ к микрофону нужен для голосовых сообщений.',
      'cameraPermissionDenied': 'Доступ к камере нужен для фотографий.',
      'permissionNeeded': 'Требуется разрешение',
      'permissionRationaleCamera':
          'Shifa нужен доступ к камере для фото профиля, документов и чата.',
      'permissionRationaleMicrophone':
          'Shifa нужен доступ к микрофону для голосовых сообщений и видеозвонков с врачом.',
      'permissionRationaleLocation':
          'Shifa нужен доступ к местоположению для выбора адреса на карте в профиле.',
      'permissionRationaleNotifications':
          'Shifa нужны уведомления о приёмах, сообщениях и документах.',

      // Bookings
      'bookings': 'Записи',
      'upcoming': 'Предстоящие',
      'past': 'Прошедшие',
      'createBooking': 'Создать запись',
      'noAppointmentsFound': 'Записи не найдены',
      'appointmentDetails': 'Детали записи',
      'dateAndTime': 'Дата и время',
      'location': 'Местоположение',
      'selectedLocation': 'Выбранное местоположение',
      'reasonForVisit': 'Причина визита',
      'selectDate': 'Выберите дату',
      'availableTimes': 'Доступное время',
      'selectLocation': 'Выберите локацию',
      'primary': 'Основная',
      'noAvailableTimeSlots': 'На эту дату нет доступных слотов',
      'errorLoadingSlots': 'Ошибка загрузки слотов',
      'joinVideoCall': 'Присоединиться к видеозвонку',
      'viewVisitSummary': 'Смотреть итог визита',
      'leaveReview': 'Оставить отзыв',
      'yourRating': 'Ваша оценка',
      'thankYouForYourRating': 'Спасибо за вашу оценку.',
      'stars': 'звёзд',
      'changeBooking': 'Изменить запись',
      'cancelBooking': 'Отменить запись',
      'cancelAppointment': 'Отменить запись',
      'areYouSureCancel': 'Вы уверены, что хотите отменить эту запись?',
      'appointmentCancelledSuccessfully': 'Запись успешно отменена',
      'appointmentRescheduledSuccessfully': 'Запись успешно перенесена',
      'payNow': 'Оплатить сейчас',
      'paymentPendingBadge': 'ОЖИДАЕТ ОПЛАТЫ',
      'paymentCouldNotStart':
          'Не удалось начать оплату. Пожалуйста, попробуйте снова.',
      'paymentCouldNotStartWithError':
          'Не удалось начать оплату: {{error}}',
      'paymentCompletedAppointmentConfirmed':
          'Оплата завершена. Запись подтверждена.',
      'completePayment': 'Завершить оплату',
      'paymentPendingTitle': 'Ожидание оплаты',
      'paymentPendingMessage':
          'Ваша запись создана и ожидает подтверждения оплаты.',
      'currentPaymentStatus': 'Текущий статус оплаты: {{status}}',
      'checking': 'Проверка...',
      'checkPaymentStatus': 'Проверить статус оплаты',
      'continuePayment': 'Продолжить оплату',
      'backToBookings': 'Назад к записям',
      'couldNotRefreshPaymentStatus':
          'Не удалось обновить статус оплаты. Пожалуйста, попробуйте снова.',
      'paymentStillPendingConfirmBooking':
          'Оплата всё ещё в ожидании. Завершите оплату, чтобы подтвердить эту запись.',
      'errorCancellingAppointment': 'Ошибка при отмене записи',
      'contactDoctor': 'Связаться с врачом',
      'callDoctor': 'Позвонить врачу',
      'emailDoctor': 'Написать врачу',
      'appointmentLessThan48Hours': 'Прием менее чем через 48 часов',
      'contactDoctorDirectly':
          'Чтобы изменить эту запись, пожалуйста, свяжитесь с врачом напрямую.',

      // Doctors
      'doctors': 'Врачи',
      'myDoctors': 'Мои врачи',
      'recommended': 'Рекомендуемые',
      'bookAppointment': 'Записаться на прием',
      'reviews': 'Отзывы',
      'noReviews': 'Пока нет отзывов',
      'writeReview': 'Написать отзыв',
      'rating': 'Рейтинг',
      'comment': 'Комментарий',
      'commentOptional': 'Комментарий (необязательно)',
      'submitReview': 'Отправить отзыв',
      'shareExperience': 'Поделитесь своим опытом...',
      'howWasYourExperience': 'Как был ваш опыт?',
      'contactInformation': 'Контактная информация',
      'specializations': 'Специализации',
      'furtherInformation': 'Дополнительная информация',
      'noRatingsYet': 'Пока нет оценок',

      // Documents
      'documents': 'Документы',
      'searchDocuments': 'Поиск документов...',
      'upload': 'Загрузить',
      'noDocuments': 'Документы недоступны',
      'noDocumentsFound': 'Документы не найдены',
      'documentTitle': 'Название документа',
      'enterDocumentTitle': 'Введите название документа',
      'documentUploadedSuccessfully': 'Документ успешно загружен',
      'uploadFailed': 'Ошибка загрузки',
      'openDocument': 'Открыть документ',
      'addedByDoctor': 'Добавлено врачом',
      'addedByYou': 'Добавлено вами',
      // Document categories (optional tag)
      'documentCategoryLabel': 'Тип документа',
      'documentCategorySelect': 'Выберите тип (необязательно)',
      'documentCategoryHint':
          'Необязательно. Тегирование помогает врачам быстрее понять, что это за документ.',
      'documentCategory_BLOOD_TEST': 'Анализ крови',
      'documentCategory_URINE_TEST': 'Анализ мочи',
      'documentCategory_STOOL_TEST': 'Анализ кала',
      'documentCategory_LAB_RESULT': 'Лабораторный анализ',
      'documentCategory_MRI': 'МРТ',
      'documentCategory_CT_SCAN': 'КТ',
      'documentCategory_XRAY': 'Рентген',
      'documentCategory_ULTRASOUND': 'УЗИ',
      'documentCategory_MAMMOGRAPHY': 'Маммография',
      'documentCategory_ECG': 'ЭКГ',
      'documentCategory_EEG': 'ЭЭГ',
      'documentCategory_ENDOSCOPY': 'Эндоскопия',
      'documentCategory_BIOPSY': 'Биопсия',
      'documentCategory_PATHOLOGY': 'Патология',
      'documentCategory_IMAGING_OTHER': 'Другая визуализация',
      'documentCategory_PRESCRIPTION': 'Рецепт',
      'documentCategory_VACCINATION_RECORD': 'Запись о вакцинации',
      'documentCategory_DISCHARGE_SUMMARY': 'Выписной эпикриз',
      'documentCategory_REFERRAL': 'Направление',
      'documentCategory_HOSPITAL_REPORT': 'Заключение из больницы',
      'documentCategory_ALLERGY_REPORT': 'Аллергологический отчёт',
      'documentCategory_OTHER_MEDICAL': 'Другой медицинский результат',

      // Chat
      'chat': 'Чат',
      'messages': 'Сообщения',
      'searchDoctors': 'Поиск врачей...',
      'typeMessage': 'Введите сообщение...',
      'send': 'Отправить',
      'noConversations': 'Пока нет разговоров',
      'isTyping': 'печатает',
      'selectConversation': 'Выберите беседу',
      'attachFile': 'Прикрепить файл',
      'selectImage': 'Выбрать изображение',
      'takePhoto': 'Сделать фото',
      'chooseFromGallery': 'Выбрать из галереи',
      'recordVoice': 'Записать голосовое сообщение',
      'voiceMessage': 'Голосовое сообщение',
      'sendVoice': 'Отправить голосовое',
      'compressingImage': 'Сжатие изображения...',
      'uploadingFile': 'Загрузка файла...',
      'errorUploadingFile': 'Ошибка загрузки файла',
      'errorRecordingVoice': 'Ошибка записи голоса',
      'selectDocument': 'Выбрать документ',

      // Status
      'confirmed': 'Подтверждено',
      'cancelled': 'Отменено',
      'completed': 'Завершено',
      'pending': 'Ожидается',

      // Video Call
      'videoCall': 'Видеозвонок',
      'videoCallYouCanJoinNow':
          'Теперь вы можете присоединиться к видеозвонку.',
      'callDuration': 'Длительность звонка',

      // Account Info
      'accountInformation': 'Информация об аккаунте',
      'dateOfBirth': 'Дата рождения',

      // Profile Image
      'profileImage': 'Фото профиля',
      'addProfilePhoto': 'Добавить фото профиля (Необязательно)',
      'addPhotoLater': 'Вы можете добавить фото позже в настройках профиля',
      'removePhoto': 'Удалить фото',
      'completeRegistration': 'Завершить регистрацию',

      // Registration
      'step1': 'Шаг 1',
      'step2': 'Шаг 2',
      'step3': 'Шаг 3',
      'accountCreated': 'Аккаунт успешно создан!',
      'registrationFailed': 'Регистрация не удалась',

      // Errors
      'required': 'Обязательно',
      'passwordsDoNotMatch': 'Пароли не совпадают',
      'invalidEmail':
          'Пожалуйста, введите действительный адрес электронной почты',
      'passwordTooShort': 'Пароль должен содержать не менее 6 символов',
      'minimum6Characters': 'Минимум 6 символов',
      'invalidPhone': 'Пожалуйста, введите действительный номер телефона',

      // Notifications
      'notifications': 'Уведомления',
      'noNotifications': 'Нет уведомлений',
      'notificationCannotOpen':
          'Невозможно открыть уведомление. Недостаточно данных.',
      'signatureRequestedTitle': 'Требуется подпись',
      'signatureRequestedMessage':
          '{doctorName} запрашивает вашу подпись к итогу приёма.',
      'visitSummaryReadyTitle': 'Итог визита готов',
      'visitSummaryReadyMessage': 'Ваш итог после визита уже доступен.',
      'chatNewMessageTitle': 'Новое сообщение',
      'appointmentReminderTitle': 'Напоминание о приёме',
      'appointmentCancelledTitle': 'Приём отменён',
      'appointmentChangedTitle': 'Приём перенесён',
      'newAppointmentScheduledTitle': 'Новый приём назначен',
      'notificationMessageReminder':
          'У вас скоро приём. Пожалуйста, приходите вовремя.',
      'notificationMessageCancelled':
          'Ваш приём отменён. При необходимости запишитесь снова.',
      'notificationMessageChanged':
          'Приём перенесён. Проверьте новую дату и время в записях.',
      'notificationMessageScheduled':
          'Врач назначил вам приём. Подробности в разделе «Записи».',
      'markAllAsRead': 'Отметить все прочитанными',
      'allNotificationsMarkedAsRead':
          'Все уведомления отмечены как прочитанные',
      'errorLoadingNotifications': 'Ошибка загрузки уведомлений',
      'today': 'Сегодня',
      'yesterday': 'Вчера',
      'timeYesterday': 'Вчера {time}',
      'notificationFilterAll': 'Все',
      'notificationFilterAppointments': 'Приёмы',
      'notificationFilterDocuments': 'Документы',
      'notificationFilterTasks': 'Задачи',
      'notificationSettings': 'Настройки',
      'notificationEmptyFilter': 'В этой категории нет уведомлений',
      'notificationEmptyFilterHint':
          'Попробуйте другой фильтр или загляните позже.',
      'notificationEmptyBody':
          'Здесь будут обновления о приёмах, документах и задачах.',
      'visitSummaryTitle': 'Итог визита',
      'visitSummaryPreparing': 'Итог визита готовится.',
      'visitSummaryWhatHappened': 'Что произошло сегодня',
      'visitSummaryCarePlan': 'Ваш план лечения',
      'visitSummaryMedications': 'Лекарства',
      'visitSummaryMissedDose': 'Пропущенная доза',
      'visitSummaryRedFlags': 'Тревожные признаки',
      'visitSummaryNextSteps': 'Следующие шаги',
      'visitSummaryAskTitle': 'Спросить об этом итоге',
      'visitSummaryAskHint': 'Введите ваш вопрос',
      'visitSummaryChecklistReminderTitle': 'Напоминание о плане лечения',
      'visitSummaryReminderCreated': 'Напоминание создано',
      'visitSummarySources': 'Источники',
      'remindMe': 'Напомнить',
      'visitSummaryQuickReminderTitle': 'Установить напоминание',
      'visitSummaryQuick15Min': 'Через 15 минут',
      'visitSummaryTonight': 'Сегодня вечером (20:00)',
      'visitSummaryTomorrowMorning': 'Завтра утром (09:00)',
      'visitSummaryCustom': 'Выбрать дату и время',
      'taskReminderTitle': 'Напоминание о задаче',
      'taskAssignedTitle': 'Назначена задача',
      'taskCancelledTitle': 'Задача отменена',
      'approve': 'Разрешить',
      'reject': 'Отклонить',
      'documentAccessApproved': 'Доступ разрешён',
      'documentAccessRejected': 'Запрос на доступ отклонён',
      'documentAccessRequestTitle': 'Запрос доступа к документу',
      'documentAccessRequestMessage':
          '{requesterName} запросил доступ к «{fileName}» для {patientName}.',
      // Backend / API errors (Auth, Security, Video, fallbacks)
      'errorNoAccountFound': 'Аккаунт не найден',
      'errorNoDoctorAccountFound': 'Аккаунт врача не найден',
      'errorNoPatientAccountFound': 'Аккаунт пациента не найден',
      'errorNoAdminAccountFound': 'Аккаунт администратора не найден',
      'errorDoctorProfileNotFound': 'Профиль врача не найден',
      'errorEmailOtpRequiredWhenEmailProvided':
          'При указании email требуется код подтверждения (OTP)',
      'errorEmailVerificationCodeRequired': 'Требуется код подтверждения email',
      'errorInvalidOrExpiredEmailVerificationCode':
          'Неверный или просроченный код подтверждения email',
      'errorInvalidPhoneNumber': 'Неверный номер телефона',
      'errorAccountIsDisabled': 'Аккаунт отключён',
      'errorNotADoctorAccount': 'Это не аккаунт врача',
      'errorMissingBearerToken': 'Bearer-токен отсутствует',
      'errorFirebaseVerificationNotConfigured':
          'Верификация Firebase не настроена',
      'errorInvalidOrExpiredToken': 'Неверный или просроченный токен',
      'errorAccessRestrictedToDoctors': 'Доступ разрешён только врачам',
      'errorYourAccountHasBeenBlocked': 'Ваш аккаунт заблокирован',
      'errorInvalidKey': 'Неверный ключ',
      'errorKeyAlreadyUsed': 'Ключ уже использован',
      'errorUsernameRequired': 'Требуется имя пользователя',
      'errorPasswordRequired': 'Требуется пароль',
      'errorInvalidCredentials':
          'Аккаунт с этим номером телефона или email не найден. Проверьте данные или создайте аккаунт.',
      'errorAccountLocked': 'Аккаунт заблокирован',
      'errorInvalidCredentialsPasswordMismatch':
          'Неверный пароль. Попробуйте ещё раз или сбросьте пароль.',
      'errorCreatePatientAccountFirst':
          'Это аккаунт врача. Чтобы войти сюда, нажмите «Создать аккаунт» и создайте аккаунт пациента, связанный с вашим аккаунтом врача.',
      'errorAccessDeniedThisAppRequiresRole':
          'Доступ запрещён: этому приложению требуется роль \${requiredRole.name}',
      'errorPhoneVerificationNotConfigured':
          'Верификация телефона не настроена',
      'errorInvalidOrExpiredPhoneVerification':
          'Неверная или просроченная проверка телефона',
      'errorPhoneNumberNotFoundInVerification':
          'Номер телефона не найден в проверке',
      'errorInvalidPhoneInVerification': 'Неверный телефон в проверке',
      'errorPhoneNumberDoesNotMatchVerification':
          'Номер телефона не совпадает с проверкой',
      'errorEmailAlreadyRegistered': 'Email уже зарегистрирован',
      'errorPhoneAlreadyRegistered': 'Номер телефона уже зарегистрирован',
      'errorPatientWithPhoneAlreadyExists':
          'Пациент с таким номером уже существует',
      'errorPhoneNumberNotFound': 'Номер телефона не найден',
      'errorUserNotFound': 'Пользователь не найден',
      'errorSessionInvalid': 'Сессия недействительна',
      'errorSessionExpiredOrSignedOut': 'Сессия истекла или выполнен выход',
      'errorInvalidToken': 'Неверный токен',
      'errorTooManyRequests': 'Слишком много запросов. Попробуйте позже.',
      'errorPatientProfileNotFoundForUser':
          'Профиль пациента для пользователя \${user.id} не найден. Заполните профиль (телефон или email).',
      'errorAuthenticationRequired': 'Требуется авторизация',
      'errorAppointmentNotFound': 'Приём не найден: {{id}}',
      'errorVideoCallNotYetAvailable':
          'Видеозвонок пока недоступен. Можно подключиться за 5 минут до начала.',
      'errorVideoCallHasEnded':
          'Видеозвонок завершён. Подключение возможно в течение 15 минут после окончания.',
      'errorAppointmentDoesNotBelongToDoctor':
          'Этот приём не относится к данному врачу',
      'errorAppointmentDoesNotHavePatientAssigned':
          'К этому приёму не привязан пациент. Убедитесь, что приём связан с вашим профилем.',
      'errorAppointmentDoesNotBelongToPatient':
          'Этот приём не относится к данному пациенту',
      'errorFailedToRetrievePatientProfile':
          'Не удалось получить профиль пациента: {{detail}}',
      'errorFailedToGetOrCreateRoom':
          'Не удалось получить или создать комнату: {{detail}}',
      'errorUnableToDetermineUserIdentity':
          'Не удалось определить пользователя',
      'errorUnableToDetermineUserName':
          'Не удалось определить имя пользователя',
      'errorUserNameCannotBeBlank': 'Имя пользователя не может быть пустым',
      'errorFailedToGenerateToken': 'Не удалось сгенерировать токен',
      'errorFailedToGenerateTokenDetail':
          'Не удалось сгенерировать токен: {{detail}}',
      'errorFailedToGenerateVideoToken':
          'Не удалось сгенерировать видео-токен: {{detail}}',
      'errorDailyApiKeyNotConfigured':
          'API-ключ Daily.co не настроен. Установите DAILY_API_KEY в Railway.',
      'errorNoTokenReceivedFromServer': 'Токен от сервера не получен',
      'errorNoTokenReceived': 'Токен не получен',
      'errorInvalidResponseFromServer': 'Неверный ответ сервера: {{code}}',
      'errorLoginFailed': 'Вход не выполнен',
      'errorNetworkError': 'Ошибка сети: {{type}}',
      'errorFailedToSendEmailCode': 'Не удалось отправить код email',
      'errorFailedToCreatePatientAccount':
          'Не удалось создать аккаунт пациента',
      'errorRegistrationFailed': 'Регистрация не выполнена',
      'errorFailedToResetPassword': 'Не удалось сбросить пароль',
      'errorFailedToChangePassword': 'Не удалось изменить пароль',
      'errorUnknownError': 'Неизвестная ошибка',
      'errorStatusCode': 'Ошибка {{code}}',
      'errorSomethingWentWrong': 'Что-то пошло не так',
      'errorSessionExpiredPleaseStartAgain':
          'Сессия истекла. Пожалуйста, начните заново.',
      'signatureAlreadySubmitted': 'Подпись уже отправлена',
      'appInitializationError': 'Ошибка инициализации приложения',
      'pleaseRestartApp': 'Пожалуйста, перезапустите приложение',
      'notificationChannelName': 'Уведомления Shifa Patient',
      'notificationChannelDescription':
          'Уведомления о сообщениях, приёмах и задачах',
      'notificationChannelAppointmentsName': 'Уведомления о приёмах',
      'notificationChannelAppointmentsDescription':
          'Уведомления об изменениях приёмов',
    },
  };

  String translate(String key) {
    return _localizedValues[locale.languageCode]?[key] ??
        _localizedValues['en']?[key] ??
        key;
  }

  /// Translates doctor profession/specialty using ProfessionModel (same method as doctor app).
  /// Backend stores profession as English (e.g. "Dentist"), we find the ProfessionModel and return Uzbek if language is 'uz'.
  String translateProfession(String? profession) {
    if (profession == null || profession.trim().isEmpty) {
      return profession ?? '';
    }

    // Find profession model by English name (backend stores English)
    final professionModel = ProfessionData.findByEnglish(profession.trim());
    if (professionModel != null) {
      // Use getDisplayText with current locale language code
      return professionModel.getDisplayText(locale.languageCode);
    }

    // Fallback: if not found in ProfessionData, return original
    return profession;
  }

  // Convenience getters
  String get appName => translate('appName');
  String get notificationChannelName => translate('notificationChannelName');
  String get notificationChannelDescription =>
      translate('notificationChannelDescription');
  String get notificationChannelAppointmentsName =>
      translate('notificationChannelAppointmentsName');
  String get notificationChannelAppointmentsDescription =>
      translate('notificationChannelAppointmentsDescription');
  String get hello => translate('hello');
  String get patient => translate('patient');
  String get loading => translate('loading');
  String get error => translate('error');
  String get retry => translate('retry');
  String get cancel => translate('cancel');
  String get save => translate('save');
  String get delete => translate('delete');
  String get edit => translate('edit');
  String get back => translate('back');
  String get next => translate('next');
  String get complete => translate('complete');
  String get submit => translate('submit');
  String get close => translate('close');
  String get yes => translate('yes');
  String get no => translate('no');

  // Auth
  String get login => translate('login');
  String get signIn => translate('signIn');
  String get createAccount => translate('createAccount');
  String get phoneOrEmail => translate('phoneOrEmail');
  String get password => translate('password');
  String get confirmPassword => translate('confirmPassword');
  String get continueWithGoogle => translate('continueWithGoogle');
  String get forgotPassword => translate('forgotPassword');

  // Profile
  String get profile => translate('profile');
  String get editProfile => translate('editProfile');
  String get language => translate('language');
  String get preferences => translate('preferences');
  String get settingsTitle => translate('settingsTitle');
  String get accountTitle => translate('accountTitle');
  String get privacy => translate('privacy');
  String get deleteAccount => translate('deleteAccount');
  String get logOut => translate('logOut');
  String get signOutConfirm => translate('signOutConfirm');
  String get birthDate => translate('birthDate');
  String? get birthdate => translate('birthdate');
  String get phoneNumber => translate('phoneNumber');
  String get email => translate('email');
  String get address => translate('address');
  String get name => translate('name');
  String? get firstName => translate('firstName');
  String? get lastName => translate('lastName');
  String get surname => translate('surname');
  String get gender => translate('gender');
  String get male => translate('male');
  String get female => translate('female');
  String get other => translate('other');
  String get uploadPhoto => translate('uploadPhoto');
  String get selectLanguage => translate('selectLanguage');
  String languageChanged(String lang) =>
      '${translate('languageChanged')} $lang';
  String get profileUpdatedSuccessfully =>
      translate('profileUpdatedSuccessfully');
  String get failedToUpdateProfile => translate('failedToUpdateProfile');
  String get discardChanges => translate('discardChanges');
  String get saveChanges => translate('saveChanges');

  // Home
  String? get home => translate('home');
  String get upcomingAppointments => translate('upcomingAppointments');
  String get doctorsNearMe => translate('doctorsNearMe');
  String get noUpcomingAppointments => translate('noUpcomingAppointments');
  String get noDoctorsAvailable => translate('noDoctorsAvailable');
  String? get noDoctorsInCloseDistance => translate('noDoctorsInCloseDistance');
  String? get myTasks => translate('myTasks');
  String? get remoteCareTasks => translate('remoteCareTasks');
  String? get viewAndCompleteAssignedTasks =>
      translate('viewAndCompleteAssignedTasks');
  String? get openTasks => translate('openTasks');
  String get viewTasks => translate('viewTasks');
  String get useMyLocation => translate('useMyLocation');
  String? get getCurrentLocation => translate('getCurrentLocation');
  String? get latitude => translate('latitude');
  String? get longitude => translate('longitude');
  String? get noTasksFound => translate('noTasksFound');
  String get starts => translate('starts');
  String get started => translate('started');
  String? get selectLocationOnMap => translate('selectLocationOnMap');
  String? get country => translate('country');
  String? get region => translate('region');
  String? get district => translate('district');
  String? get city => translate('city');
  String? get postalCode => translate('postalCode');
  String? get streetAddress => translate('streetAddress');
  String? get enterStreetAddress => translate('enterStreetAddress');
  String? get streetAddressHelper => translate('streetAddressHelper');

  // Bookings
  String get bookings => translate('bookings');
  String get upcoming => translate('upcoming');
  String get past => translate('past');
  String get createBooking => translate('createBooking');
  String get noAppointmentsFound => translate('noAppointmentsFound');
  String get appointmentDetails => translate('appointmentDetails');
  String get date => translate('date');
  String get dateAndTime => translate('dateAndTime');
  String get location => translate('location');
  String get about => translate('about');
  String get services => translate('services');
  String get certificates => translate('certificates');
  String get contacts => translate('contacts');
  String get reasonForVisit => translate('reasonForVisit');
  String? get selectDate => translate('selectDate');
  String? get availableTimes => translate('availableTimes');
  String? get noAvailableTimeSlots => translate('noAvailableTimeSlots');
  String? get videoConsultation => translate('videoConsultation');
  String? get haveYourAppointment => translate('haveYourAppointment');
  String? get optional => translate('optional');
  String? get describeYourReason => translate('describeYourReason');
  String? get confirm => translate('confirm');
  String? get appointmentSlotBooked => translate('appointmentSlotBooked');
  String get appointmentRescheduledSuccessfully =>
      translate('appointmentRescheduledSuccessfully');
  String get joinVideoCall => translate('joinVideoCall');
  String get leaveReview => translate('leaveReview');
  String? get yourRating => translate('yourRating');
  String? get thankYouForYourRating => translate('thankYouForYourRating');
  String get changeBooking => translate('changeBooking');
  String get cancelBooking => translate('cancelBooking');
  String get cancelAppointment => translate('cancelAppointment');
  String get areYouSureCancel => translate('areYouSureCancel');
  String get appointmentCancelledSuccessfully =>
      translate('appointmentCancelledSuccessfully');
  String get errorCancellingAppointment =>
      translate('errorCancellingAppointment');
  String get contactDoctor => translate('contactDoctor');
  String get callDoctor => translate('callDoctor');
  String get emailDoctor => translate('emailDoctor');
  String get appointmentLessThan48Hours =>
      translate('appointmentLessThan48Hours');
  String get contactDoctorDirectly => translate('contactDoctorDirectly');
  String get signAppointmentSummary => translate('signAppointmentSummary');
  String get appointmentSummaryPreview =>
      translate('appointmentSummaryPreview');
  String get confirmAppointmentSummaryReflectsDiscussion =>
      translate('confirmAppointmentSummaryReflectsDiscussion');
  String get yourSignature => translate('yourSignature');
  String get pleaseSignAbove => translate('pleaseSignAbove');
  String get signatureSubmittedSuccess =>
      translate('signatureSubmittedSuccess');
  String get time => translate('time');
  String get reason => translate('reason');
  String get errorSaving => translate('errorSaving');
  String get clear => translate('clear');

  // Doctors
  String get doctor => translate('doctor');
  String get doctors => translate('doctors');
  String get myDoctors => translate('myDoctors');
  String get recommended => translate('recommended');
  String get sortBy => translate('sortBy');
  String get sortByDistance => translate('sortByDistance');
  String get sortByRating => translate('sortByRating');
  String get sortByReviews => translate('sortByReviews');
  String get filterBy => translate('filterBy');
  String get filterByRegion => translate('filterByRegion');
  String get filterBySpecialty => translate('filterBySpecialty');
  String get allRegions => translate('allRegions');
  String get allSpecialties => translate('allSpecialties');
  String get gettingYourLocation => translate('gettingYourLocation');
  String get usingCurrentLocation => translate('usingCurrentLocation');
  String get couldNotGetLocationUsingProfile =>
      translate('couldNotGetLocationUsingProfile');
  String get bookAppointment => translate('bookAppointment');
  String get reviews => translate('reviews');
  String get noReviews => translate('noReviews');
  String get writeReview => translate('writeReview');
  String get rating => translate('rating');
  String get comment => translate('comment');
  String get commentOptional => translate('commentOptional');
  String get submitReview => translate('submitReview');
  String get shareExperience => translate('shareExperience');
  String? get dentist => translate('dentist');
  String? get checkUp => translate('checkUp');
  String? get cardiologist => translate('cardiologist');
  String get howWasYourExperience => translate('howWasYourExperience');
  String get contactInformation => translate('contactInformation');
  String get specializations => translate('specializations');
  String get furtherInformation => translate('furtherInformation');
  String get noRatingsYet => translate('noRatingsYet');

  // Documents
  String get documents => translate('documents');
  String get searchDocuments => translate('searchDocuments');
  String get upload => translate('upload');
  String get noDocuments => translate('noDocuments');
  String get noDocumentsFound => translate('noDocumentsFound');
  String get documentTitle => translate('documentTitle');
  String get enterDocumentTitle => translate('enterDocumentTitle');
  String get documentUploadedSuccessfully =>
      translate('documentUploadedSuccessfully');
  String get uploadFailed => translate('uploadFailed');
  String get openDocument => translate('openDocument');
  String? get takePhoto => translate('takePhoto');
  String? get chooseFromGallery => translate('chooseFromGallery');
  String? get uploadFile => translate('uploadFile');
  String get addedByDoctor => translate('addedByDoctor');
  String get addedByYou => translate('addedByYou');
  String? get appLock => translate('appLock');
  String? get unlockApp => translate('unlockApp');
  String get unlockShifa => translate('unlockShifa');
  String get enableBiometricForAppSecurity =>
      translate('enableBiometricForAppSecurity');
  String get enableBiometricPrompt => translate('enableBiometricPrompt');
  String get skipBiometric => translate('skipBiometric');
  String? get enterPinOrUseBiometric => translate('enterPinOrUseBiometric');
  String get enterPinToUnlock => translate('enterPinToUnlock');
  String? get useBiometric => translate('useBiometric');
  String? get enableAppLock => translate('enableAppLock');
  String? get appLockEnabled => translate('appLockEnabled');
  String? get appLockDisabled => translate('appLockDisabled');
  String? get pinCode => translate('pinCode');
  String? get setUpPin => translate('setUpPin');
  String? get setUpPinRequired => translate('setUpPinRequired');
  String? get setUp => translate('setUp');
  String? get setUpPinDescription => translate('setUpPinDescription');
  String? get changePin => translate('changePin');
  String? get changePinDescription => translate('changePinDescription');
  String? get clearPin => translate('clearPin');
  String? get clearPinDescription => translate('clearPinDescription');
  String? get clearPinConfirmation => translate('clearPinConfirmation');
  String? get pinSetSuccessfully => translate('pinSetSuccessfully');
  String? get pinChangedSuccessfully => translate('pinChangedSuccessfully');
  String? get pinCleared => translate('pinCleared');
  String? get pinsDoNotMatch => translate('pinsDoNotMatch');
  String? get enterPin => translate('enterPin');
  String? get enterCurrentPin => translate('enterCurrentPin');
  String? get enterNewPin => translate('enterNewPin');
  String? get confirmPin => translate('confirmPin');
  String? get confirmNewPin => translate('confirmNewPin');
  String? get reEnterPin => translate('reEnterPin');
  String? get incorrectPin => translate('incorrectPin');
  String? get forgotPinLogOut => translate('forgotPinLogOut');
  String? get biometricAuthentication => translate('biometricAuthentication');
  String? get enableBiometric => translate('enableBiometric');
  String? get biometricEnabled => translate('biometricEnabled');
  String? get biometricDisabled => translate('biometricDisabled');
  String? get setUpPinFirst => translate('setUpPinFirst');
  String? get enterPinCode => translate('enterPinCode');
  String? get createAPinCode => translate('createAPinCode');

  // Additional
  String? get uploading => translate('uploading');
  String? get photoUploadEndpointNotAvailable =>
      translate('photoUploadEndpointNotAvailable');
  String? get profilePhotoUpdatedSuccessfully =>
      translate('profilePhotoUpdatedSuccessfully');
  String? get failedToGetPhotoUrl => translate('failedToGetPhotoUrl');
  String? get failedToUploadPhoto => translate('failedToUploadPhoto');
  String? get deleteAccountConfirmation =>
      translate('deleteAccountConfirmation');
  String? get deleteAccountComingSoon => translate('deleteAccountComingSoon');
  String? get couldNotReadFileBytes => translate('couldNotReadFileBytes');
  String? get pleaseSelectCheckIn => translate('pleaseSelectCheckIn');
  String? get checkInSubmittedSuccessfully =>
      translate('checkInSubmittedSuccessfully');
  String? get failedToSubmit => translate('failedToSubmit');
  String? get taskCheckIn => translate('taskCheckIn');
  String? get taskNotFound => translate('taskNotFound');
  String? get noPendingCheckIns => translate('noPendingCheckIns');
  String? get taskCheckInNotYetAvailable =>
      translate('taskCheckInNotYetAvailable');
  String? get exampleValue => translate('exampleValue');
  String? get additionalNotesOptional => translate('additionalNotesOptional');
  String? get submitCheckIn => translate('submitCheckIn');

  // Chat
  String get chat => translate('chat');
  String get messages => translate('messages');
  String get searchDoctors => translate('searchDoctors');
  String get typeMessage => translate('typeMessage');
  String get send => translate('send');
  String get noConversations => translate('noConversations');

  // Notifications
  String? get notifications => translate('notifications');
  String? get newAppointmentScheduled => translate('newAppointmentScheduled');
  String? get doctorHasScheduled => translate('doctorHasScheduled');
  String? get markAllAsRead => translate('markAllAsRead');
  String? get allNotificationsMarkedAsRead =>
      translate('allNotificationsMarkedAsRead');
  String? get errorLoadingNotifications =>
      translate('errorLoadingNotifications');
  String? get noNotifications => translate('noNotifications');
  String? get approve => translate('approve');
  String? get reject => translate('reject');
  String? get documentAccessApproved => translate('documentAccessApproved');
  String? get documentAccessRejected => translate('documentAccessRejected');

  // Status
  String get confirmed => translate('confirmed');
  String get cancelled => translate('cancelled');
  String get completed => translate('completed');
  String get pending => translate('pending');

  // Video Call
  String get videoCall => translate('videoCall');
  String get callDuration => translate('callDuration');

  // Account Info
  String get accountInformation => translate('accountInformation');
  String get dateOfBirth => translate('dateOfBirth');

  // Profile Image
  String get profileImage => translate('profileImage');
  String get addProfilePhoto => translate('addProfilePhoto');
  String get addPhotoLater => translate('addPhotoLater');
  String get removePhoto => translate('removePhoto');
  String get completeRegistration => translate('completeRegistration');

  // Registration
  String get step1 => translate('step1');
  String get step2 => translate('step2');
  String get step3 => translate('step3');
  String get accountCreated => translate('accountCreated');
  String get registrationFailed => translate('registrationFailed');

  // Errors
  String get required => translate('required');
  String get passwordsDoNotMatch => translate('passwordsDoNotMatch');
  String get invalidEmail => translate('invalidEmail');
  String get passwordTooShort => translate('passwordTooShort');
  String get minimum6Characters => translate('minimum6Characters');
  String get invalidPhone => translate('invalidPhone');
  String get changePassword => translate('changePassword');
  String get currentPassword => translate('currentPassword');
  String get newPassword => translate('newPassword');
  String get passwordChangedSuccess => translate('passwordChangedSuccess');
}

class _AppLocalizationsDelegate
    extends LocalizationsDelegate<AppLocalizations> {
  const _AppLocalizationsDelegate();

  @override
  bool isSupported(Locale locale) {
    return ['en', 'de', 'uz', 'ru'].contains(locale.languageCode);
  }

  @override
  Future<AppLocalizations> load(Locale locale) async {
    return AppLocalizations(locale);
  }

  @override
  bool shouldReload(_AppLocalizationsDelegate old) => false;
}
