package com.example.buildengine

import java.io.ByteArrayOutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * Pure-Kotlin Android Binary Resource Table (resources.arsc) compiler.
 * Encodes RES_TABLE_TYPE (0x0002) with mipmap/ic_launcher, mipmap/ic_launcher_round,
 * string/app_name, color/primary_color, and color/background_color.
 */
object ArscCompiler {

    private const val RES_TABLE_TYPE: Short = 0x0002
    private const val RES_TABLE_PACKAGE_TYPE: Short = 0x0200
    private const val RES_TABLE_TYPE_TYPE: Short = 0x0201
    private const val RES_TABLE_TYPE_SPEC_TYPE: Short = 0x0202

    private const val TYPE_STRING: Byte = 0x03
    private const val TYPE_INT_COLOR_ARGB8: Byte = 0x1C

    const val RES_ID_IC_LAUNCHER = 0x7F010000
    const val RES_ID_IC_LAUNCHER_ROUND = 0x7F010001
    const val RES_ID_APP_NAME = 0x7F020000
    const val RES_ID_PRIMARY_COLOR = 0x7F030000
    const val RES_ID_BG_COLOR = 0x7F030001

    const val ICON_APK_PATH = "res/mipmap-xxhdpi-v4/ic_launcher.png"
    const val ICON_ROUND_APK_PATH = "res/mipmap-xxhdpi-v4/ic_launcher_round.png"

    fun compile(
        packageName: String,
        appName: String,
        primaryColorArgb: Int,
        backgroundColorArgb: Int,
    ): ByteArray {
        val globalStrings = listOf(
            ICON_APK_PATH,       // 0
            ICON_ROUND_APK_PATH, // 1
            appName,             // 2
        )
        val globalPool = AxmlCompiler.encodeStringPoolUtf16(globalStrings)

        val typeStrings = listOf("mipmap", "string", "color")
        val typePool = AxmlCompiler.encodeStringPoolUtf16(typeStrings)

        val keyStrings = listOf(
            "ic_launcher",       // 0
            "ic_launcher_round", // 1
            "app_name",          // 2
            "primary_color",     // 3
            "background_color",  // 4
        )
        val keyPool = AxmlCompiler.encodeStringPoolUtf16(keyStrings)

        // Type 1: mipmap (2 entries)
        val mipmapSpec = encodeTypeSpec(typeId = 1, entryCount = 2)
        val mipmapType = encodeTypeEntries(
            typeId = 1,
            entries = listOf(
                EntrySpec(keyIdx = 0, valueType = TYPE_STRING, valueData = 0),
                EntrySpec(keyIdx = 1, valueType = TYPE_STRING, valueData = 1),
            )
        )

        // Type 2: string (1 entry)
        val stringSpec = encodeTypeSpec(typeId = 2, entryCount = 1)
        val stringType = encodeTypeEntries(
            typeId = 2,
            entries = listOf(
                EntrySpec(keyIdx = 2, valueType = TYPE_STRING, valueData = 2),
            )
        )

        // Type 3: color (2 entries)
        val colorSpec = encodeTypeSpec(typeId = 3, entryCount = 2)
        val colorType = encodeTypeEntries(
            typeId = 3,
            entries = listOf(
                EntrySpec(keyIdx = 3, valueType = TYPE_INT_COLOR_ARGB8, valueData = primaryColorArgb),
                EntrySpec(keyIdx = 4, valueType = TYPE_INT_COLOR_ARGB8, valueData = backgroundColorArgb),
            )
        )

        val pkgBody = ByteArrayOutputStream().apply {
            write(typePool)
            write(keyPool)
            write(mipmapSpec)
            write(mipmapType)
            write(stringSpec)
            write(stringType)
            write(colorSpec)
            write(colorType)
        }.toByteArray()

        val pkgHeaderSize = 288
        val pkgChunkSize = pkgHeaderSize + pkgBody.size
        val pkgBuf = ByteBuffer.allocate(pkgChunkSize).order(ByteOrder.LITTLE_ENDIAN)
        pkgBuf.putShort(RES_TABLE_PACKAGE_TYPE)
        pkgBuf.putShort(pkgHeaderSize.toShort())
        pkgBuf.putInt(pkgChunkSize)
        pkgBuf.putInt(0x7F) // packageId

        // 128 UTF-16 chars = 256 bytes for package name
        val pkgChars = CharArray(128)
        val copyLen = minOf(packageName.length, 127)
        for (i in 0 until copyLen) {
            pkgChars[i] = packageName[i]
        }
        for (ch in pkgChars) {
            pkgBuf.putShort(ch.code.toShort())
        }

        pkgBuf.putInt(pkgHeaderSize) // typeStrings offset
        pkgBuf.putInt(typeStrings.size) // lastPublicType
        pkgBuf.putInt(pkgHeaderSize + typePool.size) // keyStrings offset
        pkgBuf.putInt(keyStrings.size) // lastPublicKey
        pkgBuf.putInt(0) // typeIdOffset
        pkgBuf.put(pkgBody)

        val pkgBytes = pkgBuf.array()
        val tableHeaderSize = 12
        val totalSize = tableHeaderSize + globalPool.size + pkgBytes.size
        val tableBuf = ByteBuffer.allocate(totalSize).order(ByteOrder.LITTLE_ENDIAN)
        tableBuf.putShort(RES_TABLE_TYPE)
        tableBuf.putShort(tableHeaderSize.toShort())
        tableBuf.putInt(totalSize)
        tableBuf.putInt(1) // packageCount
        tableBuf.put(globalPool)
        tableBuf.put(pkgBytes)
        return tableBuf.array()
    }

