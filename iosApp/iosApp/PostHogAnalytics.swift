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
            properties: properties,
            userProperties: userProperties.isEmpty ? nil : userProperties,
            userPropertiesSetOnce: userPropertiesSetOnce.isEmpty ? nil : userPropertiesSetOnce
        )
    }

    func screen(name: String, properties: [String: Any]) {
        PostHogSDK.shared.screen(name, properties: properties)
    }

    func identify(distinctId: String, userProperties: [String: Any], userPropertiesSetOnce: [String: Any]) {
        PostHogSDK.shared.identify(
            distinctId,
            userProperties: userProperties,
            userPropertiesSetOnce: userPropertiesSetOnce.isEmpty ? nil : userPropertiesSetOnce
        )
    }

    func register(key: String, value: Any) {
        PostHogSDK.shared.register([key: value])
    }

    func reset() {
        PostHogSDK.shared.reset()
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
