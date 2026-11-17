package com.example

import android.content.Context
import android.content.pm.PackageManager
import androidx.test.core.app.ApplicationProvider
import com.example.buildengine.BuildSpec
import com.example.buildengine.EnvironmentChecker
import com.example.buildengine.OnDeviceBuildOrchestrator
import com.example.buildengine.OutputValidator
import com.example.data.crypto.SigningManager
import com.example.domain.model.BuildOutputFormat
import com.example.domain.model.BuildStep
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File
import java.io.RandomAccessFile
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.zip.ZipEntry
import java.util.zip.ZipFile

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class OnDeviceBuildEngineTest {

    @Test
    fun `on-device build engine compiles packages signs and validates real APK and AAB`() = runTest {
        val context = ApplicationProvider.getApplicationContext<Context>()

        val env = EnvironmentChecker.inspect(context)
        assertTrue("Environment must be ready: ${env.issues}", env.isReady)
        assertTrue(env.hasEmbeddedStubs)
        assertTrue(env.hasJavaCompiler)
        assertTrue(env.hasDexCompiler)
        assertTrue(env.hasApkSigner)

        val signingManager = SigningManager(context)
        val ksInfo = signingManager.createNewKeystore(
            alias = "webino_poc",
            password = "StrongTestPassword#1405",
            commonName = "WEBINO PoC",
            organization = "WEBINO",
            country = "IR",
        )
        val signingMaterial = signingManager.loadSigningMaterial(
            keystorePath = ksInfo.keystorePath,
            alias = ksInfo.alias,
            encryptedPasswordToken = ksInfo.encryptedPasswordToken,
        )

        val spec = BuildSpec(
            projectId = 1L,
            appName = "وبینو تست",
            packageName = "ir.example.webinotest",
            versionName = "1.2.3",
            versionCode = 12,
            websiteUrl = "https://example.ir",
            developerName = "Webino Team",
            enableDeepLinks = true,
            deepLinkScheme = "webinotest",
            outputFormat = BuildOutputFormat.BOTH,
            keyAlias = signingMaterial.alias,
            privateKey = signingMaterial.privateKey,
            certificateChain = signingMaterial.certificateChain,
        )

        val outputDir = File(context.filesDir, "outputs_test").apply { deleteRecursively(); mkdirs() }
        val orchestrator = OnDeviceBuildOrchestrator(context)
        val seenSteps = mutableSetOf<BuildStep>()
        var lastPercent = 0

        val result = orchestrator.executeBuild(spec, outputDir) { progress ->
            seenSteps.add(progress.step)
            assertTrue("Progress must be monotonic", progress.progressPercent >= lastPercent)
            lastPercent = progress.progressPercent
        }

        assertTrue("Build failed: ${result.errorMessage}\nLogs: ${result.logs.joinToString("\n") { it.message }}", result.success)
        assertEquals(2, result.artifacts.size)
        assertEquals(100, lastPercent)
        assertTrue(seenSteps.contains(BuildStep.COMPILING))
        assertTrue(seenSteps.contains(BuildStep.SIGNING))
        assertTrue(seenSteps.contains(BuildStep.VALIDATING))
        assertTrue(seenSteps.contains(BuildStep.COMPLETED))

        val apkArtifact = result.artifacts.first { it.format == BuildOutputFormat.APK }
        val apkFile = File(apkArtifact.filePath)
        assertTrue(apkFile.exists())

        // Verify APK signature (v1, v2, v3) via official Google ApkVerifier
        val apkValidation = OutputValidator.validateApk(apkFile)
        assertTrue("APK validation failed: ${apkValidation.summary}", apkValidation.isValid)
        assertTrue(apkValidation.v2Verified)
        assertTrue(apkValidation.v3Verified)

        // Verify resources.arsc is STORED and 4-byte aligned
        ZipFile(apkFile).use { zip ->
            val arsc = zip.getEntry("resources.arsc")
            assertNotNull(arsc)
            assertEquals(ZipEntry.STORED, arsc!!.method)
        }
        val arscOffset = findZipEntryDataOffset(apkFile, "resources.arsc")
        assertTrue("resources.arsc offset ($arscOffset) must be 4-byte aligned", arscOffset > 0 && arscOffset % 4L == 0L)

        // Verify Android PackageManager parses our binary AndroidManifest.xml cleanly
        @Suppress("DEPRECATION")
        val pkgInfo = context.packageManager.getPackageArchiveInfo(
            apkFile.absolutePath,
            PackageManager.GET_ACTIVITIES or PackageManager.GET_PERMISSIONS
        )
        assertNotNull("PackageManager failed to parse generated APK manifest", pkgInfo)
        assertEquals("ir.example.webinotest", pkgInfo!!.packageName)
        assertEquals("1.2.3", pkgInfo.versionName)
        assertTrue(pkgInfo.requestedPermissions?.contains("android.permission.INTERNET") == true)
        assertEquals("ir.example.webinotest.MainActivity", pkgInfo.activities?.firstOrNull()?.name)

        // Verify AAB archive structure & CMS JAR signature
        val aabArtifact = result.artifacts.first { it.format == BuildOutputFormat.AAB }
        val aabFile = File(aabArtifact.filePath)
        assertTrue(aabFile.exists())
        val aabValidation = OutputValidator.validateAab(aabFile)
        assertTrue("AAB validation failed: ${aabValidation.summary}", aabValidation.isValid)
    }

    private fun findZipEntryDataOffset(zipFile: File, targetName: String): Long {
        RandomAccessFile(zipFile, "r").use { raf ->
            val len = raf.length()
            val searchStart = (len - 65560L).coerceAtLeast(0L)
            val tail = ByteArray((len - searchStart).toInt())
            raf.seek(searchStart)
            raf.readFully(tail)
            val tailBuf = ByteBuffer.wrap(tail).order(ByteOrder.LITTLE_ENDIAN)
            var eocdIdx = -1
            for (i in tail.size - 22 downTo 0) {
                if (tail[i] == 0x50.toByte() &&
                    tail[i + 1] == 0x4B.toByte() &&
                    tail[i + 2] == 0x05.toByte() &&
                    tail[i + 3] == 0x06.toByte()
                ) {
                    eocdIdx = i
                    break
                }
            }
            if (eocdIdx < 0) return -1L
            val cdSize = tailBuf.getInt(eocdIdx + 12).toLong() and 0xFFFFFFFFL
            val cdOffset = tailBuf.getInt(eocdIdx + 16).toLong() and 0xFFFFFFFFL

            var pos = cdOffset
            val cdEnd = cdOffset + cdSize
            val cdHdr = ByteArray(46)
            while (pos + 46 <= cdEnd) {
                raf.seek(pos)
                raf.readFully(cdHdr)
                val bb = ByteBuffer.wrap(cdHdr).order(ByteOrder.LITTLE_ENDIAN)
                if (bb.getInt(0) != 0x02014B50) break
                val nameLen = bb.getShort(28).toInt() and 0xFFFF
                val extraLen = bb.getShort(30).toInt() and 0xFFFF
                val commentLen = bb.getShort(32).toInt() and 0xFFFF
                val localHeaderOffset = bb.getInt(42).toLong() and 0xFFFFFFFFL
                val nameBytes = ByteArray(nameLen)
                raf.readFully(nameBytes)
                val name = String(nameBytes, Charsets.UTF_8)
                if (name == targetName) {
                    val lfh = ByteArray(30)
                    raf.seek(localHeaderOffset)
                    raf.readFully(lfh)
                    val lfhBuf = ByteBuffer.wrap(lfh).order(ByteOrder.LITTLE_ENDIAN)
                    if (lfhBuf.getInt(0) != 0x04034B50) return -1L
                    val lfhNameLen = lfhBuf.getShort(26).toInt() and 0xFFFF
                    val lfhExtraLen = lfhBuf.getShort(28).toInt() and 0xFFFF
                    return localHeaderOffset + 30L + lfhNameLen + lfhExtraLen
                }
                pos += 46L + nameLen + extraLen + commentLen
            }
        }
        return -1L
    }
}
