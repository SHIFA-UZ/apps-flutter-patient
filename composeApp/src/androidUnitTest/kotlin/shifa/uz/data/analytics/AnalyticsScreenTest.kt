package shifa.uz.data.analytics

import com.google.common.truth.Truth.assertThat
import org.junit.Test
import shifa.uz.data.model.Appointment
import shifa.uz.ui.bookings.BookingsList
import shifa.uz.ui.forgotpassword.ForgotPasswordStep
import shifa.uz.ui.navigation.ShifaTab
import shifa.uz.ui.navigation.analyticsScreen

class AnalyticsScreenTest {
    @Test
    fun `screen ids are unique snake_case`() {
        val ids = AnalyticsScreen.entries.map { it.id }
        assertThat(ids).containsNoDuplicates()
        ids.forEach { assertThat(it).matches("[a-z]+(_[a-z]+)*") }
    }

    @Test
    fun `enum values become snake_case`() {
        assertThat(ForgotPasswordStep.NewPassword.analyticsName()).isEqualTo("new_password")
        assertThat(BookingsList.MyDoctors.analyticsName()).isEqualTo("my_doctors")
        assertThat(BookingsList.Past.analyticsName()).isEqualTo("past")
    }

    @Test
    fun `every tab has a screen`() {
        assertThat(ShifaTab.entries.map { it.analyticsScreen }).containsNoDuplicates()
    }

    @Test
    fun `the payment badge follows the details card`() {
        assertThat(Appointment(paymentStatus = "PAID").paymentBadge()).isEqualTo("paid")
        assertThat(Appointment(paymentBadge = "PAY_IN_CLINIC").paymentBadge()).isEqualTo("pay_in_clinic")
        assertThat(
            Appointment(status = "PENDING_PAYMENT", paymentStatus = "PENDING").paymentBadge(),
        ).isEqualTo("pending")
        assertThat(Appointment(paymentStatus = "PAID", refundRequired = true).paymentBadge()).isEqualTo("refund")
        assertThat(Appointment().paymentBadge()).isEqualTo("none")
    }

    @Test
    fun `the appointment state is cancelled, upcoming or past`() {
        assertThat(appointmentState(Appointment(status = "CANCELLED"), isUpcoming = false)).isEqualTo("cancelled")
        assertThat(appointmentState(Appointment(status = "EXPIRED"), isUpcoming = false)).isEqualTo("cancelled")
        assertThat(appointmentState(Appointment(status = "CONFIRMED"), isUpcoming = true)).isEqualTo("upcoming")
        assertThat(appointmentState(Appointment(status = "COMPLETED"), isUpcoming = false)).isEqualTo("past")
    }

    @Test
    fun `file types are coarse`() {
        assertThat(fileType("application/pdf")).isEqualTo("pdf")
        assertThat(fileType("PDF")).isEqualTo("pdf")
        assertThat(fileType("image/jpeg")).isEqualTo("image")
        assertThat(fileType("png")).isEqualTo("image")
        assertThat(fileType("docx")).isEqualTo("other")
        assertThat(fileType(null)).isEqualTo("other")
    }
}
