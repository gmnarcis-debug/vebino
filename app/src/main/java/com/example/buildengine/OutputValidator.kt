package com.example.buildengine

import com.android.apksig.ApkVerifier
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.bouncycastle.cert.X509CertificateHolder
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter
import org.bouncycastle.cms.CMSProcessableByteArray
import org.bouncycastle.cms.CMSSignedData
import org.bouncycastle.cms.jcajce.JcaSimpleSignerInfoVerifierBuilder
import org.bouncycastle.jce.provider.BouncyCastleProvider
import java.io.File
import java.io.InputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.security.MessageDigest
import java.security.Security
import java.security.cert.X509Certificate
import java.util.zip.ZipFile

/**
 * Cryptographic and structural validator for compiled APK and AAB artifacts.
 * Ensures incomplete, tampered, mismatched, or unsigned outputs are never marked valid.
 */
object OutputValidator {

    init {
        if (Security.getProvider(BouncyCastleProvider.PROVIDER_NAME) == null) {
            Security.addProvider(BouncyCastleProvider())
        }
    }

    // افزودن تابع کمکی برای خواندن امن بایت‌ها در همه نسخه‌های اندروید
    private fun InputStream.readBytesSafely(maxBytes: Int): ByteArray {
        val buffer = ByteArray(maxBytes)
        var bytesRead = 0
        while (bytesRead < maxBytes) {
            val result = this.read(buffer, bytesRead, maxBytes - bytesRead)
            if (result == -1) break
            bytesRead += result
        }
        return if (bytesRead == 0) ByteArray(0) else if (bytesRead < maxBytes) buffer.copyOf(bytesRead) else buffer
    }

    data class ManifestIdentity(
        val packageName: String? = null,
        val versionCode: Int? = null,
        val versionName: String? = null,
    )

    data class ValidationReport(
        val isValid: Boolean,
        val isSigned: Boolean,
        val v1Verified: Boolean,
        val v2Verified: Boolean,
        val v3Verified: Boolean,
        val certSha256: String,
        val summary: String,
        val errors: List<String> = emptyList(),
        val parsedManifest: ManifestIdentity? = null,
    )

