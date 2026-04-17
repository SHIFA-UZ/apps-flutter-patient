import 'package:equatable/equatable.dart';

class UserModel extends Equatable {
  final String id;
  final String? email;
  final String? phone;
  final String firstName;
  final String lastName;
  final String? fullName;
  final String? photoUrl;
  final String? birthDate;
  final String? gender;
  final String? address;
  final String? language;
  final String role;

  const UserModel({
    required this.id,
    this.email,
    this.phone,
    required this.firstName,
    required this.lastName,
    this.fullName,
    this.photoUrl,
    this.birthDate,
    this.gender,
    this.address,
    this.language,
    required this.role,
  });

  factory UserModel.fromJson(Map<String, dynamic> json) {
    return UserModel(
      id: json['id']?.toString() ?? '',
      email: json['email'] as String?,
      phone: json['phone'] as String?,
      firstName: json['firstName'] ?? json['fullName']?.split(' ').first ?? '',
      lastName: json['lastName'] ?? json['fullName']?.split(' ').skip(1).join(' ') ?? '',
      fullName: json['fullName'] ?? '${json['firstName'] ?? ''} ${json['lastName'] ?? ''}'.trim(),
      photoUrl: json['photoUrl'] as String?,
      birthDate: json['birthDate'] as String?,
      gender: json['gender'] as String?,
      address: json['address'] as String?,
      language: json['language'] as String?,
      role: json['role'] ?? 'PATIENT',
    );
  }

  Map<String, dynamic> toJson() {
    return {
      'id': id,
      'email': email,
      'phone': phone,
      'firstName': firstName,
      'lastName': lastName,
      'fullName': fullName ?? '$firstName $lastName',
      'photoUrl': photoUrl,
      'birthDate': birthDate,
      'gender': gender,
      'address': address,
      'language': language,
      'role': role,
    };
  }

  @override
  List<Object?> get props => [
        id,
        email,
        phone,
        firstName,
        lastName,
        fullName,
        photoUrl,
        birthDate,
        gender,
        address,
        language,
        role,
      ];
}
