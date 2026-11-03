import ComposeApp
import Foundation
import PostHog

/// PostHog's iOS SDK behind the shared `AnalyticsSink` (docs/posthog/setup.md §5). Registered in AppDelegate.
final class PostHogAnalytics: NSObject, AnalyticsSink {
    func capture(
        event: String,
        properties: [String: Any],
        userProperties: [String: Any],
        userPropertiesSetOnce: [String: Any]
    ) {
        PostHogSDK.shared.capture(
            event,
            properties: properties.json,
            userProperties: userProperties.isEmpty ? nil : userProperties.json,
            userPropertiesSetOnce: userPropertiesSetOnce.isEmpty ? nil : userPropertiesSetOnce.json
        )
    }

    func screen(name: String, properties: [String: Any]) {
        PostHogSDK.shared.screen(name, properties: properties.json)
    }

    func identify(distinctId: String, userProperties: [String: Any], userPropertiesSetOnce: [String: Any]) {
        PostHogSDK.shared.identify(
            distinctId,
            userProperties: userProperties.json,
            userPropertiesSetOnce: userPropertiesSetOnce.isEmpty ? nil : userPropertiesSetOnce.json
        )
    }

    func register(key: String, value: Any) {
        PostHogSDK.shared.register([key: value].json)
    }

    func reset() {
        PostHogSDK.shared.reset()
    }

    func distinctId() -> String {
        PostHogSDK.shared.getDistinctId()
    }

    func optOut(out: Bool) {
        if out { PostHogSDK.shared.optOut() } else { PostHogSDK.shared.optIn() }
    }

    func isFeatureEnabled(key: String) -> Bool {
        PostHogSDK.shared.isFeatureEnabled(key)
    }

    func featureFlagPayloadJson(key: String) -> String? {
        guard let payload = PostHogSDK.shared.getFeatureFlagResult(key)?.payload else { return nil }
        guard let data = try? JSONSerialization.data(withJSONObject: payload, options: [.fragmentsAllowed]) else {
            return nil
        }
        return String(data: data, encoding: .utf8)
    }

    func reloadFeatureFlags() {
        PostHogSDK.shared.reloadFeatureFlags()
    }
}

private extension Dictionary where Key == String, Value == Any {
    /// A Kotlin `Boolean` arrives as `KotlinBoolean`, which the SDK's JSONSerialization writes as `1` / `0` (Android
    /// sends `true` / `false`); a Swift `Bool` bridges to the CFBoolean it writes as `true` / `false`. Kotlin can't
    /// fix this: it turns even `kCFBooleanTrue` back into a `KotlinBoolean`. Values are flat (events.md).
    var json: [String: Any] { mapValues { ($0 as? KotlinBoolean)?.boolValue ?? $0 } }
}
