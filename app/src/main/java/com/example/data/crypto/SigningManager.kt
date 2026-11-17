package com.example.data.crypto

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import com.example.buildengine.OutputValidator
import org.bouncycastle.asn1.x500.X500Name
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter
import org.bouncycastle.cert.jcajce.JcaX509v3CertificateBuilder
import org.bouncycastle.jce.provider.BouncyCastleProvider
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder
import java.io.File
import java.math.BigInteger
import java.security.KeyPairGenerator
import java.security.KeyStore
import java.security.MessageDigest
import java.security.PrivateKey
import java.security.SecureRandom
import java.security.Security
import java.security.cert.X509Certificate
import java.util.Date
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

data class CreatedKeystoreInfo(
    val alias: String,
    val keystorePath: String,
    val keystoreType: String,
    val commonName: String,
    val organization: String,
    val country: String,
    val validityYears: Int,
    val sha256Fingerprint: String,
    val sha1Fingerprint: String,
    val encryptedPasswordToken: String,
)

data class LoadedSigningMaterial(
    val alias: String,
    val privateKey: PrivateKey,
    val certificateChain: List<X509Certificate>,
)

/**
 * Manages real PKCS#12 signing keystores, X.509 v3 certificate generation, password
 * encryption via AndroidKeyStore AES-GCM, password rotation, import, and verification.
 */
class SigningManager(private val context: Context) {

    init {
        if (Security.getProvider(BouncyCastleProvider.PROVIDER_NAME) == null) {
            Security.addProvider(BouncyCastleProvider())
        }
    }

    private val keystoresDir: File
        get() = File(context.filesDir, "keystores").apply { mkdirs() }

    fun createNewKeystore(
        alias: String,
        password: String,
        commonName: String,
        organization: String,
        country: String,
        validityYears: Int = 25,
    ): CreatedKeystoreInfo {
        val cleanAlias = alias.trim().ifBlank { "webino_key" }
        val keyGen = KeyPairGenerator.getInstance("RSA").apply {
            initialize(2048, SecureRandom())
        }
        val keyPair = keyGen.generateKeyPair()

        val now = System.currentTimeMillis()
        val notBefore = Date(now - 60_000L)
        val notAfter = Date(now + validityYears.toLong() * 365L * 24L * 3600L * 1000L)
        val dn = X500Name("CN=${sanitizeDn(commonName)}, O=${sanitizeDn(organization)}, C=${sanitizeDn(country)}")

        val certBuilder = JcaX509v3CertificateBuilder(
            dn,
            BigInteger(64, SecureRandom()).abs(),
            notBefore,
            notAfter,
            dn,
            keyPair.public,
        )
        val contentSigner = JcaContentSignerBuilder("SHA256withRSA").build(keyPair.private)
        val certHolder = certBuilder.build(contentSigner)
        val cert = JcaX509CertificateConverter().getCertificate(certHolder)

        val safeName = cleanAlias.replace(Regex("[^A-Za-z0-9_-]"), "_").take(40).ifBlank { "key" }
        val ksFile = File(keystoresDir, "${safeName}_${System.currentTimeMillis()}.p12")
        val ks = KeyStore.getInstance("PKCS12").apply {
            load(null, null)
            setKeyEntry(cleanAlias, keyPair.private, password.toCharArray(), arrayOf(cert))
        }
        ksFile.outputStream().use { fos ->
            ks.store(fos, password.toCharArray())
        }

        return CreatedKeystoreInfo(
            alias = cleanAlias,
            keystorePath = ksFile.absolutePath,
            keystoreType = "PKCS12",
            commonName = commonName,
            organization = organization,
            country = country,
            validityYears = validityYears,
            sha256Fingerprint = OutputValidator.sha256Fingerprint(cert),
            sha1Fingerprint = sha1Fingerprint(cert),
            encryptedPasswordToken = protectSecret(password),
        )
    }

