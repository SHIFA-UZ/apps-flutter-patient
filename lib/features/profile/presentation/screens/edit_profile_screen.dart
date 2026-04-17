import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';
import 'package:image_picker/image_picker.dart';
import 'package:intl/intl.dart';
import 'package:shifa_patient_app_v1/core/localization/app_localizations.dart';
import 'package:shifa_patient_app_v1/core/widgets/phone_input_field.dart';
import 'package:shifa_patient_app_v1/core/widgets/shifa_button.dart';
import 'package:shifa_patient_app_v1/features/profile/providers/profile_provider.dart';
import 'package:shifa_patient_app_v1/features/profile/presentation/location_picker_widget.dart';

class EditProfileScreen extends ConsumerStatefulWidget {
  const EditProfileScreen({super.key});

  @override
  ConsumerState<EditProfileScreen> createState() => _EditProfileScreenState();
}

class _EditProfileScreenState extends ConsumerState<EditProfileScreen> {
  final _formKey = GlobalKey<FormState>();
  final _firstNameController = TextEditingController();
  final _lastNameController = TextEditingController();
  final _birthDateController = TextEditingController();
  final _addressController = TextEditingController();
  final _languageController = TextEditingController();

  String _phoneValue = '';

  DateTime? _selectedBirthDate;
  String? _selectedPhotoUrl;
  bool _isSaving = false;
  
  // Structured location data
  double? _selectedLatitude;
  double? _selectedLongitude;
  String? _locationCountry;
  String? _locationRegion;
  String? _locationDistrict;
  String? _locationCity;
  String? _locationPostalCode;
  String? _locationStreetAddress;

  @override
  void initState() {
    super.initState();
    // Load profile data and populate fields
    WidgetsBinding.instance.addPostFrameCallback((_) {
      _loadProfileData();
    });
  }

  void _loadProfileData() {
    final profileState = ref.read(profileProvider);
    final profile = profileState.profile;

    if (profile != null) {
      // Split fullName into firstName and lastName
      final nameParts = profile.fullName.split(' ');
      _firstNameController.text = nameParts.isNotEmpty ? nameParts.first : '';
      _lastNameController.text = nameParts.length > 1 ? nameParts.sublist(1).join(' ') : '';

      // Parse and set birth date
      if (profile.birthDate != null && profile.birthDate!.isNotEmpty) {
        try {
          _selectedBirthDate = DateTime.parse(profile.birthDate!);
          _birthDateController.text = DateFormat('dd.MM.yyyy').format(_selectedBirthDate!);
        } catch (e) {
          _birthDateController.text = profile.birthDate!;
        }
      }

      _addressController.text = profile.address ?? '';
      _languageController.text = profile.language ?? '';
      _phoneValue = profile.phone ?? '';
      _selectedPhotoUrl = profile.photoUrl;
      _selectedLatitude = profile.latitude;
      _selectedLongitude = profile.longitude;
      _locationCountry = profile.locationCountry;
      _locationRegion = profile.locationRegion;
      _locationDistrict = profile.locationDistrict;
      _locationCity = profile.locationCity;
      _locationPostalCode = profile.locationPostalCode;
      _locationStreetAddress = profile.locationStreetAddress;
      if (mounted) setState(() {});
    }
  }

  @override
  void dispose() {
    _firstNameController.dispose();
    _lastNameController.dispose();
    _birthDateController.dispose();
    _addressController.dispose();
    _languageController.dispose();
    super.dispose();
  }

  Future<void> _selectBirthDate(BuildContext context) async {
    final picked = await showDatePicker(
      context: context,
      initialDate: _selectedBirthDate ?? DateTime.now().subtract(const Duration(days: 365 * 18)),
      firstDate: DateTime(1900),
      lastDate: DateTime.now(),
    );
    if (picked != null && picked != _selectedBirthDate) {
      setState(() {
        _selectedBirthDate = picked;
        _birthDateController.text = DateFormat('dd.MM.yyyy').format(picked);
      });
    }
  }

