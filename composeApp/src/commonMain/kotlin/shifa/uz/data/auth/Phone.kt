package shifa.uz.data.auth

/** `+<digits>`; a 9-digit local number gets Uzbekistan's `+998` (the Flutter app sent `+90…`, 05 Q8). */
fun normalizePhone(text: String): String {
    val digits = text.filter(Char::isDigit)
    return if (digits.length == 9) "+998$digits" else "+$digits"
}

/** 05 §3.3: the normalized phone, or null below 10 digits. */
fun effectivePhone(text: String): String? = normalizePhone(text).takeIf { it.length > 10 }

/** 05 §3.3: `+998` and 9 more digits. */
fun isUzbekPhone(phone: String) = phone.startsWith("+998") && phone.length == 13
