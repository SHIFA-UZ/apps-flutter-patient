package shifa.uz.data.auth

import com.google.common.truth.Truth.assertThat
import com.russhwolf.settings.MapSettings
import org.junit.Test

class LoginAttemptsTest {
    private var now = 1_000_000L
    private val settings = MapSettings()
    private val classToTest = LoginAttempts(settings) { now }

    private fun fail(times: Int) = repeat(times) { classToTest.recordFailure() }

    @Test
    fun `starts with every attempt and no lock`() {
        assertThat(classToTest.remainingAttempts()).isEqualTo(5)
        assertThat(classToTest.lockedMinutes()).isEqualTo(0)
    }

    @Test
    fun `each failure uses an attempt and the fifth locks for five minutes`() {
        fail(4)
        assertThat(classToTest.remainingAttempts()).isEqualTo(1)
        assertThat(classToTest.lockedMinutes()).isEqualTo(0)

        fail(1)
        assertThat(classToTest.remainingAttempts()).isEqualTo(0)
        assertThat(classToTest.lockedMinutes()).isEqualTo(5)
    }

    @Test
    fun `minutes round up and a failure while locked doesn't extend the lock`() {
        fail(5)
        now += 60_000
        fail(1)
        assertThat(classToTest.lockedMinutes()).isEqualTo(4)

        now += 3 * 60_000 + 1_000 // 59 s left
        assertThat(classToTest.lockedMinutes()).isEqualTo(1)
    }

    @Test
    fun `an expired lock clears both counters`() {
        fail(5)
        now += 5 * 60_000

        assertThat(classToTest.lockedMinutes()).isEqualTo(0)
        assertThat(classToTest.remainingAttempts()).isEqualTo(5)
        assertThat(settings.keys).isEmpty()
    }

    @Test
    fun `clear resets the attempts`() {
        fail(3)
        classToTest.clear()

        assertThat(classToTest.remainingAttempts()).isEqualTo(5)
    }
}