  Future<void> _pickImage() async {
    final l10n = AppLocalizations.of(context)!;
    final picker = ImagePicker();
    final pickedFile = await picker.pickImage(source: ImageSource.gallery);
    
    if (pickedFile != null) {
      // TODO: Upload image to backend and get URL
      // For now, just store the local path
      setState(() {
        _selectedPhotoUrl = pickedFile.path;
      });
      
      ScaffoldMessenger.of(context).showSnackBar(
        SnackBar(
          content: Text(l10n.translate('imageUploadComingSoon')),
        ),
      );
    }
  }

  Future<void> _saveProfile() async {
    final l10n = AppLocalizations.of(context)!;
    if (!_formKey.currentState!.validate()) {
      return;
    }

    setState(() {
      _isSaving = true;
    });

    try {
      // Format birth date for backend (yyyy-MM-dd)
      String? birthDateFormatted;
      if (_selectedBirthDate != null) {
        birthDateFormatted = DateFormat('yyyy-MM-dd').format(_selectedBirthDate!);
      }

      await ref.read(profileProvider.notifier).updateProfile(
        firstName: _firstNameController.text.trim().isNotEmpty 
            ? _firstNameController.text.trim() 
            : null,
        lastName: _lastNameController.text.trim().isNotEmpty 
            ? _lastNameController.text.trim() 
            : null,
        birthDate: birthDateFormatted,
        phone: _phoneValue.trim().isNotEmpty ? _phoneValue.trim() : null,
        address: _addressController.text.trim().isNotEmpty 
            ? _addressController.text.trim() 
            : null,
        language: _languageController.text.trim().isNotEmpty 
            ? _languageController.text.trim() 
            : null,
        photoUrl: _selectedPhotoUrl,
        latitude: _selectedLatitude,
        longitude: _selectedLongitude,
        locationCountry: _locationCountry,
        locationRegion: _locationRegion,
        locationDistrict: _locationDistrict,
        locationCity: _locationCity,
        locationPostalCode: _locationPostalCode,
        locationStreetAddress: _locationStreetAddress,
      );

      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
          SnackBar(
            content: Text(l10n.profileUpdatedSuccessfully),
            backgroundColor: Colors.green,
          ),
        );
        context.pop();
      }
    } catch (e) {
      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
          SnackBar(
            content: Text('${l10n.failedToUpdateProfile}: $e'),
            backgroundColor: Colors.red,
          ),
        );
      }
    } finally {
      if (mounted) {
        setState(() {
          _isSaving = false;
        });
      }
    }
  }

  @override
  Widget build(BuildContext context) {
    final l10n = AppLocalizations.of(context)!;
    final profileState = ref.watch(profileProvider);
    final profile = profileState.profile;

    return Scaffold(
      appBar: AppBar(
        leading: IconButton(
          icon: const Icon(Icons.arrow_back),
          onPressed: _isSaving ? null : () => context.pop(),
        ),
        title: Text(l10n.editProfile),
      ),
      body: profileState.isLoading && profile == null
          ? const Center(child: CircularProgressIndicator())
          : SingleChildScrollView(
              padding: const EdgeInsets.all(16.0),
              child: Form(
                key: _formKey,
                child: Column(
                  children: [
                    // Profile Photo
                    Stack(
                      children: [
                        CircleAvatar(
                          radius: 60,
                          backgroundImage: _selectedPhotoUrl != null && 
                                  _selectedPhotoUrl!.isNotEmpty &&
                                  _selectedPhotoUrl!.startsWith('http')
                              ? NetworkImage(_selectedPhotoUrl!)
                              : null,
                          child: _selectedPhotoUrl == null || 
                                 _selectedPhotoUrl!.isEmpty ||
                                 !_selectedPhotoUrl!.startsWith('http')
                              ? const Icon(Icons.person, size: 60, color: Colors.white)
                              : null,
                          backgroundColor: const Color(0xFF17C3B2),
                        ),
                        Positioned(
                          bottom: 0,
                          right: 0,
                          child: CircleAvatar(
                            radius: 20,
                            backgroundColor: const Color(0xFF17C3B2),
                            child: IconButton(
                              icon: const Icon(Icons.camera_alt, size: 20, color: Colors.white),
                              onPressed: _pickImage,
                              padding: EdgeInsets.zero,
                            ),
                          ),
                        ),
                      ],
                    ),
                    const SizedBox(height: 16),
                    // Name (read-only, shown for reference)
                    if (profile != null)
                      Text(
                        profile.fullName,
                        style: const TextStyle(fontSize: 24, fontWeight: FontWeight.bold),
                      ),
                    const SizedBox(height: 32),
                    // First Name
                    TextFormField(
                      controller: _firstNameController,
                      decoration: InputDecoration(
                        labelText: l10n.firstName,
                        border: const OutlineInputBorder(),
                      ),
                      textCapitalization: TextCapitalization.words,
                    ),
                    const SizedBox(height: 16),
                    // Last Name
                    TextFormField(
                      controller: _lastNameController,
                      decoration: InputDecoration(
                        labelText: l10n.lastName,
                        border: const OutlineInputBorder(),
                      ),
                      textCapitalization: TextCapitalization.words,
                    ),
                    const SizedBox(height: 16),
                    // Phone (country code + number)
                    PhoneInputField(
                      initialFullPhone: _phoneValue.isNotEmpty ? _phoneValue : null,
                      labelText: l10n.phoneNumber,
                      onChanged: (fullPhone) => setState(() => _phoneValue = fullPhone),
                      validator: (value) {
                        if (value != null && value.isNotEmpty) {
                          final digits = value.replaceAll(RegExp(r'\D'), '');
                          if (digits.length < 10) return l10n.invalidPhone;
                        }
                        return null;
                      },
                    ),
                    const SizedBox(height: 16),
                    // Birth Date
                    TextFormField(
                      controller: _birthDateController,
                      decoration: InputDecoration(
                        labelText: l10n.birthDate,
                        border: const OutlineInputBorder(),
                        suffixIcon: const Icon(Icons.calendar_today),
                      ),
                      readOnly: true,
                      onTap: () => _selectBirthDate(context),
                    ),
                    const SizedBox(height: 16),
                    // Address
                    TextFormField(
                      controller: _addressController,
                      decoration: InputDecoration(
                        labelText: l10n.address,
                        border: const OutlineInputBorder(),
                      ),
                      maxLines: 2,
                    ),
                    const SizedBox(height: 16),
                    // Structured Location picker
                    LocationPickerSection(
                      latitude: _selectedLatitude ?? profile?.latitude,
                      longitude: _selectedLongitude ?? profile?.longitude,
                      locationCountry: _locationCountry ?? profile?.locationCountry,
                      locationRegion: _locationRegion ?? profile?.locationRegion,
                      locationDistrict: _locationDistrict ?? profile?.locationDistrict,
                      locationCity: _locationCity ?? profile?.locationCity,
                      locationPostalCode: _locationPostalCode ?? profile?.locationPostalCode,
                      locationStreetAddress: _locationStreetAddress ?? profile?.locationStreetAddress,
                      onLocationSelected: (locationData) {
                        setState(() {
                          _selectedLatitude = locationData['latitude'] as double?;
                          _selectedLongitude = locationData['longitude'] as double?;
                          _locationCountry = locationData['locationCountry'] as String?;
                          _locationRegion = locationData['locationRegion'] as String?;
                          _locationDistrict = locationData['locationDistrict'] as String?;
                          _locationCity = locationData['locationCity'] as String?;
                          _locationPostalCode = locationData['locationPostalCode'] as String?;
                          _locationStreetAddress = locationData['locationStreetAddress'] as String?;
                          
                          // Populate legacy address field with formatted address
                          final formattedAddress = locationData['address'] as String?;
                          if (formattedAddress != null && formattedAddress.isNotEmpty) {
                            _addressController.text = formattedAddress;
                          }
                        });
                      },
                    ),
                    const SizedBox(height: 16),
                    // Language
                    TextFormField(
                      controller: _languageController,
                      decoration: InputDecoration(
                        labelText: l10n.language,
                        border: const OutlineInputBorder(),
                      ),
                    ),
                    const SizedBox(height: 32),
                    // Discard Button
                    ShifaSecondaryButton(
                      label: l10n.discardChanges,
                      onPressed: _isSaving ? null : () => context.pop(),
                      destructive: true,
                    ),
                    const SizedBox(height: 12),
                    // Save Button
                    ShifaPrimaryButton(
                      label: l10n.saveChanges,
                      onPressed: _isSaving ? null : _saveProfile,
                      isLoading: _isSaving,
                    ),
                  ],
                ),
              ),
            ),
    );
  }
}
