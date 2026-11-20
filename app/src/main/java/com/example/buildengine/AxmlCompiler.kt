package com.example.buildengine

import java.io.ByteArrayOutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * Pure-Kotlin Android Binary XML (AXML) compiler producing spec-compliant
 * RES_XML_TYPE (0x0003) binary AndroidManifest.xml files on-device.
 */
object AxmlCompiler {

    private const val RES_STRING_POOL_TYPE: Short = 0x0001
    private const val RES_XML_TYPE: Short = 0x0003
    private const val RES_XML_START_NAMESPACE_TYPE: Short = 0x0100
    private const val RES_XML_END_NAMESPACE_TYPE: Short = 0x0101
    private const val RES_XML_START_ELEMENT_TYPE: Short = 0x0102
    private const val RES_XML_END_ELEMENT_TYPE: Short = 0x0103
    private const val RES_XML_RESOURCE_MAP_TYPE: Short = 0x0180

    private const val TYPE_REFERENCE: Byte = 0x01
    private const val TYPE_STRING: Byte = 0x03
    private const val TYPE_INT_DEC: Byte = 0x10
    private const val TYPE_INT_HEX: Byte = 0x11
    private const val TYPE_INT_BOOLEAN: Byte = 0x12

    private const val ANDROID_NS_URI = "http://schemas.android.com/apk/res/android"
    private const val ANDROID_NS_PREFIX = "android"

