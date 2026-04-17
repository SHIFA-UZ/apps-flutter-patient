package com.shifa.service

import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseToken
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.util.Optional

/**
 * Verifies Firebase ID tokens (e.g. from phone OTP sign-in).
 * Requires GOOGLE_APPLICATION_CREDENTIALS or Firebase project configuration.
 */
@Service
class FirebaseAuthService {
    private val log = LoggerFactory.getLogger(FirebaseAuthService::class.java)
    private var firebaseAuth: FirebaseAuth? = null
        get() {
            if (field == null) {
                try {
                    FirebaseApp.initializeApp()
                    field = FirebaseAuth.getInstance()
                    log.info("Firebase Auth initialized for ID token verification")
                } catch (e: Exception) {
                    log.warn("Firebase not configured: {}", e.message)
                }
            }
            return field
        }

    /**
     * Verify ID token and return decoded token, or empty if invalid/not configured.
     */
    fun verifyIdToken(idToken: String): Optional<FirebaseToken> {
        val auth = firebaseAuth ?: return Optional.empty()
        return try {
            Optional.of(auth.verifyIdToken(idToken))
        } catch (e: Exception) {
            log.warn("Firebase ID token verification failed: {}", e.message)
            Optional.empty()
        }
    }

    /**
     * Get phone number for the Firebase UID (from Firebase Auth user record).
     * Returns null if user not found or phone not set.
     */
    fun getPhoneNumberByUid(uid: String): String? {
        val auth = firebaseAuth ?: return null
        return try {
            val userRecord = auth.getUser(uid)
            userRecord.phoneNumber
        } catch (e: Exception) {
            log.warn("Could not get Firebase user {}: {}", uid, e.message)
            null
        }
    }

    fun isConfigured(): Boolean = firebaseAuth != null
}
