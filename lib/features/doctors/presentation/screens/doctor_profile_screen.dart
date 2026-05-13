import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';
import 'package:intl/intl.dart';
import 'package:url_launcher/url_launcher.dart';
import 'dart:io' show Platform;
import 'package:shifa_patient_app_v1/core/localization/app_localizations.dart';
import 'package:shifa_patient_app_v1/core/widgets/shifa_button.dart';
import 'package:shifa_patient_app_v1/core/models/doctor_model.dart';
import 'package:shifa_patient_app_v1/core/utils/image_utils.dart';
import 'package:shifa_patient_app_v1/features/doctors/providers/doctors_provider.dart';
import 'package:shifa_patient_app_v1/features/doctors/providers/reviews_provider.dart';

class DoctorProfileScreen extends ConsumerStatefulWidget {
  final String doctorId;

  const DoctorProfileScreen({super.key, required this.doctorId});

  @override
  ConsumerState<DoctorProfileScreen> createState() => _DoctorProfileScreenState();
}

class _DoctorProfileScreenState extends ConsumerState<DoctorProfileScreen> with SingleTickerProviderStateMixin {
  DoctorModel? _doctor;
  bool _isLoading = true;
  String? _error;
  late TabController _tabController;

  @override
  void initState() {
    super.initState();
    _tabController = TabController(length: 4, vsync: this);
    _loadDoctor();
    // Load reviews
    WidgetsBinding.instance.addPostFrameCallback((_) {
      ref.read(reviewsProvider(widget.doctorId).notifier).loadReviews(widget.doctorId);
    });
  }

  @override
  void dispose() {
    _tabController.dispose();
    super.dispose();
  }

  Future<void> _loadDoctor() async {
    setState(() {
      _isLoading = true;
      _error = null;
    });

    try {
      final doctor = await ref.read(doctorsProvider.notifier).getDoctorById(widget.doctorId);
      setState(() {
        _doctor = doctor;
        _isLoading = false;
      });
    } catch (e) {
      setState(() {
        _error = e.toString();
        _isLoading = false;
      });
    }
  }

