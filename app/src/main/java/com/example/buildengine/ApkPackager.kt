package com.example.buildengine

import java.io.FilterOutputStream
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStream
import java.nio.charset.StandardCharsets
import java.util.zip.CRC32
import java.util.zip.Deflater
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

/**
 * Packages compiled Android artifacts into an unsigned, 4-byte zipaligned APK.
 * Guarantees resources.arsc and uncompressed PNG resources start at 4-byte aligned
 * offsets as required by Android 11+ (targetSdk 30+).
 */
object ApkPackager {

    private class CountingOutputStream(out: OutputStream) : FilterOutputStream(out) {
        var bytesWritten: Long = 0L
            private set

        override fun write(b: Int) {
            out.write(b)
            bytesWritten++
        }

        override fun write(b: ByteArray, off: Int, len: Int) {
            out.write(b, off, len)
            bytesWritten += len
        }
    }

    data class ApkEntry(
        val path: String,
        val data: ByteArray,
        val storedAligned: Boolean,
    )

    fun packageUnsignedApk(entries: List<ApkEntry>, outputApk: File) {
        outputApk.parentFile?.mkdirs()
        CountingOutputStream(FileOutputStream(outputApk)).use { counter ->
            ZipOutputStream(counter).use { zos ->
                zos.setLevel(Deflater.DEFAULT_COMPRESSION)
                for (item in entries) {
                    if (item.storedAligned) {
                        zos.flush()
                        val nameBytesLen = item.path.toByteArray(StandardCharsets.UTF_8).size
                        val headerBase = counter.bytesWritten + 30L + nameBytesLen
                        val pad = ((4L - (headerBase % 4L)) % 4L).toInt()
                        val crc = CRC32().apply { update(item.data) }.value
                        val entry = ZipEntry(item.path).apply {
                            method = ZipEntry.STORED
                            size = item.data.size.toLong()
                            compressedSize = item.data.size.toLong()
                            this.crc = crc
                            if (pad > 0) {
                                // Valid zero-padding in extra field for 4-byte alignment
                                extra = ByteArray(pad)
                            }
                        }
                        zos.putNextEntry(entry)
                        zos.write(item.data)
                        zos.closeEntry()
                        zos.flush()
                    } else {
                        val entry = ZipEntry(item.path).apply {
                            method = ZipEntry.DEFLATED
                        }
                        zos.putNextEntry(entry)
                        zos.write(item.data)
                        zos.closeEntry()
                        zos.flush()
                    }
                }
            }
        }
    }
}