    fun importKeystoreBytes(
        rawBytes: ByteArray,
        preferredFileName: String,
        password: String,
        aliasHint: String? = null,
    ): CreatedKeystoreInfo {
        val candidates = if (preferredFileName.endsWith(".bks", ignoreCase = true)) {
            listOf("BKS", "PKCS12")
        } else {
            listOf("PKCS12", "BKS", "JKS")
        }
        var loaded: KeyStore? = null
        var wrongPassword = false
        for (type in candidates) {
            try {
                loaded = KeyStore.getInstance(type).apply {
                    load(rawBytes.inputStream(), password.toCharArray())
                }
                break
            } catch (e: java.io.IOException) {
                // PKCS12/BKS report a bad password as an IOException caused by UnrecoverableKeyException.
                if (e.cause is java.security.UnrecoverableKeyException ||
                    e.message?.contains("password", ignoreCase = true) == true
                ) {
                    wrongPassword = true
                }
            } catch (_: Exception) {
                // This keystore type is unavailable on Android or does not match the file; try the next one.
            }
        }
        val ks = loaded ?: throw IllegalArgumentException(
            if (wrongPassword) {
                "رمز Keystore اشتباه است."
            } else {
                "فرمت Keystore پشتیبانی نمی‌شود. فایل JKS را با keytool به PKCS12 (.p12) تبدیل کنید."
            }
        )

        val aliases = ks.aliases().toList()
        val chosenAlias = aliasHint?.takeIf { ks.containsAlias(it) }
            ?: aliases.firstOrNull { ks.isKeyEntry(it) }
            ?: throw IllegalArgumentException("هیچ کلید امضای معتبری در فایل Keystore یافت نشد.")

        val key = ks.getKey(chosenAlias, password.toCharArray()) as? PrivateKey
            ?: throw IllegalArgumentException("امکان استخراج کلید خصوصی با رمز واردشده وجود ندارد.")
        val chain = ks.getCertificateChain(chosenAlias)?.filterIsInstance<X509Certificate>()
            ?: listOfNotNull(ks.getCertificate(chosenAlias) as? X509Certificate)
        if (chain.isEmpty()) {
            throw IllegalArgumentException("گواهی X.509 در فایل یافت نشد.")
        }

        val destFile = File(keystoresDir, "imported_${System.currentTimeMillis()}.p12")
        val normalizedKs = KeyStore.getInstance("PKCS12").apply {
            load(null, null)
            setKeyEntry(chosenAlias, key, password.toCharArray(), chain.toTypedArray())
        }
        destFile.outputStream().use { normalizedKs.store(it, password.toCharArray()) }

        val cert = chain.first()
        return CreatedKeystoreInfo(
            alias = chosenAlias,
            keystorePath = destFile.absolutePath,
            keystoreType = "PKCS12",
            commonName = cert.subjectX500Principal.name,
            organization = "Imported",
            country = "IR",
            validityYears = 25,
            sha256Fingerprint = OutputValidator.sha256Fingerprint(cert),
            sha1Fingerprint = sha1Fingerprint(cert),
            encryptedPasswordToken = protectSecret(password),
        )
    }

    fun loadSigningMaterial(
        keystorePath: String,
        alias: String,
        encryptedPasswordToken: String,
    ): LoadedSigningMaterial {
        val password = unprotectSecret(encryptedPasswordToken)
        return loadSigningMaterialWithPassword(keystorePath, alias, password)
    }

    fun loadSigningMaterialWithPassword(
        keystorePath: String,
        alias: String,
        password: String,
    ): LoadedSigningMaterial {
        val file = File(keystorePath)
        require(file.exists()) { "فایل Keystore در مسیر ذخیرهشده وجود ندارد." }
        val ks = KeyStore.getInstance("PKCS12")
        file.inputStream().use { ks.load(it, password.toCharArray()) }
        val key = ks.getKey(alias, password.toCharArray()) as? PrivateKey
            ?: throw IllegalStateException("کلید امضا با این رمز باز نشد.")
        val chain = ks.getCertificateChain(alias)?.filterIsInstance<X509Certificate>()
            ?: listOfNotNull(ks.getCertificate(alias) as? X509Certificate)
        require(chain.isNotEmpty()) { "گواهی دیجیتال نامعتبر است." }
        return LoadedSigningMaterial(alias = alias, privateKey = key, certificateChain = chain)
    }

    fun changeKeystorePassword(
        keystorePath: String,
        alias: String,
        oldEncryptedToken: String,
        newPassword: String,
    ): String {
        val material = loadSigningMaterial(keystorePath, alias, oldEncryptedToken)
        val ks = KeyStore.getInstance("PKCS12").apply {
            load(null, null)
            setKeyEntry(
                alias,
                material.privateKey,
                newPassword.toCharArray(),
                material.certificateChain.toTypedArray(),
            )
        }
        // Write to a temp file first and swap atomically, so a crash can never leave a truncated keystore.
        val target = File(keystorePath)
        val tmp = File(target.parentFile, "${target.name}.tmp")
        tmp.outputStream().use { ks.store(it, newPassword.toCharArray()) }
        if (!tmp.renameTo(target)) {
            target.delete()
            check(tmp.renameTo(target)) { "ذخیره‌ی کلید با رمز جدید ناموفق بود." }
        }
        return protectSecret(newPassword)
    }

