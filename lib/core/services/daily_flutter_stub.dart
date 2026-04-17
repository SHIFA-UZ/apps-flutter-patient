// lib/core/services/daily_flutter_stub.dart
// Stub file for web platform - daily_flutter doesn't support web
import 'package:flutter/widgets.dart';

// These are just type stubs to prevent compilation errors on web
class CallClient {
  CallClient();
  Stream<dynamic> get events => const Stream.empty();
  Future<void> join({required String roomUrl, required String token}) async {}
  Future<void> leave() async {}
  Future<void> setLocalAudio(bool enabled) async {}
  Future<void> setLocalVideo(bool enabled) async {}
  Future<void> startScreenShare() async {}
  Future<void> stopScreenShare() async {}
  List<Participant> participants() => [];
}

class CallEvent {}
class CallStateUpdated extends CallEvent {
  final CallState state;
  CallStateUpdated(this.state);
}
class ParticipantJoined extends CallEvent {
  final Participant participant;
  ParticipantJoined(this.participant);
}
class ParticipantLeft extends CallEvent {
  final Participant participant;
  ParticipantLeft(this.participant);
}

class Participant {
  final String _id;
  final bool _isLocal;
  final dynamic _videoTrack;
  
  Participant({required String id, required bool isLocal, dynamic videoTrack})
      : _id = id,
        _isLocal = isLocal,
        _videoTrack = videoTrack;
  
  String get id => _id;
  bool get isLocal => _isLocal;
  dynamic get videoTrack => _videoTrack;
}

class CallState {
  static const left = CallState._('left');
  static const error = CallState._('error');
  final String value;
  const CallState._(this.value);
}

class VideoView extends StatelessWidget {
  final dynamic track;
  final bool mirror;
  const VideoView({Key? key, required this.track, this.mirror = false}) : super(key: key);
  @override
  Widget build(BuildContext context) => Container();
}
