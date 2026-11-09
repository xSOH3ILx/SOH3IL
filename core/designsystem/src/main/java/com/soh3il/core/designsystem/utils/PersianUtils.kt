package com.soh3il.core.designsystem.utils

import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation

/**
 * Fast allocation-light converter from English digits (0-9) to Persian digits (۰-۹)
 */
fun String.toPersianDigits(): String {
    val chars = CharArray(this.length)
    for (i in indices) {
        val ch = this[i]
        chars[i] = if (ch in '0'..'9') {
            (ch.code - '0'.code + 0x06F0).toChar()
        } else {
            ch
        }
    }
    return String(chars)
}

fun Int.toPersianDigits(): String = this.toString().toPersianDigits()
fun Long.toPersianDigits(): String = this.toString().toPersianDigits()

/**
 * Fast, allocation-light utility that converts both Persian (\u06F0..\u06F9)
 * and Arabic-Indic (\u0660..\u0669) numerals to standard ASCII (0..9).
 */
fun String.toEnglishDigits(): String {
    val chars = CharArray(this.length)
    for (i in indices) {
        val ch = this[i]
        chars[i] = when (ch) {
            in '\u06F0'..'\u06F9' -> (ch.code - 0x06F0 + '0'.code).toChar()
            in '\u0660'..'\u0669' -> (ch.code - 0x0660 + '0'.code).toChar()
            else -> ch
        }
    }
    return String(chars)
}

/**
 * Alias for toEnglishDigits() to support backward compatibility.
 */
fun String.toStandardDigits(): String = this.toEnglishDigits()

/**
 * Parses String to Int supporting both Persian and Arabic numerals.
 */
fun String.toIntStandardOrNull(): Int? = this.toEnglishDigits().trim().toIntOrNull()

/**
 * Parses String to Long supporting both Persian and Arabic numerals.
 */
fun String.toLongStandardOrNull(): Long? = this.toEnglishDigits().trim().toLongOrNull()

/**
 * VisualTransformation preserving 1-to-1 OffsetMapping.Identity to render numerals
 * in Persian (۰-۹) on screen without breaking TextField cursor positioning or selection.
 */
class PersianDigitVisualTransformation : VisualTransformation {
    override fun filter(text: AnnotatedString): TransformedText {
        val converted = text.text.toPersianDigits()
        return TransformedText(
            text = AnnotatedString(converted),
            offsetMapping = OffsetMapping.Identity
        )
    }
}