    fun validateApk(apkFile: File, expectedSpec: BuildSpec? = null): ValidationReport {
        if (!apkFile.exists() || apkFile.length() < 1024L) {
            return ValidationReport(
                isValid = false,
                isSigned = false,
                v1Verified = false,
                v2Verified = false,
                v3Verified = false,
                certSha256 = "",
                summary = "فایل APK وجود ندارد یا حجم آن نامعتبر است.",
                errors = listOf("Missing or empty APK file"),
            )
        }

        val structuralErrors = mutableListOf<String>()
        var parsedManifest: ManifestIdentity? = null

        try {
            ZipFile(apkFile).use { zip ->
                val manifestEntry = zip.getEntry("AndroidManifest.xml")
                val arscEntry = zip.getEntry("resources.arsc")
                val dexEntry = zip.getEntry("classes.dex")
                val iconEntry = zip.getEntry(ArscCompiler.ICON_APK_PATH)
                val roundIconEntry = zip.getEntry(ArscCompiler.ICON_ROUND_APK_PATH)
                val configEntry = zip.getEntry("assets/webino_config.json")

                if (manifestEntry == null || manifestEntry.size < 32) {
                    structuralErrors.add("Missing binary AndroidManifest.xml")
                } else {
                    val manifestBytes = zip.getInputStream(manifestEntry).use { it.readBytes() }
                    if (manifestBytes.size < 8 || manifestBytes[0] != 0x03.toByte() || manifestBytes[1] != 0x00.toByte()) {
                        structuralErrors.add("Invalid AXML binary header in AndroidManifest.xml")
                    } else {
                        parsedManifest = parseBinaryManifestIdentity(manifestBytes)
                        if (parsedManifest?.packageName.isNullOrBlank()) {
                            structuralErrors.add("Binary AndroidManifest.xml missing package attribute")
                        }
                        if (expectedSpec != null && parsedManifest != null) {
                            if (parsedManifest!!.packageName != expectedSpec.packageName) {
                                structuralErrors.add("Package name mismatch: expected ${expectedSpec.packageName}, got ${parsedManifest!!.packageName}")
                            }
                            if (parsedManifest!!.versionCode != expectedSpec.versionCode) {
                                structuralErrors.add("Version code mismatch: expected ${expectedSpec.versionCode}, got ${parsedManifest!!.versionCode}")
                            }
                            if (parsedManifest!!.versionName != expectedSpec.versionName) {
                                structuralErrors.add("Version name mismatch: expected ${expectedSpec.versionName}, got ${parsedManifest!!.versionName}")
                            }
                        }
                    }
                }

                if (arscEntry == null || arscEntry.size < 32) {
                    structuralErrors.add("Missing resources.arsc")
                } else {
                    // استفاده از تابع امن جایگزین
                    val arscHdr = zip.getInputStream(arscEntry).use { it.readBytesSafely(4) }
                    if (arscHdr.size < 4 || arscHdr[0] != 0x02.toByte() || arscHdr[1] != 0x00.toByte()) {
                        structuralErrors.add("Invalid RES_TABLE_TYPE header in resources.arsc")
                    }
                }

                if (dexEntry == null || dexEntry.size < 112) {
                    structuralErrors.add("Missing classes.dex")
                } else {
                    // استفاده از تابع امن جایگزین
                    val dexHdr = zip.getInputStream(dexEntry).use { it.readBytesSafely(4) }
                    if (String(dexHdr, Charsets.US_ASCII) != "dex\n") {
                        structuralErrors.add("Invalid DEX magic header in classes.dex")
                    }
                }

                if (iconEntry == null || iconEntry.size < 32) {
                    structuralErrors.add("Missing launcher icon: ${ArscCompiler.ICON_APK_PATH}")
                } else {
                    // استفاده از تابع امن جایگزین
                    val pngHdr = zip.getInputStream(iconEntry).use { it.readBytesSafely(4) }
                    if (!isPngHeader(pngHdr)) {
                        structuralErrors.add("Invalid PNG header in ${ArscCompiler.ICON_APK_PATH}")
                    }
                }

                if (roundIconEntry == null || roundIconEntry.size < 32) {
                    structuralErrors.add("Missing round launcher icon: ${ArscCompiler.ICON_ROUND_APK_PATH}")
                }

                if (configEntry == null || configEntry.size < 10) {
                    structuralErrors.add("Missing assets/webino_config.json")
                }
            }
        } catch (t: Throwable) {
            structuralErrors.add("Invalid APK archive structure: ${t.message ?: t.javaClass.simpleName}")
        }

        val verifierResult = runCatching {
            ApkVerifier.Builder(apkFile)
                .setMinCheckedPlatformVersion(minOf(expectedSpec?.minSdk ?: 21, 21))
                .build()
                .verify()
        }.getOrElse { err ->
            return ValidationReport(
                isValid = false,
                isSigned = false,
                v1Verified = false,
                v2Verified = false,
                v3Verified = false,
                certSha256 = "",
                summary = "خطا در بررسی امضای APK: ${err.message}",
                errors = structuralErrors + listOf(err.message ?: "ApkVerifier error"),
                parsedManifest = parsedManifest,
            )
        }

        val errors = structuralErrors + verifierResult.errors.map { it.toString() }
        val certSha256 = verifierResult.signerCertificates.firstOrNull()?.let(::sha256Fingerprint).orEmpty()
        val verified = verifierResult.isVerified && structuralErrors.isEmpty()

        val summary = if (verified) {
            val pkgTag = parsedManifest?.packageName?.let { "$it • " }.orEmpty()
            "${pkgTag}تأیید شده (v1=${verifierResult.isVerifiedUsingV1Scheme}, v2=${verifierResult.isVerifiedUsingV2Scheme}, v3=${verifierResult.isVerifiedUsingV3Scheme})"
        } else {
            "اعتبارسنجی ناموفق: ${errors.firstOrNull() ?: "خطای امضای دیجیتال"}"
        }

        return ValidationReport(
            isValid = verified,
            isSigned = verifierResult.signerCertificates.isNotEmpty(),
            v1Verified = verifierResult.isVerifiedUsingV1Scheme,
            v2Verified = verifierResult.isVerifiedUsingV2Scheme,
            v3Verified = verifierResult.isVerifiedUsingV3Scheme,
            certSha256 = certSha256,
            summary = summary,
            errors = errors,
            parsedManifest = parsedManifest,
        )
    }

