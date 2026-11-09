package com.soh3il.core.designsystem.utils

/**
 * Converts English digits (0-9) to Persian digits (۰-۹)
 */
fun String.toPersianDigits(): String {
    return this.map { ch ->
        when (ch) {
            '0' -> '۰'
            '1' -> '۱'
            '2' -> '۲'
            '3' -> '۳'
            '4' -> '۴'
            '5' -> '۵'
            '6' -> '۶'
            '7' -> '۷'
            '8' -> '۸'
            '9' -> '۹'
            else -> ch
        }
    }.joinToString("")
}

fun Int.toPersianDigits(): String = this.toString().toPersianDigits()
fun Long.toPersianDigits(): String = this.toString().toPersianDigits()

/**
 * Converts Persian and Arabic-Indic digits to standard English digits (0-9)
 */
fun String.toStandardDigits(): String {
    return this.map { ch ->
        when (ch) {
            '۰', '٠' -> '0'
            '۱', '١' -> '1'
            '۲', '٢' -> '2'
            '۳', '٣' -> '3'
            '۴', '٤' -> '4'
            '۵', '٥' -> '5'
            '۶', '٦' -> '6'
            '۷', '٧' -> '7'
            '۸', '٨' -> '8'
            '۹', '٩' -> '9'
            else -> ch
        }
    }.joinToString("")
}

/**
 * Parses String to Int supporting both Persian and Arabic numerals
 */
fun String.toIntStandardOrNull(): Int? = this.toStandardDigits().trim().toIntOrNull()

/**
 * Parses String to Long supporting both Persian and Arabic numerals
 */
fun String.toLongStandardOrNull(): Long? = this.toStandardDigits().trim().toLongOrNull()
