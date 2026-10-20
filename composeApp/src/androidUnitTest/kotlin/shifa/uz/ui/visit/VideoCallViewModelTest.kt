package shifa.uz.ui.visit

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.wheneverBlocking
import shifa.composeapp.generated.resources.Res
import shifa.composeapp.generated.resources.errorVideoCallMediaPermissionRequired
import shifa.composeapp.generated.resources.errorVideoCallPaymentRequired
import shifa.composeapp.generated.resources.failedToStartVideoCall
import shifa.uz.data.model.ApiException
import shifa.uz.data.model.Appointment
import shifa.uz.data.model.VideoToken
import shifa.uz.data.video.VideoRepository
import shifa.uz.utils.VideoCall
import shifa.uz.utils.VideoCallState

@OptIn(ExperimentalCoroutinesApi::class)
class VideoCallViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    private val videos = mock<VideoRepository>()
    private val call = FakeCall()

    private val classToTest by lazy {
        VideoCallViewModel(
            Appointment("7", startAt = "2026-10-19T09:00:00Z"),
            call,
            videos,
        )
    }

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `the preview starts once both permissions are granted`() =
        runTest {
            classToTest.onPermissions(false)
            assertThat(classToTest.state.value.error).isEqualTo(Res.string.errorVideoCallMediaPermissionRequired)
            assertThat(call.previewing).isFalse()

            classToTest.onPermissions(true)
            assertThat(call.previewing).isTrue()
        }

    @Test
    fun `join takes V1's room and token to Daily, and V1's refusal is shown`() =
        runTest {
            wheneverBlocking { videos.token("7") } doReturn
                Result.failure(ApiException.Forbidden("Payment is required before joining this video consultation."))
            classToTest.join()
            advanceUntilIdle()
            assertThat(classToTest.state.value.error).isEqualTo(Res.string.errorVideoCallPaymentRequired)
            assertThat(call.joined).isNull()

            wheneverBlocking { videos.token("7") } doReturn Result.success(VideoToken("t", "https://shifa.daily.co/r"))
            classToTest.join()
            assertThat(classToTest.state.value.isJoining).isTrue()
            advanceUntilIdle()
            assertThat(call.joined).isEqualTo("https://shifa.daily.co/r" to "t")
        }

    @Test
    fun `a call that fails says so, and one the doctor ends closes the page`() =
        runTest {
            classToTest.closed.test {
                call.state.value = VideoCallState(VideoCallState.Phase.Preview, failed = true)
                advanceUntilIdle()
                assertThat(classToTest.state.value.error).isEqualTo(Res.string.failedToStartVideoCall)

                call.state.value = VideoCallState(VideoCallState.Phase.Left)
                advanceUntilIdle()
                awaitItem()
            }
        }

    @Test
    fun `leaving frees the call once and closes the page`() =
        runTest {
            classToTest.closed.test {
                classToTest.leave()
                classToTest.leave()
                awaitItem()
            }
            assertThat(call.releases).isEqualTo(1)
        }

    private class FakeCall : VideoCall {
        override val state = MutableStateFlow(VideoCallState())
        var previewing = false
        var joined: Pair<String, String>? = null
        var releases = 0

        override fun startPreview() {
            previewing = true
        }

        override fun join(
            roomUrl: String,
            token: String,
        ) {
            joined = roomUrl to token
        }

        override fun setCamera(on: Boolean) = Unit

        override fun setMic(on: Boolean) = Unit

        override fun flipCamera() = Unit

        override fun release() {
            releases++
        }
    }
}