    private data class EntrySpec(
        val keyIdx: Int,
        val valueType: Byte,
        val valueData: Int,
    )

    private fun encodeTypeSpec(typeId: Int, entryCount: Int): ByteArray {
        val headerSize = 16
        val chunkSize = headerSize + entryCount * 4
        val buf = ByteBuffer.allocate(chunkSize).order(ByteOrder.LITTLE_ENDIAN)
        buf.putShort(RES_TABLE_TYPE_SPEC_TYPE)
        buf.putShort(headerSize.toShort())
        buf.putInt(chunkSize)
        buf.put(typeId.toByte())
        buf.put(0) // res0
        buf.putShort(0) // res1
        buf.putInt(entryCount)
        repeat(entryCount) {
            buf.putInt(0) // config flags
        }
        return buf.array()
    }

    private fun encodeTypeEntries(typeId: Int, entries: List<EntrySpec>): ByteArray {
        // ResTable_type header (84 bytes includes 64-byte ResTable_config for API 36 compatibility)
        val configSize = 64
        val headerSize = 20 + configSize // 84 bytes
        val entryOffsetsSize = entries.size * 4
        val entrySize = 16 // 8-byte ResTable_entry + 8-byte Res_value
        val entriesStart = headerSize + entryOffsetsSize
        val totalSize = entriesStart + entries.size * entrySize

        val buf = ByteBuffer.allocate(totalSize).order(ByteOrder.LITTLE_ENDIAN)
        buf.putShort(RES_TABLE_TYPE_TYPE)
        buf.putShort(headerSize.toShort())
        buf.putInt(totalSize)
        buf.put(typeId.toByte())
        buf.put(0) // flags
        buf.putShort(0) // reserved
        buf.putInt(entries.size)
        buf.putInt(entriesStart)

        // ResTable_config (64 bytes, first 4 bytes = size)
        buf.putInt(configSize)
        repeat(configSize - 4) { buf.put(0) }

        // Entry offset array
        for (i in entries.indices) {
            buf.putInt(i * entrySize)
        }

        // Entries
        for (e in entries) {
            buf.putShort(8) // ResTable_entry size
            buf.putShort(0) // flags
            buf.putInt(e.keyIdx)
            buf.putShort(8) // Res_value size
            buf.put(0) // res0
            buf.put(e.valueType)
            buf.putInt(e.valueData)
        }
        return buf.array()
    }
}
