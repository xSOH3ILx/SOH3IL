package com.soh3il.core.jalali

import java.io.InputStream

/** Counts source bytes before a decoder can materialize an oversized line. */
internal class IcsBoundedInputStream(private val source: InputStream, private val limit: Long) : InputStream() {
    private var consumed = 0L

    override fun read(): Int {
        val value = source.read()
        if (value >= 0) checkSize(1)
        return value
    }

    override fun read(buffer: ByteArray, offset: Int, length: Int): Int {
        if (length == 0) return 0
        val allowed = minOf(length.toLong(), limit - consumed + 1).toInt()
        val read = source.read(buffer, offset, allowed)
        if (read > 0) checkSize(read)
        return read
    }

    private fun checkSize(read: Int) {
        consumed += read
        require(consumed <= limit) { "حجم فایل تقویم فراتر از سقف مجاز (۱۰ مگابایت) است" }
    }
}