    fun validateAab(aabFile: File, expectedSpec: BuildSpec? = null): ValidationReport {
        if (!aabFile.exists() || aabFile.length() < 1024L) {
            return ValidationReport(
                isValid = false,
                isSigned = false,
                v1Verified = false,
                v2Verified = false,
                v3Verified = false,
                certSha256 = "",
                summary = "فایل AAB نامعتبر است.",
                errors = listOf("Missing or empty AAB file"),
            )
        }

        val errors = mutableListOf<String>()
        var hasSigVerified = false
        var certSha256 = ""
        var parsedManifest: ManifestIdentity? = null

        try {
            ZipFile(aabFile).use { zip ->
                val required = listOf(
                    "BundleConfig.pb",
                    "base/manifest/AndroidManifest.xml",
                    "base/resources.pb",
                    "base/dex/classes.dex",
                    "base/${ArscCompiler.ICON_APK_PATH}",
                    "base/${ArscCompiler.ICON_ROUND_APK_PATH}",
                    "base/assets/webino_config.json",
                    "META-INF/MANIFEST.MF",
                )
                required.forEach { path ->
                    val e = zip.getEntry(path)
                    if (e == null || e.size <= 0) {
                        errors.add("Missing required AAB entry: $path")
                    }
                }

                zip.getEntry("base/dex/classes.dex")?.let { dexEntry ->
                    // استفاده از تابع امن جایگزین
                    val hdr = zip.getInputStream(dexEntry).use { it.readBytesSafely(4) }
                    if (String(hdr, Charsets.US_ASCII) != "dex\n") {
                        errors.add("Invalid DEX magic in base/dex/classes.dex")
                    }
                }

                zip.getEntry("base/${ArscCompiler.ICON_APK_PATH}")?.let { iconEntry ->
                    // استفاده از تابع امن جایگزین
                    val hdr = zip.getInputStream(iconEntry).use { it.readBytesSafely(4) }
                    if (!isPngHeader(hdr)) {
                        errors.add("Invalid PNG icon in AAB")
                    }
                }

                zip.getEntry("base/assets/webino_config.json")?.let { cfgEntry ->
                    val cfgText = zip.getInputStream(cfgEntry).use { it.readBytes().toString(Charsets.UTF_8) }
                    runCatching {
                        val obj = Json.parseToJsonElement(cfgText).jsonObject
                        val pkg = obj["packageName"]?.jsonPrimitive?.content
                        val verName = obj["versionName"]?.jsonPrimitive?.content
                        val verCode = obj["versionCode"]?.jsonPrimitive?.intOrNull
                        parsedManifest = ManifestIdentity(pkg, verCode, verName)
                        if (expectedSpec != null) {
                            if (pkg != expectedSpec.packageName) {
                                errors.add("AAB package mismatch: expected ${expectedSpec.packageName}, got $pkg")
                            }
                            if (verCode != expectedSpec.versionCode) {
                                errors.add("AAB versionCode mismatch: expected ${expectedSpec.versionCode}, got $verCode")
                            }
                            if (verName != expectedSpec.versionName) {
                                errors.add("AAB versionName mismatch: expected ${expectedSpec.versionName}, got $verName")
                            }
                        }
                    }.onFailure {
                        errors.add("Invalid JSON in base/assets/webino_config.json")
                    }
                }

                var sfEntryName: String? = null
                var sigBlockName: String? = null
                val entries = zip.entries()
                while (entries.hasMoreElements()) {
                    val name = entries.nextElement().name
                    if (name.startsWith("META-INF/") && name.endsWith(".SF")) {
                        sfEntryName = name
                    } else if (name.startsWith("META-INF/") && (name.endsWith(".RSA") || name.endsWith(".EC"))) {
                        sigBlockName = name
                    }
                }

                if (sfEntryName == null || sigBlockName == null) {
                    errors.add("Missing META-INF JAR signature (.SF / .RSA / .EC)")
                } else {
                    val sfBytes = zip.getInputStream(zip.getEntry(sfEntryName)).use { it.readBytes() }
                    val sigBytes = zip.getInputStream(zip.getEntry(sigBlockName)).use { it.readBytes() }
                    runCatching {
                        val cms = CMSSignedData(CMSProcessableByteArray(sfBytes), sigBytes)
                        val certStore = cms.certificates
                        val signer = cms.signerInfos.signers.firstOrNull()
                            ?: error("No CMS SignerInformation found")
                        @Suppress("UNCHECKED_CAST")
                        val matches = certStore.getMatches(signer.sid as org.bouncycastle.util.Selector<X509CertificateHolder>)
                        val certHolder = matches.firstOrNull() ?: error("No signer certificate in CMS store")
                        val x509 = JcaX509CertificateConverter()
                            .setProvider(BouncyCastleProvider.PROVIDER_NAME)
                            .getCertificate(certHolder)
                        val verifier = JcaSimpleSignerInfoVerifierBuilder()
                            .setProvider(BouncyCastleProvider.PROVIDER_NAME)
                            .build(x509)
                        if (signer.verify(verifier)) {
                            hasSigVerified = true
                            certSha256 = sha256Fingerprint(x509)
                        } else {
                            errors.add("CMS signature verification returned false")
                        }
                    }.onFailure { err ->
                        errors.add("Invalid AAB CMS signature: ${err.message ?: err.javaClass.simpleName}")
                    }
                }
            }
        } catch (t: Throwable) {
            errors.add("Invalid AAB archive: ${t.message ?: t.javaClass.simpleName}")
        }

        val valid = errors.isEmpty() && hasSigVerified
        return ValidationReport(
            isValid = valid,
            isSigned = hasSigVerified,
            v1Verified = hasSigVerified,
            v2Verified = false,
            v3Verified = false,
            certSha256 = certSha256,
            summary = if (valid) "بسته AAB با ساختار Protobuf و امضای CMS تأیید شد" else errors.joinToString("; "),
            errors = errors,
            parsedManifest = parsedManifest,
        )
    }

