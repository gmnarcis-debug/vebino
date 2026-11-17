package com.example.buildengine

import java.io.File
import java.security.PrivateKey
import java.security.cert.X509Certificate

/**
 * Cryptographic APK signer (APK Signature Scheme v1, v2, and v3) backed by Google apksig.
 */
object ApkSigner {

    fun sign(
        unsignedApk: File,
        signedApk: File,
        signerName: String,
        privateKey: PrivateKey,
        certificates: List<X509Certificate>,
        minSdkVersion: Int = 24,
    ) {
        signedApk.parentFile?.mkdirs()
        val signerConfig = com.android.apksig.ApkSigner.SignerConfig.Builder(
            signerName.ifBlank { "WEBINO" },
            privateKey,
            certificates,
        ).build()

        com.android.apksig.ApkSigner.Builder(listOf(signerConfig))
            .setInputApk(unsignedApk)
            .setOutputApk(signedApk)
            .setMinSdkVersion(minSdkVersion)
            .setV1SigningEnabled(true)
            .setV2SigningEnabled(true)
            .setV3SigningEnabled(true)
            .setCreatedBy("WEBINO On-Device Builder")
            .build()
            .sign()
    }
}
