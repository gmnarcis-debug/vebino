package com.example.buildengine

import java.io.File
import java.security.PrivateKey
import java.security.cert.X509Certificate

/**
 * Cryptographically signs APK files via ApkSigner with v1 (JAR), v2, and v3 schemes.
 */
object ApkSignerEngine {

    fun signApk(
        unsignedApk: File,
        signedApk: File,
        signerName: String,
        privateKey: PrivateKey,
        certificates: List<X509Certificate>,
        minSdkVersion: Int = 24,
    ) {
        ApkSigner.sign(
            unsignedApk = unsignedApk,
            signedApk = signedApk,
            signerName = signerName,
            privateKey = privateKey,
            certificates = certificates,
            minSdkVersion = minSdkVersion,
        )
    }
}
