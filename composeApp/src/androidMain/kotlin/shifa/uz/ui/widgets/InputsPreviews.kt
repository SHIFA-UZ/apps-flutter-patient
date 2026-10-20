package shifa.uz.ui.widgets

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

@Preview
@Composable
internal fun ChipsPreview() =
    Gallery {
        ShifaChipType.entries.forEach { type ->
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ShifaChip("tag text", selected = false, onClick = {}, type = type)
                ShifaChip("tag text", selected = true, onClick = {}, type = type)
            }
        }
        val times = listOf("09:00", "09:30", "10:00", "10:30", "11:00", "11:30", "12:00", "12:30", "13:00", "13:30")
        ShifaChipGrid(times, Modifier.fillMaxWidth()) { ShifaChip(it, selected = it == "10:00", onClick = {}) }
        ShifaCarousel {
            listOf("All", "Dentist", "Cardiologist", "Therapist", "Neurologist").forEach {
                ShifaChip(it, selected = it == "All", onClick = {}, type = ShifaChipType.Slot)
            }
        }
    }

@Preview
@Composable
internal fun SearchFieldPreview() =
    Gallery {
        val wide = Modifier.fillMaxWidth()
        ShifaSearchField("", {}, "Search", wide)
        ShifaSearchField("Input text", {}, "Search", wide)
        ShifaSearchField("", {}, "Search", wide, enabled = false)
        ShifaSearchField("Input text", {}, "Search", wide, isError = true)
    }

@Preview
@Composable
internal fun DropdownPreview() =
    Gallery {
        val options = listOf("Option 1", "Option 2", "Option 3", "Option 4")
        val wide = Modifier.fillMaxWidth()
        ShifaDropdown("label", options, null, {}, expanded = false, onExpandedChange = {}, modifier = wide)
        ShifaDropdown("label", options, null, {}, expanded = true, onExpandedChange = {}, modifier = wide)
        ShifaDropdown("label", options, "Option 2", {}, expanded = true, onExpandedChange = {}, modifier = wide)
        ShifaDropdown("label", options, "Option 2", {}, expanded = false, onExpandedChange = {}, modifier = wide)
        ShifaDropdown("label", options, "Option 1", {}, false, {}, wide, isError = true)
    }

@Preview
@Composable
internal fun SwitchPreview() =
    Gallery {
        ShifaSwitch(checked = false, onCheckedChange = {})
        ShifaSwitch(checked = true, onCheckedChange = {})
    }
