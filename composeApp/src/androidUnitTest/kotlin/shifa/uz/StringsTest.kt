package shifa.uz

import com.google.common.truth.Truth.assertThat
import com.google.common.truth.Truth.assertWithMessage
import org.junit.Test
import java.io.File

// Unit tests run from the module dir. de may fall back to English (strings/README.md), the others may not.
class StringsTest {
    private fun strings(dir: String): Map<String, String> =
        Regex("""<string name="(\w+)">(.*)</string>""")
            .findAll(File("src/commonMain/composeResources/$dir/strings.xml").readText())
            .associate { it.groupValues[1] to it.groupValues[2] }

    private val en = strings("values")

    @Test
    fun `uz and ru have every key and only known keys`() {
        listOf("values-uz", "values-ru").forEach {
            assertWithMessage(it).that(strings(it).keys).containsExactlyElementsIn(en.keys)
        }
    }

    @Test
    fun `de has only known keys`() {
        assertThat(en.keys).containsAtLeastElementsIn(strings("values-de").keys)
    }

    @Test
    fun `every locale uses the same placeholders as en`() {
        val placeholders = Regex("""%\d+\$[ds]""")
        listOf("values-uz", "values-ru", "values-de").forEach { dir ->
            strings(dir).forEach { (key, value) ->
                assertWithMessage("$dir/$key")
                    .that(placeholders.findAll(value).map { it.value }.toSet())
                    .isEqualTo(placeholders.findAll(en.getValue(key)).map { it.value }.toSet())
            }
        }
    }
}