    fun verifyKeystoreIntegrity(keystorePath: String, alias: String, encryptedToken: String): Boolean =
        runCatching {
            val mat = loadSigningMaterial(keystorePath, alias, encryptedToken)
            mat.certificateChain.first().checkValidity()
            true
        }.getOrDefault(false)

    private fun sha1Fingerprint(cert: X509Certificate): String {
        val digest = MessageDigest.getInstance("SHA-1").digest(cert.encoded)
        return digest.joinToString(":") { "%02X".format(it) }
    }

    private fun sanitizeDn(value: String): String =
        value.replace(",", " ").replace("=", " ").trim().ifBlank { "WEBINO" }

    private fun protectSecret(plain: String): String {
        return try {
            val secretKey = getOrCreateMasterKey()
            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            cipher.init(Cipher.ENCRYPT_MODE, secretKey)
            val iv = cipher.iv
            val enc = cipher.doFinal(plain.toByteArray(Charsets.UTF_8))
            val combined = ByteArray(1 + iv.size + enc.size)
            combined[0] = iv.size.toByte()
            System.arraycopy(iv, 0, combined, 1, iv.size)
            System.arraycopy(enc, 0, combined, 1 + iv.size, enc.size)
            "AKS:" + Base64.encodeToString(combined, Base64.NO_WRAP)
        } catch (e: Throwable) {
            // The weaker local-seed fallback exists only for JVM unit tests, where the AndroidKeyStore
            // provider is absent. On a real device a keystore failure must surface instead of silently
            // protecting the password with a key stored next to the .p12.
            if (Security.getProvider("AndroidKeyStore") != null) throw e
            val prefsKey = getOrCreateLocalFallbackSecret()
            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            cipher.init(Cipher.ENCRYPT_MODE, prefsKey)
            val iv = cipher.iv
            val enc = cipher.doFinal(plain.toByteArray(Charsets.UTF_8))
            val combined = ByteArray(1 + iv.size + enc.size)
            combined[0] = iv.size.toByte()
            System.arraycopy(iv, 0, combined, 1, iv.size)
            System.arraycopy(enc, 0, combined, 1 + iv.size, enc.size)
            "LOC:" + Base64.encodeToString(combined, Base64.NO_WRAP)
        }
    }

    private fun unprotectSecret(token: String): String {
        return if (token.startsWith("AKS:")) {
            val bytes = Base64.decode(token.removePrefix("AKS:"), Base64.NO_WRAP)
            val ivLen = bytes[0].toInt() and 0xFF
            val iv = bytes.copyOfRange(1, 1 + ivLen)
            val enc = bytes.copyOfRange(1 + ivLen, bytes.size)
            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            cipher.init(Cipher.DECRYPT_MODE, getOrCreateMasterKey(), GCMParameterSpec(128, iv))
            String(cipher.doFinal(enc), Charsets.UTF_8)
        } else {
            val bytes = Base64.decode(token.removePrefix("LOC:"), Base64.NO_WRAP)
            val ivLen = bytes[0].toInt() and 0xFF
            val iv = bytes.copyOfRange(1, 1 + ivLen)
            val enc = bytes.copyOfRange(1 + ivLen, bytes.size)
            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            cipher.init(Cipher.DECRYPT_MODE, getOrCreateLocalFallbackSecret(), GCMParameterSpec(128, iv))
            String(cipher.doFinal(enc), Charsets.UTF_8)
        }
    }

    private fun getOrCreateMasterKey(): SecretKey = synchronized(vaultLock) {
        val ks = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        val alias = "webino_vault_master_key"
        val existing = ks.getKey(alias, null) as? SecretKey
        if (existing != null) return existing

        val kg = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore")
        val spec = KeyGenParameterSpec.Builder(
            alias,
            KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT,
        )
            .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
            .setKeySize(256)
            .build()
        kg.init(spec)
        return kg.generateKey()
    }

    private fun getOrCreateLocalFallbackSecret(): SecretKey = synchronized(vaultLock) {
        val file = File(keystoresDir, ".vault_seed")
        val raw = if (file.exists() && file.length() == 32L) {
            file.readBytes()
        } else {
            ByteArray(32).also { seed ->
                SecureRandom().nextBytes(seed)
                val tmp = File(keystoresDir, ".vault_seed.tmp.${System.nanoTime()}")
                tmp.writeBytes(seed)
                if (!file.exists() || file.length() != 32L) {
                    tmp.renameTo(file)
                } else {
                    tmp.delete()
                }
            }
            if (file.exists() && file.length() == 32L) file.readBytes() else ByteArray(32)
        }
        return javax.crypto.spec.SecretKeySpec(raw, "AES")
    }

    companion object {
        private val vaultLock = Any()
    }
}
