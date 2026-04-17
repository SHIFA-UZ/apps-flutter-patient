package com.shifa.service

import com.google.firebase.FirebaseApp
import com.google.firebase.messaging.FirebaseMessaging
import com.google.firebase.messaging.Message
import com.shifa.domain.Notification
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

/**
 * Sends FCM push notifications to patient app tokens.
 * Requires Firebase Admin SDK to be configured (same as FirebaseAuthService).
 */
@Service
class FcmService {

    private val log = LoggerFactory.getLogger(FcmService::class.java)

    private var firebaseMessaging: FirebaseMessaging? = null
        get() {
            if (field == null) {
                try {
                    try {
                        FirebaseApp.getInstance()
                    } catch (_: Exception) {
                        FirebaseApp.initializeApp()
                    }
                    field = FirebaseMessaging.getInstance()
                } catch (e: Exception) {
                    log.debug("Firebase Messaging not available: {}", e.message)
                }
            }
            return field
        }

    /**
     * Send push notification to the given FCM token.
     * Data map values must be strings (FCM requirement). Safe to call if Firebase is not configured.
     */
    fun sendToToken(
        token: String,
        title: String,
        body: String,
        data: Map<String, String>
    ): Boolean {
        if (token.isBlank()) return false
        val messaging = firebaseMessaging ?: return false
        return try {
            val message = Message.builder()
                .setToken(token)
                .setNotification(
                    com.google.firebase.messaging.Notification.builder()
                        .setTitle(title)
                        .setBody(body)
                        .build()
                )
                .putAllData(data)
                .build()
            messaging.send(message)
            log.info("FCM sent to patient (title={})", title)
            true
        } catch (e: Exception) {
            log.warn("FCM send failed: {}", e.message)
            false
        }
    }

    /**
     * Build data map for patient app from a saved notification (matches NotificationModel.fromFcmData).
     */
    fun sendPatientNotification(
        fcmToken: String?,
        notification: Notification,
        extraData: Map<String, String> = emptyMap()
    ): Boolean {
        if (fcmToken.isNullOrBlank()) {
            log.info("FCM skip: no token for patient (notification id={})", notification.id)
            return false
        }
        val data = mutableMapOf<String, String>(
            "id" to notification.id.toString(),
            "title" to notification.title,
            "message" to notification.message,
            "type" to notification.type.name,
            "createdAt" to notification.createdAt.toString(),
        )
        notification.appointmentId?.let { data["appointmentId"] = it.toString() }
        notification.documentAccessRequestId?.let { data["documentAccessRequestId"] = it.toString() }
        notification.taskId?.let { data["taskId"] = it.toString() }
        data.putAll(extraData)
        return sendToToken(
            token = fcmToken,
            title = notification.title,
            body = notification.message,
            data = data
        )
    }
}
