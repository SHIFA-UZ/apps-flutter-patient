package shifa.uz.ui.documents

import com.github.takahirom.roborazzi.captureRoboImage
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [33], qualifiers = "w393dp-h852dp-xhdpi")
class DocumentsScreenshotTest {
    @Test
    fun list() {
        captureRoboImage("screenshots/Documents_list.png") { DocumentsListPreview() }
    }

    @Test
    fun filtered() {
        captureRoboImage("screenshots/Documents_filtered.png") { DocumentsFilteredPreview() }
    }

    @Test
    fun empty() {
        captureRoboImage("screenshots/Documents_empty.png") { DocumentsEmptyPreview() }
    }

    @Test
    fun error() {
        captureRoboImage("screenshots/Documents_error.png") { DocumentsErrorPreview() }
    }

    @Test
    fun upload() {
        captureRoboImage("screenshots/Documents_upload.png") { DocumentUploadPreview() }
    }

    @Test
    fun tray() {
        captureRoboImage("screenshots/Documents_tray.png") { DocumentUploadTrayPreview() }
    }

    @Test
    fun `viewer loading`() {
        captureRoboImage("screenshots/DocumentViewer_loading.png") { DocumentViewerLoadingPreview() }
    }

    @Test
    fun `viewer error`() {
        captureRoboImage("screenshots/DocumentViewer_error.png") { DocumentViewerErrorPreview() }
    }

    @Test
    fun `viewer unsupported`() {
        captureRoboImage("screenshots/DocumentViewer_unsupported.png") { DocumentViewerUnsupportedPreview() }
    }
}
