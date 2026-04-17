import 'package:connectivity_plus/connectivity_plus.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

bool connectivityListOnline(List<ConnectivityResult> results) {
  return results.any((r) => r != ConnectivityResult.none);
}

/// Current connectivity; emits an initial value then stream updates.
final connectivityListProvider = StreamProvider<List<ConnectivityResult>>((
  ref,
) async* {
  yield await Connectivity().checkConnectivity();
  await for (final results in Connectivity().onConnectivityChanged) {
    yield results;
  }
});

/// Whether the device likely has a network route (not a guarantee of working API).
final isOnlineProvider = Provider<bool>((ref) {
  return ref
      .watch(connectivityListProvider)
      .maybeWhen(data: connectivityListOnline, orElse: () => true);
});
