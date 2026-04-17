# Shifa Patient App

Flutter Patient Mobile App for the Shifa healthcare platform.

## ✅ Completed Features

### Core Infrastructure
- ✅ Clean Architecture with feature-based structure
- ✅ Riverpod for state management
- ✅ GoRouter for navigation with 5-tab bottom navigation
- ✅ Theme configuration matching UI design (Teal primary color)
- ✅ API client with Dio
- ✅ Local storage with SharedPreferences
- ✅ File/Image picker support

### Authentication & Onboarding
- ✅ Sign In screen (Phone/Password)
- ✅ Create Account screen (Name, Surname, Phone, Password)
- ✅ Account Information screen (Birth Date, Gender, Address)
- ✅ Profile Image upload screen
- ✅ Google Sign-In UI (stub, needs backend implementation)

### Home Feature
- ✅ Greeting header ("Hello, [Name]")
- ✅ Upcoming appointments card
- ✅ Doctors near me list
- ✅ Navigation to doctor profiles and appointment details

### Doctors Feature
- ✅ My Doctors tab
- ✅ Recommended tab with search
- ✅ Doctor list with profile images, names, clinics
- ✅ Doctor profile screen with:
  - Address information
  - Specializations (tags)
  - Reviews section
  - Book button

### Booking Flow
- ✅ Create Booking screen (search doctors)
- ✅ Appointment booking flow:
  - Select date (calendar)
  - Select time slot
  - Video consultation toggle
  - Reason for visit
- ✅ Confirm booking screen
- ✅ Appointment details screen
- ✅ Join Video Call screen (UI only)
- ✅ Waiting Room screen

### Documents Feature
- ✅ Documents list with search
- ✅ Document details screen
- ✅ Upload button (needs backend integration)
- ✅ Download button (needs backend integration)
- ✅ Delete functionality (needs backend integration)

### Profile/Account Feature
- ✅ Profile overview with personal information
- ✅ Edit profile screen
- ✅ Preferences (stub)
- ✅ Privacy (stub)
- ✅ Delete account (confirmation only)
- ✅ Logout functionality

## 🔧 Backend Adjustments Needed

### Patient Registration Endpoint
Currently uses `/auth/register-patient` which needs to be created in the backend. Should accept:
- firstName, lastName, phone, email (optional), password
- Return JWT token

### Patient Appointment Endpoints
The backend currently has `/api/schedule/book` which is doctor-initiated. Need to add:
- `GET /api/patients/{patientId}/appointments` - List patient's appointments
- `POST /api/patients/{patientId}/appointments` - Book appointment as patient
- `GET /api/patients/{patientId}/appointments/{appointmentId}` - Get appointment details
- `DELETE /api/patients/{patientId}/appointments/{appointmentId}` - Cancel appointment

### Doctor Listing for Patients
Need endpoints:
- `GET /api/doctors` - List all doctors (public)
- `GET /api/doctors/{doctorId}` - Get doctor profile (public)
- `GET /api/doctors/{doctorId}/available-slots` - Get available time slots for booking

### Patient Documents
Already exists at `/api/patients/{patientId}/documents`, but may need:
- `DELETE /api/patients/{patientId}/documents/{documentId}` - Delete document
- File download endpoint adjustments

## 📱 UI Design Compliance

The app follows the provided UI designs with:
- ✅ Teal primary color (#26C6DA)
- ✅ Rounded cards with soft shadows
- ✅ Bottom navigation with 5 tabs: Home, Bookings, Documents, Doctors, Account
- ✅ Consistent spacing and typography
- ✅ Primary (filled) and Secondary (outlined) button styles

## 🚀 Running the App

```bash
cd shifa_patient_app_v1
flutter pub get
flutter run -d ios
```

## 📝 Notes

- Video calling is UI-only (no real streaming implementation)
- Google Sign-In is UI stub (needs backend OAuth implementation)
- Some screens use placeholder/mock data - needs backend integration
- iOS minimum version: iOS 14+
- Backend base URL configured in `lib/core/network/api_client.dart` (currently `http://localhost:8080/api`)

## 🔄 Next Steps

1. Adjust backend endpoints for patient-specific operations
2. Implement real video calling integration (if needed)
3. Connect all screens to actual backend APIs
4. Add error handling and loading states
5. Implement push notifications (if needed)
6. Add form validations and error messages
7. Implement reviews/ratings backend if needed
