package com.example.buildengine

import java.io.File
import java.io.FileOutputStream
import java.security.PrivateKey
import java.security.cert.X509Certificate
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

/**
 * Packages and delegates cryptographic signing of Android App Bundle (.aab) archives
 * on-device using Protobuf descriptors and AabSigner.
 */
object AabPackager {

    fun packageAndSignAab(
        bundleConfigPb: ByteArray,
        protoManifestPb: ByteArray,
        protoResourcesPb: ByteArray,
        dexBytes: ByteArray,
        iconPng: ByteArray,
        roundIconPng: ByteArray,
        splashPng: ByteArray,
        configJsonBytes: ByteArray,
        outputAab: File,
        signerAlias: String,
        privateKey: PrivateKey,
        certificates: List<X509Certificate>,
    ) {
        outputAab.parentFile?.mkdirs()

        val payloadEntries = linkedMapOf(
            "BundleConfig.pb" to bundleConfigPb,
            "base/manifest/AndroidManifest.xml" to protoManifestPb,
            "base/resources.pb" to protoResourcesPb,
            "base/dex/classes.dex" to dexBytes,
            "base/${ArscCompiler.ICON_APK_PATH}" to iconPng,
            "base/${ArscCompiler.ICON_ROUND_APK_PATH}" to roundIconPng,
            "base/assets/splash.png" to splashPng,
            "base/assets/webino_config.json" to configJsonBytes,
        )

        val sigFiles = AabSigner.generateSignatureFiles(
            payloadEntries = payloadEntries,
            signerAlias = signerAlias,
            privateKey = privateKey,
            certificates = certificates,
        )

        ZipOutputStream(FileOutputStream(outputAab)).use { zos ->
            fun putBytes(name: String, data: ByteArray) {
                zos.putNextEntry(ZipEntry(name))
                zos.write(data)
                zos.closeEntry()
            }
            putBytes("META-INF/MANIFEST.MF", sigFiles.manifestBytes)
            putBytes(sigFiles.signatureFileName, sigFiles.signatureFileBytes)
            putBytes(sigFiles.signatureBlockName, sigFiles.signatureBlockBytes)
            for ((path, data) in payloadEntries) {
                putBytes(path, data)
            }
        }
    }
}
