import 'package:flutter_riverpod/flutter_riverpod.dart';

/// Tracks the main bottom-nav shell tab (0=home, 1=bookings, 2=documents, 3=doctors).
final shellTabIndexProvider = StateProvider<int>((ref) => 0);
