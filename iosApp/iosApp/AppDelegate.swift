import ComposeApp
import FirebaseCore
import FirebaseMessaging
import PostHog
import UIKit
import UserNotifications

/// 13 §3.1-3.3 (NOTIF-03): Firebase, the FCM token and push taps, handed to the shared code through PushBridge.
/// The permission is asked for after login, from PushSetup.
class AppDelegate: NSObject, UIApplicationDelegate, UNUserNotificationCenterDelegate, MessagingDelegate {
    func application(
        _ application: UIApplication,
        didFinishLaunchingWithOptions launchOptions: [UIApplication.LaunchOptionsKey: Any]? = nil
    ) -> Bool {
        FirebaseApp.configure()
        Messaging.messaging().delegate = self
        // Set before launch ends, so a tap that cold-starts the app still arrives.
        UNUserNotificationCenter.current().delegate = self
        application.registerForRemoteNotifications()
        // 06 §3.6: the shared call screens make their Daily calls through this.
        VideoCalls.shared.factory = { DailyVideoCall() }
        setUpAnalytics()
        return true
    }

    /// docs/posthog/setup.md §5. Release builds only (README D4: debug builds never track), and no key
    /// (Secrets.xcconfig missing) means analytics stays off.
    private func setUpAnalytics() {
        #if DEBUG
        return
        #else
        guard let key = Bundle.main.object(forInfoDictionaryKey: "PostHogApiKey") as? String, !key.isEmpty else { return }
        let config = PostHogConfig(projectToken: key, host: "https://eu.i.posthog.com")
        config.captureApplicationLifecycleEvents = true
        config.captureScreenViews = false // UIKit only sees the one Compose controller (README D6)
        config.captureElementInteractions = false
        // Our own push events (events.md §11); the SDK's would carry the push payload.
        config.capturePushNotificationSubscriptions = false
        config.capturePushNotificationOpened = false
        config.sessionReplay = false // README D5
        config.surveys = false // README D12
        config.personProfiles = .identifiedOnly
        config.errorTrackingConfig.autoCapture = true // README D11
        let off = AnalyticsBridgeKt.analyticsStartsOptedOut() // privacy.md §4, README D8
        config.preloadFeatureFlags = !off // README D10; not while off, or setup would still ask PostHog for flags
        config.optOut = off
        PostHogSDK.shared.setup(config)
        AnalyticsBridgeKt.registerAnalytics(sink: PostHogAnalytics())
        #endif
    }

    func application(_ application: UIApplication, didRegisterForRemoteNotificationsWithDeviceToken deviceToken: Data) {
        Messaging.messaging().apnsToken = deviceToken
    }

    func messaging(_ messaging: Messaging, didReceiveRegistrationToken fcmToken: String?) {
        if let fcmToken { PushBridgeKt.onPushToken(token: fcmToken) }
    }

    // Foreground: the system banner, with the backend's text.
    func userNotificationCenter(
        _ center: UNUserNotificationCenter,
        willPresent notification: UNNotification,
        withCompletionHandler completionHandler: @escaping (UNNotificationPresentationOptions) -> Void
    ) {
        // Signed out: a push for the account that left is not shown (as on Android).
        guard PushBridgeKt.isSignedIn() else { return completionHandler([]) }
        PushBridgeKt.onPushArrived(type: notification.request.content.userInfo["type"] as? String)
        completionHandler([.banner, .list, .sound])
    }

    func userNotificationCenter(
        _ center: UNUserNotificationCenter,
        didReceive response: UNNotificationResponse,
        withCompletionHandler completionHandler: @escaping () -> Void
    ) {
        PushBridgeKt.onPushOpened(userInfo: response.notification.request.content.userInfo)
        completionHandler()
    }
}
