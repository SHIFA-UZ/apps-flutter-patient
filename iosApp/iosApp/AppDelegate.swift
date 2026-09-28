import ComposeApp
import FirebaseCore
import FirebaseMessaging
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
        return true
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
        PushBridgeKt.onPushArrived()
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