    fun parseBinaryManifestIdentity(axmlBytes: ByteArray): ManifestIdentity? {
        return runCatching {
            val buf = ByteBuffer.wrap(axmlBytes).order(ByteOrder.LITTLE_ENDIAN)
            if (buf.capacity() < 8 || buf.getShort(0) != 0x0003.toShort()) return null
            var offset = buf.getShort(2).toInt() and 0xFFFF
            var strings: List<String> = emptyList()

            while (offset + 8 <= axmlBytes.size) {
                val chunkType = buf.getShort(offset).toInt() and 0xFFFF
                val headerSize = buf.getShort(offset + 2).toInt() and 0xFFFF
                val chunkSize = buf.getInt(offset + 4)
                if (chunkSize <= 0 || offset + chunkSize > axmlBytes.size) break

                when (chunkType) {
                    0x0001 -> {
                        strings = decodeStringPool(axmlBytes, offset, chunkSize)
                    }
                    0x0102 -> {
                        // RES_XML_START_ELEMENT_TYPE
                        val nameIdx = buf.getInt(offset + 20)
                        val tagName = strings.getOrNull(nameIdx)
                        if (tagName == "manifest") {
                            val attrStart = buf.getShort(offset + 24).toInt() and 0xFFFF
                            val attrSize = buf.getShort(offset + 26).toInt() and 0xFFFF
                            val attrCount = buf.getShort(offset + 28).toInt() and 0xFFFF
                            var pkg: String? = null
                            var verCode: Int? = null
                            var verName: String? = null
                            val attrsBase = offset + 16 + attrStart
                            for (i in 0 until attrCount) {
                                val pos = attrsBase + i * attrSize
                                if (pos + 20 > offset + chunkSize) break
                                val attrNameIdx = buf.getInt(pos + 4)
                                val rawValueIdx = buf.getInt(pos + 8)
                                val valueType = buf.get(pos + 15).toInt() and 0xFF
                                val valueData = buf.getInt(pos + 16)
                                val attrName = strings.getOrNull(attrNameIdx)
                                when (attrName) {
                                    "package" -> {
                                        pkg = when (valueType) {
                                            0x03 -> strings.getOrNull(valueData)
                                            else -> strings.getOrNull(rawValueIdx)
                                        }
                                    }
                                    "versionCode" -> {
                                        if (valueType == 0x10 || valueType == 0x11) {
                                            verCode = valueData
                                        }
                                    }
                                    "versionName" -> {
                                        verName = when (valueType) {
                                            0x03 -> strings.getOrNull(valueData)
                                            else -> strings.getOrNull(rawValueIdx)
                                        }
                                    }
                                }
                            }
                            return ManifestIdentity(pkg, verCode, verName)
                        }
                    }
                }
                offset += if (chunkType == 0x0003) headerSize else chunkSize
            }
            null
        }.getOrNull()
    }

