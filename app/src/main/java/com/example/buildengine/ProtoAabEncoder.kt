package com.example.buildengine

import java.io.ByteArrayOutputStream
import java.nio.charset.StandardCharsets

/**
 * Pure-Kotlin Protocol Buffers wire encoder for Android App Bundle (.aab) artifacts:
 * - BundleConfig.pb
 * - base/manifest/AndroidManifest.xml (aapt.pb.XmlNode)
 * - base/resources.pb (aapt.pb.ResourceTable)
 */
object ProtoAabEncoder {

    private class ProtoBuf {
        private val out = ByteArrayOutputStream()

        fun writeVarint(value: Long) {
            var v = value
            while (true) {
                if ((v and 0x7FL.inv()) == 0L) {
                    out.write(v.toInt())
                    return
                }
                out.write(((v.toInt() and 0x7F) or 0x80))
                v = v ushr 7
            }
        }

        fun writeTag(fieldNumber: Int, wireType: Int) {
            writeVarint(((fieldNumber shl 3) or wireType).toLong())
        }

        fun writeInt32(fieldNumber: Int, value: Int) {
            writeTag(fieldNumber, 0)
            writeVarint(value.toLong())
        }

        fun writeUInt32(fieldNumber: Int, value: Int) {
            writeTag(fieldNumber, 0)
            writeVarint(value.toLong() and 0xFFFFFFFFL)
        }

        fun writeBool(fieldNumber: Int, value: Boolean) {
            writeTag(fieldNumber, 0)
            writeVarint(if (value) 1L else 0L)
        }

        fun writeString(fieldNumber: Int, value: String) {
            val bytes = value.toByteArray(StandardCharsets.UTF_8)
            writeBytes(fieldNumber, bytes)
        }

        fun writeBytes(fieldNumber: Int, bytes: ByteArray) {
            writeTag(fieldNumber, 2)
            writeVarint(bytes.size.toLong())
            out.write(bytes)
        }

        fun writeMessage(fieldNumber: Int, build: ProtoBuf.() -> Unit) {
            val child = ProtoBuf().apply(build).toByteArray()
            writeBytes(fieldNumber, child)
        }

        fun toByteArray(): ByteArray = out.toByteArray()
    }

    fun encodeBundleConfig(bundletoolVersion: String = "1.17.1"): ByteArray =
        ProtoBuf().apply {
            // BundleConfig.bundletool (field 1)
            writeMessage(1) {
                writeString(2, bundletoolVersion)
            }
            // BundleConfig.optimizations (field 2) -> splits_config
            writeMessage(2) {
                writeMessage(1) {
                    // split_dimension: ABI(1), SCREEN_DENSITY(2), LANGUAGE(3)
                    writeMessage(1) { writeInt32(1, 1); writeBool(2, false) }
                    writeMessage(1) { writeInt32(1, 2); writeBool(2, false) }
                    writeMessage(1) { writeInt32(1, 3); writeBool(2, false) }
                }
                // uncompress_native_libraries
                writeMessage(2) {
                    writeBool(1, true)
                }
            }
            // BundleConfig.type = REGULAR (0)
            writeInt32(4, 0)
        }.toByteArray()