  @override
  Widget build(BuildContext context) {
    final l10n = AppLocalizations.of(context)!;
    final reviewsState = ref.watch(reviewsProvider(widget.doctorId));

    return Scaffold(
      appBar: AppBar(
        leading: IconButton(
          icon: const Icon(Icons.arrow_back),
          onPressed: () => context.pop(),
        ),
        title: Text('${l10n.doctors} ${l10n.profile}'),
      ),
      body: _isLoading
          ? const Center(child: CircularProgressIndicator())
          : _error != null
              ? Center(
                  child: Column(
                    mainAxisAlignment: MainAxisAlignment.center,
                    children: [
                      Text(
                        '${l10n.error}: $_error',
                        style: const TextStyle(color: Colors.red),
                        textAlign: TextAlign.center,
                      ),
                      const SizedBox(height: 16),
                      ShifaPrimaryButton(
                        label: l10n.retry,
                        onPressed: _loadDoctor,
                        width: ButtonWidth.hug,
                      ),
                    ],
                  ),
                )
              : _doctor == null
                  ? Center(child: Text('${l10n.doctors} ${l10n.error.toLowerCase()}'))
                  : RefreshIndicator(
                      onRefresh: _loadDoctor,
                      child: SingleChildScrollView(
                      padding: const EdgeInsets.all(16.0),
                      child: Column(
                        children: [
                          // Doctor info
                          CircleAvatar(
                            radius: 60,
                            backgroundImage: _doctor!.photoUrl != null
                                ? NetworkImage(normalizePhotoUrl(_doctor!.photoUrl!) ?? _doctor!.photoUrl!)
                                : null,
                            child: _doctor!.photoUrl == null
                                ? Text(
                                    _doctor!.firstName.isNotEmpty
                                        ? _doctor!.firstName[0].toUpperCase()
                                        : 'D',
                                    style: const TextStyle(fontSize: 40),
                                  )
                                : null,
                          ),
                          const SizedBox(height: 16),
                          Text(
                            _doctor!.fullName,
                            style: const TextStyle(fontSize: 24, fontWeight: FontWeight.bold),
                          ),
                          const SizedBox(height: 8),
                          Builder(
                            builder: (context) {
                              final raw = (_doctor!.profession != null && _doctor!.profession!.trim().isNotEmpty)
                                  ? _doctor!.profession!
                                  : (_doctor!.specializations != null && _doctor!.specializations!.isNotEmpty
                                      ? _doctor!.specializations!.join(', ')
                                      : _doctor!.clinic);
                              if (raw == null || raw.isEmpty) {
                                return const SizedBox.shrink();
                              }
                              final profession = AppLocalizations.of(context)!.translateProfession(raw);
                              return Text(
                                profession,
                                style: const TextStyle(color: Colors.grey, fontSize: 16),
                                textAlign: TextAlign.center,
                              );
                            },
                          ),
                          // Location row: City, Region (e.g. Tashkent, Yunusabad)
                          if (_getHeaderLocationLine() != null) ...[
                            const SizedBox(height: 6),
                            Row(
                              mainAxisAlignment: MainAxisAlignment.center,
                              children: [
                                Icon(
                                  Icons.location_on,
                                  size: 18,
                                  color: const Color(0xFF616161),
                                ),
                                const SizedBox(width: 6),
                                Flexible(
                                  child: Text(
                                    _getHeaderLocationLine()!,
                                    style: const TextStyle(
                                      color: Color(0xFF616161),
                                      fontSize: 15,
                                      fontWeight: FontWeight.w500,
                                    ),
                                    textAlign: TextAlign.center,
                                    overflow: TextOverflow.ellipsis,
                                    maxLines: 1,
                                  ),
                                ),
                              ],
                            ),
                            const SizedBox(height: 6),
                          ],
                          const SizedBox(height: 8),
                          // Rating display
                          if (_doctor!.rating != null)
                            Row(
                              mainAxisAlignment: MainAxisAlignment.center,
                              children: [
                                ...List.generate(5, (index) {
                                  return Icon(
                                    Icons.star,
                                    color: index < _doctor!.rating!.round()
                                        ? Colors.amber
                                        : Colors.grey,
                                    size: 20,
                                  );
                                }),
                                const SizedBox(width: 8),
                                Text(
                                  '${_doctor!.rating!.toStringAsFixed(1)}',
                                  style: const TextStyle(
                                    fontSize: 16,
                                    fontWeight: FontWeight.bold,
                                  ),
                                ),
                                if (_doctor!.reviewCount != null && _doctor!.reviewCount! > 0) ...[
                                  const SizedBox(width: 4),
                                  Text(
                                    '(${_doctor!.reviewCount} ${l10n.reviews})',
                                    style: const TextStyle(
                                      fontSize: 14,
                                      color: Colors.grey,
                                    ),
                                  ),
                                ],
                              ],
                            )
                          else
                            Text(
                              l10n.translate('noRatingsYet'),
                              style: const TextStyle(color: Colors.grey),
                            ),
                          const SizedBox(height: 32),
                          // Book Appointment Button (outside reviews section)
                          ShifaPrimaryButton(
                            label: l10n.bookAppointment,
                            onPressed: () {
                              context.push('/bookings/flow/${widget.doctorId}');
                            },
                          ),
                          const SizedBox(height: 24),
                          // Multi-tab card and Reviews - equal height and scrollable
                          LayoutBuilder(
                            builder: (context, constraints) {
                              final isWide = constraints.maxWidth > 600;
                              final cardHeight = isWide ? 500.0 : 400.0;
                              
                              if (isWide) {
                                // Side-by-side on wide screens
                                return SizedBox(
                                  height: cardHeight,
                                  child: Row(
                                    crossAxisAlignment: CrossAxisAlignment.start,
                                    children: [
                                      Expanded(
                                        child: _buildTabCard(),
                                      ),
                                      const SizedBox(width: 16),
                                      Expanded(
                                        child: _buildReviewsCard(reviewsState, l10n),
                                      ),
                                    ],
                                  ),
                                );
                              } else {
                                // Stacked on mobile
                                return Column(
                                  children: [
                                    SizedBox(
                                      height: cardHeight,
                                      child: _buildTabCard(),
                                    ),
                                    const SizedBox(height: 16),
                                    SizedBox(
                                      height: cardHeight,
                                      child: _buildReviewsCard(reviewsState, l10n),
                                    ),
                                  ],
                                );
                              }
                            },
                          ),
                        ],
                      ),
                      ),
                    ),
    );
  }

