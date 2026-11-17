package com.example.buildengine

import org.bouncycastle.cert.jcajce.JcaCertStore
import org.bouncycastle.cms.CMSProcessableByteArray
import org.bouncycastle.cms.CMSSignedDataGenerator
import org.bouncycastle.cms.jcajce.JcaSignerInfoGeneratorBuilder
import org.bouncycastle.jce.provider.BouncyCastleProvider
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder
import org.bouncycastle.operator.jcajce.JcaDigestCalculatorProviderBuilder
import java.io.ByteArrayOutputStream
import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import java.security.PrivateKey
import java.security.Security
import java.security.cert.X509Certificate
import java.util.Base64

/**
 * Cryptographic JAR / App Bundle (.aab) signer using SHA-256 MANIFEST.MF / .SF
 * digests and BouncyCastle CMS/PKCS#7 (.RSA / .EC) signature generation.
 */
object AabSigner {

    data class JarSignatureFiles(
        val manifestBytes: ByteArray,
        val signatureFileName: String,
        val signatureFileBytes: ByteArray,
        val signatureBlockName: String,
        val signatureBlockBytes: ByteArray,
    )

    init {
        if (Security.getProvider(BouncyCastleProvider.PROVIDER_NAME) == null) {
            Security.addProvider(BouncyCastleProvider())
        }
    }

    fun generateSignatureFiles(
        payloadEntries: Map<String, ByteArray>,
        signerAlias: String,
        privateKey: PrivateKey,
        certificates: List<X509Certificate>,
    ): JarSignatureFiles {
        require(certificates.isNotEmpty()) { "گواهی امضای دیجیتال برای بسته AAB یافت نشد." }
        val sha256 = MessageDigest.getInstance("SHA-256")
        val manifestSb = StringBuilder()
        manifestSb.append("Manifest-Version: 1.0\r\n")
        manifestSb.append("Created-By: WEBINO On-Device Builder\r\n\r\n")

        val sfEntriesSb = StringBuilder()
        for ((path, bytes) in payloadEntries) {
            val digestB64 = Base64.getEncoder().encodeToString(sha256.digest(bytes))
            val entryText = "Name: $path\r\nSHA-256-Digest: $digestB64\r\n\r\n"
            manifestSb.append(entryText)
            val entryHashB64 = Base64.getEncoder().encodeToString(
                sha256.digest(entryText.toByteArray(StandardCharsets.UTF_8))
            )
            sfEntriesSb.append("Name: $path\r\nSHA-256-Digest: $entryHashB64\r\n\r\n")
        }

        val manifestBytes = manifestSb.toString().toByteArray(StandardCharsets.UTF_8)
        val manifestDigestB64 = Base64.getEncoder().encodeToString(sha256.digest(manifestBytes))
        val sfHeader = buildString {
            append("Signature-Version: 1.0\r\n")
            append("Created-By: WEBINO On-Device Builder\r\n")
            append("SHA-256-Digest-Manifest: $manifestDigestB64\r\n\r\n")
            append(sfEntriesSb)
        }
        val sfBytes = sfHeader.toByteArray(StandardCharsets.UTF_8)
        val cmsSignatureBytes = generateCmsSignature(sfBytes, privateKey, certificates)

        val safeAlias = signerAlias.uppercase().replace(Regex("[^A-Z0-9_]"), "").ifBlank { "WEBINO" }.take(8)
        val blockExt = if (privateKey.algorithm.equals("EC", ignoreCase = true)) "EC" else "RSA"

        return JarSignatureFiles(
            manifestBytes = manifestBytes,
            signatureFileName = "META-INF/$safeAlias.SF",
            signatureFileBytes = sfBytes,
            signatureBlockName = "META-INF/$safeAlias.$blockExt",
            signatureBlockBytes = cmsSignatureBytes,
        )
    }

    private fun generateCmsSignature(
        sfBytes: ByteArray,
        privateKey: PrivateKey,
        certificates: List<X509Certificate>,
    ): ByteArray {
        val certStore = JcaCertStore(certificates)
        val signingCert = certificates.first()
        val sigAlg = when {
            privateKey.algorithm.equals("EC", ignoreCase = true) -> "SHA256withECDSA"
            else -> "SHA256withRSA"
        }
        val contentSigner = JcaContentSignerBuilder(sigAlg).build(privateKey)
        val digestCalcProvider = JcaDigestCalculatorProviderBuilder().build()
        val gen = CMSSignedDataGenerator().apply {
            addSignerInfoGenerator(
                JcaSignerInfoGeneratorBuilder(digestCalcProvider)
                    .setDirectSignature(true)
                    .build(contentSigner, signingCert)
            )
            addCertificates(certStore)
        }
        val signedData = gen.generate(CMSProcessableByteArray(sfBytes), false)
        return ByteArrayOutputStream().apply {
            write(signedData.encoded)
        }.toByteArray()
    }
}