    fun encodeProtoXmlNode(node: AxmlCompiler.XmlNode): ByteArray {
        val androidNs = "http://schemas.android.com/apk/res/android"
        val attrIds = mapOf(
            "theme" to 0x01010000,
            "label" to 0x01010001,
            "icon" to 0x01010002,
            "name" to 0x01010003,
            "exported" to 0x01010010,
            "launchMode" to 0x0101001d,
            "screenOrientation" to 0x0101001e,
            "configChanges" to 0x0101001f,
            "scheme" to 0x01010027,
            "host" to 0x01010028,
            "pathPrefix" to 0x0101002b,
            "minSdkVersion" to 0x0101020c,
            "versionCode" to 0x0101021b,
            "versionName" to 0x0101021c,
            "windowSoftInputMode" to 0x0101022b,
            "targetSdkVersion" to 0x01010270,
            "allowBackup" to 0x01010280,
            "hardwareAccelerated" to 0x010102d3,
            "supportsRtl" to 0x010103af,
            "usesCleartextTraffic" to 0x010104ec,
            "autoVerify" to 0x010104ee,
            "roundIcon" to 0x0101052c,
        )

        fun writeXmlNode(buf: ProtoBuf, xml: AxmlCompiler.XmlNode, isRoot: Boolean) {
            // XmlNode.element (field 1)
            buf.writeMessage(1) {
                if (isRoot) {
                    writeMessage(1) {
                        writeString(1, "android")
                        writeString(2, androidNs)
                    }
                }
                writeString(3, xml.tag)
                xml.attrs.forEach { attr ->
                    writeMessage(4) {
                        if (attr.namespace != null) writeString(1, attr.namespace)
                        writeString(2, attr.name)
                        val resId = attrIds[attr.name] ?: 0
                        if (resId != 0) writeUInt32(4, resId)
                        when (val v = attr.value) {
                            is AxmlCompiler.AttrValue.Str -> {
                                writeString(3, v.value)
                                writeMessage(5) {
                                    writeMessage(2) { writeString(1, v.value) }
                                }
                            }
                            is AxmlCompiler.AttrValue.Ref -> {
                                writeMessage(5) {
                                    writeMessage(1) { writeUInt32(2, v.resId) }
                                }
                            }
                            is AxmlCompiler.AttrValue.IntDec -> {
                                writeString(3, v.value.toString())
                                writeMessage(5) {
                                    writeMessage(7) { writeInt32(1, v.value) }
                                }
                            }
                            is AxmlCompiler.AttrValue.IntHex -> {
                                writeString(3, "0x${v.value.toString(16)}")
                                writeMessage(5) {
                                    writeMessage(7) { writeInt32(2, v.value) }
                                }
                            }
                            is AxmlCompiler.AttrValue.Bool -> {
                                writeString(3, v.value.toString())
                                writeMessage(5) {
                                    writeMessage(7) { writeBool(3, v.value) }
                                }
                            }
                        }
                    }
                }
                xml.children.forEach { child ->
                    writeMessage(5) {
                        writeXmlNode(this, child, isRoot = false)
                    }
                }
            }
        }

        return ProtoBuf().apply {
            writeXmlNode(this, node, isRoot = true)
        }.toByteArray()
    }

    fun encodeProtoResourceTable(
        packageName: String,
        appName: String,
        primaryColorArgb: Int,
        backgroundColorArgb: Int,
    ): ByteArray = ProtoBuf().apply {
        // ResourceTable.package (field 2)
        writeMessage(2) {
            writeMessage(1) { writeUInt32(1, 0x7F) } // package_id
            writeString(2, packageName)

            // type 1: mipmap
            writeMessage(3) {
                writeMessage(1) { writeUInt32(1, 1) }
                writeString(2, "mipmap")
                writeProtoFileEntry(0, "ic_launcher", ArscCompiler.ICON_APK_PATH)
                writeProtoFileEntry(1, "ic_launcher_round", ArscCompiler.ICON_ROUND_APK_PATH)
            }
            // type 2: string
            writeMessage(3) {
                writeMessage(1) { writeUInt32(1, 2) }
                writeString(2, "string")
                writeMessage(3) {
                    writeMessage(1) { writeUInt32(1, 0) }
                    writeString(2, "app_name")
                    writeMessage(5) {
                        writeMessage(2) {
                            writeMessage(2) { writeMessage(2) { writeString(1, appName) } }
                        }
                    }
                }
            }
            // type 3: color
            writeMessage(3) {
                writeMessage(1) { writeUInt32(1, 3) }
                writeString(2, "color")
                writeProtoColorEntry(0, "primary_color", primaryColorArgb)
                writeProtoColorEntry(1, "background_color", backgroundColorArgb)
            }
        }
    }.toByteArray()

    private fun ProtoBuf.writeProtoFileEntry(id: Int, name: String, path: String) {
        writeMessage(3) {
            writeMessage(1) { writeUInt32(1, id) }
            writeString(2, name)
            writeMessage(5) {
                writeMessage(2) {
                    writeMessage(2) {
                        writeMessage(4) {
                            writeString(1, path)
                            writeInt32(2, 1) // PNG
                        }
                    }
                }
            }
        }
    }

    private fun ProtoBuf.writeProtoColorEntry(id: Int, name: String, argb: Int) {
        writeMessage(3) {
            writeMessage(1) { writeUInt32(1, id) }
            writeString(2, name)
            writeMessage(5) {
                writeMessage(2) {
                    writeMessage(2) {
                        writeMessage(7) { writeUInt32(4, argb) }
                    }
                }
            }
        }
    }
}