  Widget _buildTabCard() {
    final l10n = AppLocalizations.of(context)!;
    return Card(
      child: Column(
        children: [
          TabBar(
            controller: _tabController,
            isScrollable: true,
            labelColor: const Color(0xFF17C3B2),
            unselectedLabelColor: Colors.grey,
            indicatorColor: const Color(0xFF17C3B2),
            tabs: [
              Tab(text: l10n.about),
              Tab(text: l10n.services),
              Tab(text: l10n.certificates),
              Tab(text: l10n.contacts),
            ],
          ),
          Expanded(
            child: TabBarView(
              controller: _tabController,
              children: [
                _buildAboutTab(),
                _buildServicesTab(),
                _buildCertificatesTab(),
                _buildContactTab(),
              ],
            ),
          ),
        ],
      ),
    );
  }

  Widget _buildReviewsCard(reviewsState, l10n) {
    return Card(
      child: Padding(
        padding: const EdgeInsets.all(16.0),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Text(
              l10n.reviews,
              style: const TextStyle(fontWeight: FontWeight.bold, fontSize: 16),
            ),
            const SizedBox(height: 16),
            Expanded(
              child: reviewsState.isLoading
                  ? const Center(child: CircularProgressIndicator())
                  : reviewsState.error != null
                      ? Center(
                          child: Text(
                            '${l10n.translate('errorLoadingReviews')}: ${reviewsState.error}',
                            style: const TextStyle(color: Colors.red),
                          ),
                        )
                      : reviewsState.reviews.isEmpty
                          ? Center(
                              child: Text(
                                l10n.noReviews,
                                style: const TextStyle(color: Colors.grey),
                              ),
                            )
                          : ListView(
                              children: reviewsState.reviews.map<Widget>((review) => _buildReviewItem(review)).toList(),
                            ),
            ),
          ],
        ),
      ),
    );
  }

  Widget _buildAboutTab() {
    return SingleChildScrollView(
      padding: const EdgeInsets.all(16.0),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          if (_doctor!.biography != null && _doctor!.biography!.isNotEmpty) ...[
            Text(
              _doctor!.biography!,
              style: const TextStyle(fontSize: 14),
            ),
          ] else if (_doctor!.furtherInformation != null && _doctor!.furtherInformation!.isNotEmpty) ...[
            Text(
              _doctor!.furtherInformation!,
              style: const TextStyle(fontSize: 14),
            ),
          ] else ...[
            Center(
              child: Padding(
                padding: const EdgeInsets.all(32.0),
                child: Text(
                  AppLocalizations.of(context)!.translate('noBiographyAvailable'),
                  style: const TextStyle(color: Colors.grey),
                ),
              ),
            ),
          ],
        ],
      ),
    );
  }

  Widget _buildServicesTab() {
    final serviceItems = _doctor!.serviceItems ?? const [];
    if (serviceItems.isNotEmpty) {
      return GridView.builder(
        padding: const EdgeInsets.all(16),
        gridDelegate: const SliverGridDelegateWithFixedCrossAxisCount(
          crossAxisCount: 2,
          crossAxisSpacing: 12,
          mainAxisSpacing: 12,
          childAspectRatio: 1.25,
        ),
        itemCount: serviceItems.length,
        itemBuilder: (context, index) {
          final s = serviceItems[index];
          final priceLabel = s.isFreeConsultation
              ? AppLocalizations.of(context)!.translate('consultationServiceFreeBadge')
              : s.teaserPriceLabel(
                  priceNotSetLabel: AppLocalizations.of(context)!.translate('priceNotSet'),
                );
          return InkWell(
            onTap: () {
              showModalBottomSheet<void>(
                context: context,
                isScrollControlled: true,
                builder: (_) => Padding(
                  padding: const EdgeInsets.all(16),
                  child: Column(
                    mainAxisSize: MainAxisSize.min,
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: [
                      if ((s.groupName ?? '').trim().isNotEmpty) ...[
                        Text(
                          s.groupName!,
                          style: TextStyle(
                            fontSize: 12,
                            color: Theme.of(context).colorScheme.primary,
                            fontWeight: FontWeight.w600,
                          ),
                        ),
                        const SizedBox(height: 4),
                      ],
                      const SizedBox(height: 8),
                      if ((s.description ?? '').trim().isNotEmpty)
                        Text(s.description!),
                      const SizedBox(height: 12),
                      ...s.prices.map((p) => Padding(
                            padding: const EdgeInsets.only(bottom: 6),
                            child: Text(
                              '${(p.amountMinor / 100).toStringAsFixed(2)} ${p.currency}'
                              '${p.locationId == null ? '' : ' · Location #${p.locationId}'}',
                            ),
                          )),
                    ],
                  ),
                ),
              );
            },
            child: Card(
              elevation: 2,
              shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(12)),
              child: Padding(
                padding: const EdgeInsets.all(12),
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    const Icon(Icons.medical_services, color: Color(0xFF17C3B2), size: 22),
                    const SizedBox(height: 8),
                    Text(s.title, maxLines: 2, overflow: TextOverflow.ellipsis, style: const TextStyle(fontWeight: FontWeight.w600)),
                    const Spacer(),
                    Text(priceLabel, style: const TextStyle(color: Color(0xFF17C3B2), fontWeight: FontWeight.w700)),
                  ],
                ),
              ),
            ),
          );
        },
      );
    }

    if (_doctor!.services == null || _doctor!.services!.isEmpty) {
      return Center(
        child: Padding(
          padding: const EdgeInsets.all(32.0),
          child: Text(
            AppLocalizations.of(context)!.translate('noServicesAvailable'),
            style: const TextStyle(color: Colors.grey),
          ),
        ),
      );
    }

    return SingleChildScrollView(
      padding: const EdgeInsets.all(16.0),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.stretch,
        children: _doctor!.services!.map<Widget>((service) {
          return Card(
            margin: const EdgeInsets.only(bottom: 12),
            elevation: 2,
            shape: RoundedRectangleBorder(
              borderRadius: BorderRadius.circular(12),
            ),
            child: Padding(
              padding: const EdgeInsets.all(16.0),
              child: Row(
                children: [
                  const Icon(
                    Icons.medical_services,
                    color: Color(0xFF17C3B2),
                    size: 24,
                  ),
                  const SizedBox(width: 12),
                  Expanded(
                    child: Text(
                      service,
                      style: const TextStyle(
                        fontSize: 14,
                        fontWeight: FontWeight.w500,
                      ),
                    ),
                  ),
                ],
              ),
            ),
          );
        }).toList(),
      ),
    );
  }

  Widget _buildCertificatesTab() {
    if (_doctor!.certificates == null || _doctor!.certificates!.isEmpty) {
      return Center(
        child: Padding(
          padding: const EdgeInsets.all(32.0),
          child: Text(
            AppLocalizations.of(context)!.translate('noCertificatesAvailable'),
            style: const TextStyle(color: Colors.grey),
          ),
        ),
      );
    }
    final l10n = AppLocalizations.of(context)!;
    return SingleChildScrollView(
      padding: const EdgeInsets.all(16.0),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.stretch,
        children: _doctor!.certificates!.map<Widget>((certUrl) {
          return Card(
            margin: const EdgeInsets.only(bottom: 12),
            elevation: 2,
            shape: RoundedRectangleBorder(
              borderRadius: BorderRadius.circular(12),
            ),
            child: InkWell(
              onTap: () async {
                final uri = Uri.parse(certUrl);
                if (await canLaunchUrl(uri)) {
                  await launchUrl(uri, mode: LaunchMode.externalApplication);
                }
              },
              borderRadius: BorderRadius.circular(12),
              child: Padding(
                padding: const EdgeInsets.all(16.0),
                child: Row(
                  children: [
                    const Icon(
                      Icons.description,
                      color: Color(0xFF17C3B2),
                      size: 24,
                    ),
                    const SizedBox(width: 12),
                    Expanded(
                      child: Column(
                        crossAxisAlignment: CrossAxisAlignment.start,
                        children: [
                          Text(
                            l10n.translate('certificate'),
                            style: const TextStyle(
                              fontSize: 14,
                              fontWeight: FontWeight.w500,
                            ),
                          ),
                          const SizedBox(height: 4),
                          Text(
                            certUrl.split('/').last,
                            style: const TextStyle(
                              fontSize: 12,
                              color: Colors.grey,
                            ),
                            maxLines: 1,
                            overflow: TextOverflow.ellipsis,
                          ),
                        ],
                      ),
                    ),
                    const Icon(
                      Icons.open_in_new,
                      size: 20,
                      color: Colors.grey,
                    ),
                  ],
                ),
              ),
            ),
          );
        }).toList(),
      ),
    );
  }

  /// Location line for header: "City, Region" (e.g. Tashkent, Yunusabad). Only region/city, no street.
  String? _getHeaderLocationLine() {
    final city = _doctor!.city?.trim();
    final region = _doctor!.region?.trim();
    final hasCity = city != null && city.isNotEmpty;
    final hasRegion = region != null && region.isNotEmpty;
    if (hasCity && hasRegion) return '$city, $region';
    if (hasCity) return city;
    if (hasRegion) return region;
    // Fallback: show first part of address if no structured location (e.g. "Tashkent" from "Tashkent, Uzbekistan")
    final addr = _doctor!.address?.trim();
    if (addr != null && addr.isNotEmpty) {
      final firstPart = addr.contains(',') ? addr.split(',').first.trim() : addr;
      if (firstPart.isNotEmpty) return firstPart;
    }
    return null;
  }

  /// Formats the full address from street, city, and region
  String? _getFormattedAddress() {
    final parts = <String>[];
    if (_doctor!.street != null && _doctor!.street!.trim().isNotEmpty) {
      parts.add(_doctor!.street!.trim());
    }
    if (_doctor!.city != null && _doctor!.city!.trim().isNotEmpty) {
      parts.add(_doctor!.city!.trim());
    }
    if (_doctor!.region != null && _doctor!.region!.trim().isNotEmpty) {
      parts.add(_doctor!.region!.trim());
    }
    
    // Fallback to legacy address field if new fields are not available
    if (parts.isEmpty && _doctor!.address != null && _doctor!.address!.trim().isNotEmpty) {
      return _doctor!.address!.trim();
    }
    
    return parts.isEmpty ? null : parts.join(', ');
  }

  /// Opens map application: prefers coordinates when available, else address string.
  Future<void> _openMap(String address) async {
    final l10n = AppLocalizations.of(context)!;
    final lat = _doctor!.latitude;
    final lng = _doctor!.longitude;
    final hasCoords = lat != null && lng != null;

    try {
      // 1) Native deep links first (best UX)
      if (Platform.isAndroid) {
        // Android: geo:lat,lng?q=encoded_address (prefer coords when available)
        final geoUri = hasCoords
            ? Uri.parse('geo:$lat,$lng?q=${Uri.encodeComponent(address)}')
            : Uri.parse('geo:0,0?q=${Uri.encodeComponent(address)}');

        final ok = await launchUrl(geoUri, mode: LaunchMode.externalApplication);
        if (ok) return;
      } else if (Platform.isIOS) {
        // iOS: Apple Maps query (coords if available, else address)
        final appleMapsUri = hasCoords
            ? Uri.parse('http://maps.apple.com/?ll=$lat,$lng&q=${Uri.encodeComponent(address)}')
            : Uri.parse('http://maps.apple.com/?q=${Uri.encodeComponent(address)}');

        final ok = await launchUrl(appleMapsUri, mode: LaunchMode.externalApplication);
        if (ok) return;
      }

      // 2) Cross-platform fallback: Google Maps HTTPS
      final googleMapsUrl = hasCoords
          ? Uri.parse('https://www.google.com/maps/search/?api=1&query=$lat,$lng')
          : Uri.parse('https://www.google.com/maps/search/?api=1&query=${Uri.encodeComponent(address)}');

      final ok = await launchUrl(googleMapsUrl, mode: LaunchMode.externalApplication);
      if (ok) return;
    } catch (e) {
      // fall through to snackbar
    }

    if (mounted) {
      ScaffoldMessenger.of(context).showSnackBar(
        SnackBar(
          content: Text(l10n.translate('couldNotOpenMapApplication')),
          backgroundColor: Colors.red,
        ),
      );
    }
  }

  Widget _buildContactTab() {
    final l10n = AppLocalizations.of(context)!;
    final formattedAddress = _getFormattedAddress();

    return SingleChildScrollView(
      padding: const EdgeInsets.all(16.0),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          // Address section first – clearly clickable, opens map
          if (formattedAddress != null && formattedAddress.isNotEmpty) ...[
            Text(
              l10n.address,
              style: const TextStyle(
                fontSize: 16,
                fontWeight: FontWeight.bold,
                color: Color(0xFF424242),
              ),
            ),
            const SizedBox(height: 8),
            Material(
              color: Colors.transparent,
              child: InkWell(
                onTap: () => _openMap(formattedAddress),
                borderRadius: BorderRadius.circular(12),
                child: Container(
                  width: double.infinity,
                  padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 14),
                  decoration: BoxDecoration(
                    color: const Color(0xFF17C3B2).withOpacity(0.08),
                    borderRadius: BorderRadius.circular(12),
                    border: Border.all(color: const Color(0xFF17C3B2).withOpacity(0.4)),
                  ),
                  child: Row(
                    crossAxisAlignment: CrossAxisAlignment.center,
                    children: [
                      const Icon(
                        Icons.map,
                        size: 24,
                        color: Color(0xFF17C3B2),
                      ),
                      const SizedBox(width: 14),
                      Expanded(
                        child: Column(
                          crossAxisAlignment: CrossAxisAlignment.start,
                          children: [
                            Text(
                              formattedAddress,
                              style: const TextStyle(
                                fontSize: 14,
                                fontWeight: FontWeight.w500,
                                color: Color(0xFF17C3B2),
                              ),
                              maxLines: 3,
                              overflow: TextOverflow.ellipsis,
                            ),
                            const SizedBox(height: 4),
                            Text(
                              l10n.translate('openInMaps'),
                              style: const TextStyle(
                                fontSize: 12,
                                color: Color(0xFF17C3B2),
                                decoration: TextDecoration.underline,
                              ),
                            ),
                          ],
                        ),
                      ),
                      const SizedBox(width: 8),
                      const Icon(
                        Icons.open_in_new,
                        size: 22,
                        color: Color(0xFF17C3B2),
                      ),
                    ],
                  ),
                ),
              ),
            ),
            const SizedBox(height: 20),
          ],
          if (_doctor!.clinic != null && _doctor!.clinic!.isNotEmpty) ...[
            Row(
              children: [
                const Icon(Icons.business, size: 20, color: Colors.grey),
                const SizedBox(width: 12),
                Expanded(
                  child: Text(
                    _doctor!.clinic!,
                    style: const TextStyle(fontSize: 14, fontWeight: FontWeight.w500),
                  ),
                ),
              ],
            ),
            const SizedBox(height: 16),
          ],
          if (_doctor!.phone != null && _doctor!.phone!.isNotEmpty) ...[
            InkWell(
              onTap: () async {
                final uri = Uri.parse('tel:${_doctor!.phone}');
                if (await canLaunchUrl(uri)) {
                  await launchUrl(uri);
                }
              },
              child: Row(
                children: [
                  const Icon(Icons.phone, size: 20, color: Colors.grey),
                  const SizedBox(width: 12),
                  Expanded(
                    child: Text(
                      _doctor!.phone!,
                      style: const TextStyle(fontSize: 14, color: Color(0xFF17C3B2)),
                    ),
                  ),
                ],
              ),
            ),
            const SizedBox(height: 16),
          ],
          if (_doctor!.email != null && _doctor!.email!.isNotEmpty) ...[
            InkWell(
              onTap: () async {
                final uri = Uri.parse('mailto:${_doctor!.email}');
                if (await canLaunchUrl(uri)) {
                  await launchUrl(uri);
                }
              },
              child: Row(
                children: [
                  const Icon(Icons.email, size: 20, color: Colors.grey),
                  const SizedBox(width: 12),
                  Expanded(
                    child: Text(
                      _doctor!.email!,
                      style: const TextStyle(fontSize: 14, color: Color(0xFF17C3B2)),
                    ),
                  ),
                ],
              ),
            ),
            const SizedBox(height: 16),
          ],
          if (_doctor!.telegram != null && _doctor!.telegram!.isNotEmpty) ...[
            InkWell(
              onTap: () async {
                String telegramUrl = _doctor!.telegram!;
                if (!telegramUrl.startsWith('http://') && !telegramUrl.startsWith('https://')) {
                  telegramUrl = telegramUrl.startsWith('@') 
                      ? 'https://t.me/${telegramUrl.substring(1)}'
                      : 'https://t.me/$telegramUrl';
                }
                final uri = Uri.parse(telegramUrl);
                if (await canLaunchUrl(uri)) {
                  await launchUrl(uri, mode: LaunchMode.externalApplication);
                }
              },
              child: Row(
                children: [
                  const Icon(Icons.send, size: 20, color: Colors.grey),
                  const SizedBox(width: 12),
                  Expanded(
                    child: Text(
                      _doctor!.telegram!,
                      style: const TextStyle(fontSize: 14, color: Color(0xFF17C3B2)),
                    ),
                  ),
                  const Icon(Icons.open_in_new, size: 16, color: Colors.grey),
                ],
              ),
            ),
            const SizedBox(height: 16),
          ],
          if (_doctor!.instagram != null && _doctor!.instagram!.isNotEmpty) ...[
            InkWell(
              onTap: () async {
                String instagramUrl = _doctor!.instagram!;
                if (!instagramUrl.startsWith('http://') && !instagramUrl.startsWith('https://')) {
                  instagramUrl = instagramUrl.startsWith('@')
                      ? 'https://instagram.com/${instagramUrl.substring(1)}'
                      : 'https://instagram.com/$instagramUrl';
                }
                final uri = Uri.parse(instagramUrl);
                if (await canLaunchUrl(uri)) {
                  await launchUrl(uri, mode: LaunchMode.externalApplication);
                }
              },
              child: Row(
                children: [
                  const Icon(Icons.camera_alt, size: 20, color: Colors.grey),
                  const SizedBox(width: 12),
                  Expanded(
                    child: Text(
                      _doctor!.instagram!,
                      style: const TextStyle(fontSize: 14, color: Color(0xFF17C3B2)),
                    ),
                  ),
                  const Icon(Icons.open_in_new, size: 16, color: Colors.grey),
                ],
              ),
            ),
          ],
          if ((_doctor!.clinic == null || _doctor!.clinic!.isEmpty) &&
              (_doctor!.address == null || _doctor!.address!.isEmpty) &&
              (_doctor!.phone == null || _doctor!.phone!.isEmpty) &&
              (_doctor!.email == null || _doctor!.email!.isEmpty) &&
              (_doctor!.telegram == null || _doctor!.telegram!.isEmpty) &&
              (_doctor!.instagram == null || _doctor!.instagram!.isEmpty)) ...[
            Center(
              child: Padding(
                padding: const EdgeInsets.all(32.0),
                child: Text(
                  AppLocalizations.of(context)!.translate('noContactInformationAvailable'),
                  style: const TextStyle(color: Colors.grey),
                ),
              ),
            ),
          ],
        ],
      ),
    );
  }

  Widget _buildReviewItem(review) {
    try {
      final date = DateTime.parse(review.createdAt);
      final dateFormat = DateFormat('MMM dd, yyyy');
      
      return Padding(
        padding: const EdgeInsets.only(bottom: 16),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Row(
              children: [
                CircleAvatar(
                  radius: 20,
                  backgroundImage: review.patientPhotoUrl != null
                      ? NetworkImage(normalizePhotoUrl(review.patientPhotoUrl!) ?? review.patientPhotoUrl!)
                      : null,
                  child: review.patientPhotoUrl == null
                      ? Text(
                          review.patientName.isNotEmpty
                              ? review.patientName[0].toUpperCase()
                              : 'A',
                        )
                      : null,
                ),
                const SizedBox(width: 12),
                Expanded(
                  child: Column(
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: [
                      Text(
                        review.patientName,
                        style: const TextStyle(
                          fontWeight: FontWeight.bold,
                          fontSize: 14,
                        ),
                      ),
                      const SizedBox(height: 4),
                      Row(
                        children: [
                          ...List.generate(5, (index) {
                            return Icon(
                              Icons.star,
                              color: index < review.rating
                                  ? Colors.amber
                                  : Colors.grey,
                              size: 14,
                            );
                          }),
                          const SizedBox(width: 8),
                          Text(
                            dateFormat.format(date),
                            style: const TextStyle(
                              fontSize: 12,
                              color: Colors.grey,
                            ),
                          ),
                        ],
                      ),
                    ],
                  ),
                ),
              ],
            ),
            if (review.comment != null && review.comment!.isNotEmpty) ...[
              const SizedBox(height: 8),
              Text(
                review.comment!,
                style: const TextStyle(fontSize: 14),
              ),
            ],
          ],
        ),
      );
    } catch (e) {
      return const SizedBox.shrink();
    }
  }
}