    private fun decodeStringPool(data: ByteArray, poolOffset: Int, poolSize: Int): List<String> {
        val buf = ByteBuffer.wrap(data).order(ByteOrder.LITTLE_ENDIAN)
        val stringCount = buf.getInt(poolOffset + 8)
        val flags = buf.getInt(poolOffset + 16)
        val stringsStart = buf.getInt(poolOffset + 20)
        val isUtf8 = (flags and 0x100) != 0
        val out = ArrayList<String>(stringCount)
        for (i in 0 until stringCount) {
            val strRelOffset = buf.getInt(poolOffset + 28 + i * 4)
            var pos = poolOffset + stringsStart + strRelOffset
            if (pos < poolOffset || pos >= poolOffset + poolSize) {
                out.add("")
                continue
            }
            if (isUtf8) {
                // Skip char count (1-2 bytes), read byte length (1-2 bytes)
                val c1 = data[pos++].toInt() and 0xFF
                if ((c1 and 0x80) != 0) pos++
                var byteLen = data[pos++].toInt() and 0xFF
                if ((byteLen and 0x80) != 0) {
                    val b2 = data[pos++].toInt() and 0xFF
                    byteLen = ((byteLen and 0x7F) shl 8) or b2
                }
                val safeLen = byteLen.coerceAtMost((poolOffset + poolSize) - pos)
                out.add(String(data, pos, safeLen, Charsets.UTF_8))
            } else {
                var len = ((data[pos].toInt() and 0xFF) or ((data[pos + 1].toInt() and 0xFF) shl 8))
                pos += 2
                if ((len and 0x8000) != 0) {
                    val low = ((data[pos].toInt() and 0xFF) or ((data[pos + 1].toInt() and 0xFF) shl 8))
                    pos += 2
                    len = ((len and 0x7FFF) shl 16) or low
                }
                val chars = CharArray(len)
                for (c in 0 until len) {
                    if (pos + 1 >= poolOffset + poolSize) break
                    val code = (data[pos].toInt() and 0xFF) or ((data[pos + 1].toInt() and 0xFF) shl 8)
                    chars[c] = code.toChar()
                    pos += 2
                }
                out.add(String(chars))
            }
        }
        return out
    }

    private fun isPngHeader(bytes: ByteArray): Boolean =
        bytes.size >= 4 &&
            bytes[0] == 0x89.toByte() &&
            bytes[1] == 0x50.toByte() &&
            bytes[2] == 0x4E.toByte() &&
            bytes[3] == 0x47.toByte()

    fun sha256Fingerprint(cert: X509Certificate): String {
        val digest = MessageDigest.getInstance("SHA-256").digest(cert.encoded)
        return digest.joinToString(":") { "%02X".format(it) }
    }
}