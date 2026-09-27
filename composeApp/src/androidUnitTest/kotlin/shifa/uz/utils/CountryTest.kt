package shifa.uz.utils

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class CountryTest {
    @Test
    fun `every country, Uzbekistan first, with English API names`() {
        val countries = countries()

        assertThat(countries.size).isGreaterThan(240)
        assertThat(countries.first().apiName).isEqualTo("Uzbekistan")
        assertThat(countries.map { it.code }).containsNoDuplicates()
        val byCode = countries.associateBy { it.code }
        assertThat(byCode.getValue("AE").apiName).isEqualTo("United Arab Emirates")
        assertThat(byCode.getValue("DE").apiName).isEqualTo("Germany")
        assertThat(byCode.getValue("TR").apiName).isEqualTo("Turkey")
    }
}