    // Official android.R.attr.* resource IDs verified from Android SDK 36
    private val ATTR_RESOURCE_IDS = mapOf(
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

    sealed class AttrValue {
        data class Str(val value: String) : AttrValue()
        data class Ref(val resId: Int) : AttrValue()
        data class IntDec(val value: Int) : AttrValue()
        data class IntHex(val value: Int) : AttrValue()
        data class Bool(val value: Boolean) : AttrValue()
    }

    data class XmlAttr(
        val namespace: String?,
        val name: String,
        val value: AttrValue,
    )

    data class XmlNode(
        val tag: String,
        val attrs: List<XmlAttr> = emptyList(),
        val children: List<XmlNode> = emptyList(),
    )

    fun buildManifestNode(spec: BuildSpec): XmlNode {
        val permissions = buildList {
            add("android.permission.INTERNET")
            add("android.permission.ACCESS_NETWORK_STATE")
            if (spec.cameraPermission) add("android.permission.CAMERA")
            if (spec.microphonePermission || spec.webRtcEnabled) {
                add("android.permission.RECORD_AUDIO")
                add("android.permission.MODIFY_AUDIO_SETTINGS")
            }
            if (spec.geolocationEnabled) {
                add("android.permission.ACCESS_FINE_LOCATION")
                add("android.permission.ACCESS_COARSE_LOCATION")
            }
            if (spec.downloadManagerEnabled) {
                add("android.permission.POST_NOTIFICATIONS")
            }
        }.distinct().map { perm ->
            XmlNode(
                tag = "uses-permission",
                attrs = listOf(XmlAttr(ANDROID_NS_URI, "name", AttrValue.Str(perm)))
            )
        }

        val intentFilters = mutableListOf(
            XmlNode(
                tag = "intent-filter",
                children = listOf(
                    XmlNode("action", listOf(XmlAttr(ANDROID_NS_URI, "name", AttrValue.Str("android.intent.action.MAIN")))),
                    XmlNode("category", listOf(XmlAttr(ANDROID_NS_URI, "name", AttrValue.Str("android.intent.category.LAUNCHER")))),
                )
            )
        )

        if (spec.enableDeepLinks && spec.deepLinkScheme.isNotBlank()) {
            intentFilters.add(
                XmlNode(
                    tag = "intent-filter",
                    children = listOf(
                        XmlNode("action", listOf(XmlAttr(ANDROID_NS_URI, "name", AttrValue.Str("android.intent.action.VIEW")))),
                        XmlNode("category", listOf(XmlAttr(ANDROID_NS_URI, "name", AttrValue.Str("android.intent.category.DEFAULT")))),
                        XmlNode("category", listOf(XmlAttr(ANDROID_NS_URI, "name", AttrValue.Str("android.intent.category.BROWSABLE")))),
                        XmlNode("data", listOf(XmlAttr(ANDROID_NS_URI, "scheme", AttrValue.Str(spec.deepLinkScheme.trim())))),
                    )
                )
            )
        }

        if (spec.enableAppLinks && spec.appLinkHost.isNotBlank()) {
            intentFilters.add(
                XmlNode(
                    tag = "intent-filter",
                    attrs = listOf(XmlAttr(ANDROID_NS_URI, "autoVerify", AttrValue.Bool(true))),
                    children = listOf(
                        XmlNode("action", listOf(XmlAttr(ANDROID_NS_URI, "name", AttrValue.Str("android.intent.action.VIEW")))),
                        XmlNode("category", listOf(XmlAttr(ANDROID_NS_URI, "name", AttrValue.Str("android.intent.category.DEFAULT")))),
                        XmlNode("category", listOf(XmlAttr(ANDROID_NS_URI, "name", AttrValue.Str("android.intent.category.BROWSABLE")))),
                        XmlNode(
                            "data",
                            listOf(
                                XmlAttr(ANDROID_NS_URI, "scheme", AttrValue.Str("https")),
                                XmlAttr(ANDROID_NS_URI, "host", AttrValue.Str(spec.appLinkHost.trim())),
                            )
                        ),
                    )
                )
            )
        }

        val screenOrientationInt = when (spec.orientationMode) {
            com.example.domain.model.OrientationMode.PORTRAIT -> 1
            com.example.domain.model.OrientationMode.LANDSCAPE -> 0
            com.example.domain.model.OrientationMode.AUTO -> -1
        }

        val themeResId = if (spec.themePreset == com.example.domain.model.ThemePreset.LIGHT) {
            0x0103012C // Theme_DeviceDefault_Light_NoActionBar
        } else {
            0x01030129 // Theme_DeviceDefault_NoActionBar
        }

        val activityNode = XmlNode(
            tag = "activity",
            attrs = listOf(
                XmlAttr(ANDROID_NS_URI, "name", AttrValue.Str("${spec.packageName}.MainActivity")),
                XmlAttr(ANDROID_NS_URI, "exported", AttrValue.Bool(true)),
                XmlAttr(ANDROID_NS_URI, "label", AttrValue.Ref(ArscCompiler.RES_ID_APP_NAME)),
                XmlAttr(ANDROID_NS_URI, "theme", AttrValue.Ref(themeResId)),
                XmlAttr(ANDROID_NS_URI, "launchMode", AttrValue.IntDec(1)), // singleTop
                XmlAttr(ANDROID_NS_URI, "screenOrientation", AttrValue.IntDec(screenOrientationInt)),
                XmlAttr(ANDROID_NS_URI, "configChanges", AttrValue.IntHex(0x000004A0)), // orientation|screenSize|keyboardHidden
                XmlAttr(ANDROID_NS_URI, "windowSoftInputMode", AttrValue.IntHex(0x10)), // adjustResize
                XmlAttr(ANDROID_NS_URI, "hardwareAccelerated", AttrValue.Bool(true)),
            ),
            children = intentFilters
        )

        val applicationNode = XmlNode(
            tag = "application",
            attrs = listOf(
                XmlAttr(ANDROID_NS_URI, "allowBackup", AttrValue.Bool(true)),
                XmlAttr(ANDROID_NS_URI, "icon", AttrValue.Ref(ArscCompiler.RES_ID_IC_LAUNCHER)),
                XmlAttr(ANDROID_NS_URI, "roundIcon", AttrValue.Ref(ArscCompiler.RES_ID_IC_LAUNCHER_ROUND)),
                XmlAttr(ANDROID_NS_URI, "label", AttrValue.Ref(ArscCompiler.RES_ID_APP_NAME)),
                XmlAttr(ANDROID_NS_URI, "supportsRtl", AttrValue.Bool(spec.supportRtl)),
                XmlAttr(ANDROID_NS_URI, "theme", AttrValue.Ref(themeResId)),
                XmlAttr(ANDROID_NS_URI, "usesCleartextTraffic", AttrValue.Bool(!spec.httpsOnly)),
                XmlAttr(ANDROID_NS_URI, "hardwareAccelerated", AttrValue.Bool(true)),
            ),
            children = listOf(activityNode)
        )

        val usesSdkNode = XmlNode(
            tag = "uses-sdk",
            attrs = listOf(
                XmlAttr(ANDROID_NS_URI, "minSdkVersion", AttrValue.IntDec(spec.minSdk)),
                XmlAttr(ANDROID_NS_URI, "targetSdkVersion", AttrValue.IntDec(spec.targetSdk)),
            )
        )

        return XmlNode(
            tag = "manifest",
            attrs = listOf(
                XmlAttr(ANDROID_NS_URI, "versionCode", AttrValue.IntDec(spec.versionCode)),
                XmlAttr(ANDROID_NS_URI, "versionName", AttrValue.Str(spec.versionName)),
                XmlAttr(null, "package", AttrValue.Str(spec.packageName)),
            ),
            children = permissions + listOf(usesSdkNode, applicationNode)
        )
    }

    /**
     * Android resolves manifest attributes (icon, label, theme...) by walking them in ascending
     * resource-id order, exactly as aapt2 writes them. Attributes written in any other order are
     * silently skipped by the framework, which leaves the app without an icon or label.
     * Attributes without a resource id (e.g. `package`) go first, like aapt2 does.
     */
    private fun sortAttributesByResourceId(node: XmlNode): XmlNode {
        fun idOf(a: XmlAttr): Int =
            if (a.namespace == ANDROID_NS_URI) ATTR_RESOURCE_IDS[a.name] ?: 0 else 0
        return node.copy(
            attrs = node.attrs.sortedBy(::idOf),
            children = node.children.map(::sortAttributesByResourceId),
        )
    }

    fun compile(rawRoot: XmlNode): ByteArray {
        val root = sortAttributesByResourceId(rawRoot)
        val stringList = mutableListOf<String>()
        val stringMap = LinkedHashMap<String, Int>()
        val resourceIds = mutableListOf<Int>()

        // 1. Reserve indices 0..N-1 for android:* attributes that map to resource IDs
        val usedAndroidAttrs = linkedSetOf<String>()
        fun collectAndroidAttrs(node: XmlNode) {
            node.attrs.forEach { attr ->
                if (attr.namespace == ANDROID_NS_URI && ATTR_RESOURCE_IDS.containsKey(attr.name)) {
                    usedAndroidAttrs.add(attr.name)
                }
            }
            node.children.forEach(::collectAndroidAttrs)
        }
        collectAndroidAttrs(root)

        fun intern(s: String): Int = stringMap.getOrPut(s) {
            val idx = stringList.size
            stringList.add(s)
            idx
        }

        usedAndroidAttrs.forEach { attrName ->
            intern(attrName)
            resourceIds.add(ATTR_RESOURCE_IDS.getValue(attrName))
        }

        // 2. Intern namespace strings and remaining XML strings
        val nsPrefixIdx = intern(ANDROID_NS_PREFIX)
        val nsUriIdx = intern(ANDROID_NS_URI)

        fun internNodeStrings(node: XmlNode) {
            intern(node.tag)
            node.attrs.forEach { attr ->
                attr.namespace?.let(::intern)
                intern(attr.name)
                if (attr.value is AttrValue.Str) {
                    intern(attr.value.value)
                }
            }
            node.children.forEach(::internNodeStrings)
        }
        internNodeStrings(root)

        // 3. Build StringPool chunk & ResourceMap chunk
        val stringPoolBytes = encodeStringPoolUtf16(stringList)
        val resMapBytes = encodeResourceMap(resourceIds)

        // 4. Build XML event stream
        val eventsOut = ByteArrayOutputStream()
        var line = 1

        fun writeStartNs() {
            val buf = ByteBuffer.allocate(24).order(ByteOrder.LITTLE_ENDIAN)
            buf.putShort(RES_XML_START_NAMESPACE_TYPE)
            buf.putShort(16)
            buf.putInt(24)
            buf.putInt(line++)
            buf.putInt(-1)
            buf.putInt(nsPrefixIdx)
            buf.putInt(nsUriIdx)
            eventsOut.write(buf.array())
        }

        fun writeEndNs() {
            val buf = ByteBuffer.allocate(24).order(ByteOrder.LITTLE_ENDIAN)
            buf.putShort(RES_XML_END_NAMESPACE_TYPE)
            buf.putShort(16)
            buf.putInt(24)
            buf.putInt(line++)
            buf.putInt(-1)
            buf.putInt(nsPrefixIdx)
            buf.putInt(nsUriIdx)
            eventsOut.write(buf.array())
        }

        fun emitNode(node: XmlNode) {
            val attrCount = node.attrs.size
            val chunkSize = 36 + attrCount * 20
            val buf = ByteBuffer.allocate(chunkSize).order(ByteOrder.LITTLE_ENDIAN)
            buf.putShort(RES_XML_START_ELEMENT_TYPE)
            buf.putShort(16)
            buf.putInt(chunkSize)
            buf.putInt(line++)
            buf.putInt(-1) // comment
            buf.putInt(-1) // ns
            buf.putInt(stringMap.getValue(node.tag))
            buf.putShort(0x0014) // attributeStart
            buf.putShort(0x0014) // attributeSize
            buf.putShort(attrCount.toShort())
            buf.putShort(0) // idIndex
            buf.putShort(0) // classIndex
            buf.putShort(0) // styleIndex

            for (attr in node.attrs) {
                val attrNsIdx = attr.namespace?.let { stringMap.getValue(it) } ?: -1
                val attrNameIdx = stringMap.getValue(attr.name)
                buf.putInt(attrNsIdx)
                buf.putInt(attrNameIdx)
                when (val v = attr.value) {
                    is AttrValue.Str -> {
                        val strIdx = stringMap.getValue(v.value)
                        buf.putInt(strIdx) // rawValue
                        buf.putShort(8) // Res_value size
                        buf.put(0) // res0
                        buf.put(TYPE_STRING)
                        buf.putInt(strIdx)
                    }
                    is AttrValue.Ref -> {
                        buf.putInt(-1)
                        buf.putShort(8)
                        buf.put(0)
                        buf.put(TYPE_REFERENCE)
                        buf.putInt(v.resId)
                    }
                    is AttrValue.IntDec -> {
                        buf.putInt(-1)
                        buf.putShort(8)
                        buf.put(0)
                        buf.put(TYPE_INT_DEC)
                        buf.putInt(v.value)
                    }
                    is AttrValue.IntHex -> {
                        buf.putInt(-1)
                        buf.putShort(8)
                        buf.put(0)
                        buf.put(TYPE_INT_HEX)
                        buf.putInt(v.value)
                    }
                    is AttrValue.Bool -> {
                        buf.putInt(-1)
                        buf.putShort(8)
                        buf.put(0)
                        buf.put(TYPE_INT_BOOLEAN)
                        buf.putInt(if (v.value) -1 else 0)
                    }
                }
            }
            eventsOut.write(buf.array())

            for (child in node.children) {
                emitNode(child)
            }

            val endBuf = ByteBuffer.allocate(24).order(ByteOrder.LITTLE_ENDIAN)
            endBuf.putShort(RES_XML_END_ELEMENT_TYPE)
            endBuf.putShort(16)
            endBuf.putInt(24)
            endBuf.putInt(line++)
            endBuf.putInt(-1)
            endBuf.putInt(-1)
            endBuf.putInt(stringMap.getValue(node.tag))
            eventsOut.write(endBuf.array())
        }

        writeStartNs()
        emitNode(root)
        writeEndNs()

        val eventsBytes = eventsOut.toByteArray()
        val totalSize = 8 + stringPoolBytes.size + resMapBytes.size + eventsBytes.size
        val result = ByteBuffer.allocate(totalSize).order(ByteOrder.LITTLE_ENDIAN)
        result.putShort(RES_XML_TYPE)
        result.putShort(8)
        result.putInt(totalSize)
        result.put(stringPoolBytes)
        result.put(resMapBytes)
        result.put(eventsBytes)
        return result.array()
    }

    private fun encodeResourceMap(ids: List<Int>): ByteArray {
        val totalSize = 8 + ids.size * 4
        val buf = ByteBuffer.allocate(totalSize).order(ByteOrder.LITTLE_ENDIAN)
        buf.putShort(RES_XML_RESOURCE_MAP_TYPE)
        buf.putShort(8)
        buf.putInt(totalSize)
        ids.forEach { buf.putInt(it) }
        return buf.array()
    }

    internal fun encodeStringPoolUtf16(strings: List<String>): ByteArray {
        val offsets = IntArray(strings.size)
        val stringsBuf = ByteArrayOutputStream()

        for (i in strings.indices) {
            offsets[i] = stringsBuf.size()
            val s = strings[i]
            val len = s.length
            if (len < 0x8000) {
                stringsBuf.write(len and 0xFF)
                stringsBuf.write((len shr 8) and 0xFF)
            } else {
                val high = 0x8000 or (len shr 16)
                val low = len and 0xFFFF
                stringsBuf.write(high and 0xFF)
                stringsBuf.write((high shr 8) and 0xFF)
                stringsBuf.write(low and 0xFF)
                stringsBuf.write((low shr 8) and 0xFF)
            }
            for (ch in s) {
                val code = ch.code
                stringsBuf.write(code and 0xFF)
                stringsBuf.write((code shr 8) and 0xFF)
            }
            // UTF-16 null terminator
            stringsBuf.write(0)
            stringsBuf.write(0)
        }

        // Pad string data to 4-byte boundary
        while (stringsBuf.size() % 4 != 0) {
            stringsBuf.write(0)
        }

        val headerSize = 28
        val offsetsSize = strings.size * 4
        val stringsStart = headerSize + offsetsSize
        val chunkSize = stringsStart + stringsBuf.size()

        val buf = ByteBuffer.allocate(chunkSize).order(ByteOrder.LITTLE_ENDIAN)
        buf.putShort(RES_STRING_POOL_TYPE)
        buf.putShort(headerSize.toShort())
        buf.putInt(chunkSize)
        buf.putInt(strings.size) // stringCount
        buf.putInt(0) // styleCount
        buf.putInt(0) // flags: UTF-16
        buf.putInt(stringsStart)
        buf.putInt(0) // stylesStart
        offsets.forEach { buf.putInt(it) }
        buf.put(stringsBuf.toByteArray())
        return buf.array()
    }
}
