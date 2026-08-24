package ru.digitalhustle.certis.util.normalizer

import java.util.Locale

object EmailNormalizer {

    fun normalize(email: String): String = email.trim().lowercase(Locale.ROOT)
}
