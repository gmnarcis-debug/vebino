package com.example.buildengine

import android.app.ActivityManager
import android.content.Context
import android.os.Build
import android.os.StatFs
import com.example.domain.model.EnvironmentCheckReport

/**
 * Checks real device resources (RAM, internal storage) and verifies all embedded
 * on-device compilation engines are present and loadable.
 */
object EnvironmentChecker {

    private const val MIN_REQUIRED_STORAGE_MB = 30L
    private const val MIN_REQUIRED_RAM_MB = 32L

    fun inspect(context: Context): EnvironmentCheckReport {
        val issues = mutableListOf<String>()

        val statFs = StatFs(context.filesDir.absolutePath)
        val statBytes = statFs.availableBlocksLong * statFs.blockSizeLong
        val freeStorageBytes = if (statBytes > 0L) statBytes else context.filesDir.usableSpace
        val freeStorageMb = freeStorageBytes / (1024L * 1024L)
        if (freeStorageMb < MIN_REQUIRED_STORAGE_MB) {
            issues.add("فضای خالی حافظه داخلی کمتر از $MIN_REQUIRED_STORAGE_MB مگابایت است.")
        }

        val runtime = Runtime.getRuntime()
        val maxHeapMb = runtime.maxMemory() / (1024L * 1024L)
        val usedHeapMb = (runtime.totalMemory() - runtime.freeMemory()) / (1024L * 1024L)
        val freeHeapMb = (maxHeapMb - usedHeapMb).coerceAtLeast(0L)

        val am = context.getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager
        val memInfo = ActivityManager.MemoryInfo()
        am?.getMemoryInfo(memInfo)
        val totalDeviceRamMb = if (memInfo.totalMem > 0L) memInfo.totalMem / (1024L * 1024L) else maxHeapMb

        if (freeHeapMb < MIN_REQUIRED_RAM_MB) {
            issues.add("حافظه رم آزاد برای کامپایل کافی نیست.")
        }

        val hasStubs = runCatching {
            context.assets.open("compiler/android-stubs.jar").use { it.read() != -1 }
        }.getOrDefault(false)
        if (!hasStubs) issues.add("فایل کتابخانه پایه android-stubs.jar در بسته برنامه یافت نشد.")

        val hasJavaCompiler = runCatching {
            Class.forName("org.eclipse.jdt.internal.compiler.batch.Main")
            true
        }.getOrDefault(false)
        if (!hasJavaCompiler) issues.add("موتور کامپایلر جاوا (ECJ) در دسترس نیست.")

        val hasDexCompiler = runCatching {
            Class.forName("com.android.dx.command.dexer.Main")
            true
        }.getOrDefault(false)
        if (!hasDexCompiler) issues.add("موتور تولید DEX در دسترس نیست.")

        val hasApkSigner = runCatching {
            Class.forName("com.android.apksig.ApkSigner")
            true
        }.getOrDefault(false)
        if (!hasApkSigner) issues.add("موتور امضای دیجیتال APK در دسترس نیست.")

        val hasCrypto = runCatching {
            Class.forName("org.bouncycastle.jce.provider.BouncyCastleProvider")
            true
        }.getOrDefault(false)
        if (!hasCrypto) issues.add("ماژول رمزنگاری گواهی امضا در دسترس نیست.")

        val engineStatus = when {
            issues.isEmpty() -> com.example.domain.model.BuildEngineStatus.READY
            !hasStubs || !hasJavaCompiler || !hasDexCompiler || !hasApkSigner || !hasCrypto ->
                com.example.domain.model.BuildEngineStatus.UNSUPPORTED
            else -> com.example.domain.model.BuildEngineStatus.ERROR
        }

        return EnvironmentCheckReport(
            isReady = issues.isEmpty(),
            status = engineStatus,
            freeStorageMb = freeStorageMb,
            freeRamMb = freeHeapMb,
            totalRamMb = totalDeviceRamMb,
            hasEmbeddedStubs = hasStubs,
            hasJavaCompiler = hasJavaCompiler,
            hasDexCompiler = hasDexCompiler,
            hasApkSigner = hasApkSigner,
            hasKeystoreCrypto = hasCrypto,
            androidSdkInt = Build.VERSION.SDK_INT,
            issues = issues,
        )
    }
}
