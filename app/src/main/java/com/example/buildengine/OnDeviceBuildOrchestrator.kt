package com.example.buildengine

import android.content.Context
import com.example.domain.model.BuildOutputFormat
import com.example.domain.model.BuildStep
import com.example.domain.model.EnvironmentCheckReport
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext
import java.io.File
import kotlin.coroutines.coroutineContext

/**
 * Standalone On-Device Build Engine Orchestrator implementing [BuildEngine].
 * Executes all real build stages:
 * 1. Checking Environment & Pre-Build Validation -> 2. Preparing Resources ->
 * 3. Generating Project -> 4. Compiling Java & DEX -> 5. Packaging APK/AAB ->
 * 6. Signing -> 7. Validating -> 8. Completed.
 */
class OnDeviceBuildOrchestrator(private val context: Context) : BuildEngine {

    override fun inspectEnvironment(): EnvironmentCheckReport = EnvironmentChecker.inspect(context)

    override fun validateConfiguration(spec: BuildSpec): BuildConfiguration.ValidationResult =
        BuildConfiguration.validate(spec)

    override fun estimateStorage(spec: BuildSpec): BuildStorageManager.StorageEstimate =
        BuildStorageManager.estimateBuildStorage(context, spec)

    override suspend fun executeBuild(
        spec: BuildSpec,
        outputDir: File,
        isCancelled: () -> Boolean,
        onProgress: suspend (BuildProgressUpdate) -> Unit,
    ): BuildExecutionResult = withContext(Dispatchers.IO) {
        val startTime = System.currentTimeMillis()
        var currentStep = BuildStep.CHECKING_ENV
        val logs = mutableListOf<BuildLogEntry>()
        val artifacts = mutableListOf<GeneratedArtifactInfo>()
        val safePkgSlug = spec.packageName.replace('.', '_')
        val workDir = BuildStorageManager.createWorkDir(context, spec.projectId, startTime)
        outputDir.mkdirs()

        fun checkCancelled() {
            if (!coroutineContext.isActive || isCancelled()) {
                throw BuildCancelledException()
            }
        }

        suspend fun logAndEmit(step: BuildStep, percent: Int, msg: String, level: String = "INFO") {
            coroutineContext.ensureActive()
            checkCancelled()
            currentStep = step
            val entry = BuildLogEntry(timestamp = System.currentTimeMillis(), level = level, step = step, message = msg)
            logs.add(entry)
            onProgress(
                BuildProgressUpdate(
                    step = step,
                    progressPercent = percent,
                    statusMessage = msg,
                    logEntry = entry,
                )
            )
        }

        try {
            // Stage 1: Input Validation, Storage Estimation & Environment Check
            logAndEmit(BuildStep.CHECKING_ENV, 3, "اعتبارسنجی مشخصات پروژه و برآورد فضای ذخیره‌سازی...")
            val configResult = validateConfiguration(spec)
            if (!configResult.isValid) {
                throw IllegalArgumentException(configResult.errors.joinToString("\n"))
            }
            val config = BuildConfiguration(spec)

            val storageEst = estimateStorage(spec)
            if (!storageEst.hasEnoughSpace) {
                throw IllegalStateException(storageEst.summaryFa)
            }
            logAndEmit(BuildStep.CHECKING_ENV, 6, storageEst.summaryFa)

            val env = inspectEnvironment()
            if (!env.isReady) {
                throw IllegalStateException(env.issues.joinToString(" | "))
            }
            logAndEmit(
                BuildStep.CHECKING_ENV,
                10,
                "محیط آماده است • حافظه آزاد: ${env.freeStorageMb}MB • رم آزاد: ${env.freeRamMb}MB"
            )

            // Stage 2: Prepare Resources
            logAndEmit(BuildStep.PREPARING_RESOURCES, 15, "در حال تولید آیکونهای تطبیقی و پیکربندی منابع برنامه...")
            val generatedRes = ResourceGenerator.generate(spec)
            val arscBytes = ArscCompiler.compile(
                packageName = spec.packageName,
                appName = spec.appName,
                primaryColorArgb = generatedRes.primaryColorArgb,
                backgroundColorArgb = generatedRes.backgroundColorArgb,
            )
            val manifestNode = AxmlCompiler.buildManifestNode(spec)
            val binaryManifestBytes = AxmlCompiler.compile(manifestNode)
            logAndEmit(
                BuildStep.PREPARING_RESOURCES,
                22,
                "جدول منابع resources.arsc (${arscBytes.size} بایت) و مانیفست باینری (${binaryManifestBytes.size} بایت) آماده شد."
            )

            // Stage 3: Generate Project Sources
            logAndEmit(BuildStep.GENERATING_PROJECT, 28, "تولید سورسکد جاوا برای بسته ${spec.packageName}...")
            val srcDir = File(workDir, "src")
            val sourceFiles = ProjectGenerator.generateSources(spec, srcDir)
            val stubsJar = JavaCompiler.extractStubsJar(context, workDir)
            logAndEmit(
                BuildStep.GENERATING_PROJECT,
                36,
                "تولید ${sourceFiles.size} فایل سورس جاوا و بارگذاری کتابخانه پایه اندروید انجام شد."
            )

            // Stage 4: Compile Java -> .class -> classes.dex
            logAndEmit(BuildStep.COMPILING, 44, "کامپایل سورسکد جاوا با موتور ECJ روی دستگاه...")
            val classesDir = File(workDir, "classes")
            val compileResult = JavaCompiler.compile(
                sourceFiles = sourceFiles,
                stubsJar = stubsJar,
                classesOutDir = classesDir,
                isCancelled = { !coroutineContext.isActive || isCancelled() },
            )
            checkCancelled()
            if (!compileResult.success) {
                throw IllegalStateException("خطا در کامپایل جاوا:\n${compileResult.diagnostics}")
            }
            logAndEmit(
                BuildStep.COMPILING,
                50,
                "کامپایل جاوا موفق بود (${compileResult.classFiles.size} فایل بایتکد .class). در حال تبدیل به DEX..."
            )

            val dexFile = File(workDir, "classes.dex")
            val dexResult = DexGenerator.compileToDex(
                classesRootDir = classesDir,
                dexOutputFile = dexFile,
                minSdk = spec.minSdk,
            )
            checkCancelled()
            if (!dexResult.success) {
                throw IllegalStateException("خطا در تولید DEX: ${dexResult.errorMessage}")
            }
            val dexBytes = dexFile.readBytes()
            logAndEmit(
                BuildStep.COMPILING,
                58,
                "بایتکد Dalvik/ART (classes.dex) به حجم ${dexBytes.size} بایت ساخته شد."
            )

            val timestampTag = startTime
            val certSha256 = spec.certificateChain.firstOrNull()?.let { OutputValidator.sha256Fingerprint(it) }.orEmpty()

            var unsignedApk: File? = null
            var signedApk: File? = null
            var bundleConfigPb: ByteArray? = null
            var protoManifestPb: ByteArray? = null
            var protoResourcesPb: ByteArray? = null
            var signedAab: File? = null

            // Stage 5: Packaging
            if (config.wantApk) {
                logAndEmit(BuildStep.PACKAGING, 64, "بستهبندی و همترازی ۴بایتی (ZipAlign) فایل APK...")
                val apkRaw = File(workDir, "unsigned.apk")
                val entries = listOf(
                    ApkPackager.ApkEntry("AndroidManifest.xml", binaryManifestBytes, storedAligned = false),
                    ApkPackager.ApkEntry("resources.arsc", arscBytes, storedAligned = true),
                    ApkPackager.ApkEntry("classes.dex", dexBytes, storedAligned = false),
                    ApkPackager.ApkEntry(ArscCompiler.ICON_APK_PATH, generatedRes.iconPng, storedAligned = true),
                    ApkPackager.ApkEntry(ArscCompiler.ICON_ROUND_APK_PATH, generatedRes.roundIconPng, storedAligned = true),
                    ApkPackager.ApkEntry("assets/splash.png", generatedRes.splashPng, storedAligned = true),
                    ApkPackager.ApkEntry("assets/webino_config.json", generatedRes.configJsonBytes, storedAligned = false),
                )
                ApkPackager.packageUnsignedApk(entries, apkRaw)
                unsignedApk = apkRaw
                logAndEmit(BuildStep.PACKAGING, 70, "فایل APK خام ساخته شد (${apkRaw.length()} بایت).")
            }
            if (config.wantAab) {
                logAndEmit(BuildStep.PACKAGING, 74, "ساخت ساختار Protobuf و بستهبندی Android App Bundle (.aab)...")
                bundleConfigPb = ProtoAabEncoder.encodeBundleConfig()
                protoManifestPb = ProtoAabEncoder.encodeProtoXmlNode(manifestNode)
                protoResourcesPb = ProtoAabEncoder.encodeProtoResourceTable(
                    packageName = spec.packageName,
                    appName = spec.appName,
                    primaryColorArgb = generatedRes.primaryColorArgb,
                    backgroundColorArgb = generatedRes.backgroundColorArgb,
                )
            }

            // Stage 6: Signing
            if (config.wantApk && unsignedApk != null) {
                logAndEmit(BuildStep.SIGNING, 80, "اعمال امضای دیجیتال v1 + v2 + v3 با گواهی ${spec.keyAlias}...")
                val apkOut = File(outputDir, "${safePkgSlug}_v${spec.versionName}_$timestampTag.apk")
                ApkSigner.sign(
                    unsignedApk = unsignedApk,
                    signedApk = apkOut,
                    signerName = spec.keyAlias,
                    privateKey = spec.privateKey,
                    certificates = spec.certificateChain,
                    minSdkVersion = spec.minSdk,
                )
                signedApk = apkOut
            }
            if (config.wantAab && bundleConfigPb != null && protoManifestPb != null && protoResourcesPb != null) {
                logAndEmit(BuildStep.SIGNING, 86, "امضای CMS/PKCS#7 بسته AAB...")
                val aabOut = File(outputDir, "${safePkgSlug}_v${spec.versionName}_$timestampTag.aab")
                AabPackager.packageAndSignAab(
                    bundleConfigPb = bundleConfigPb,
                    protoManifestPb = protoManifestPb,
                    protoResourcesPb = protoResourcesPb,
                    dexBytes = dexBytes,
                    iconPng = generatedRes.iconPng,
                    roundIconPng = generatedRes.roundIconPng,
                    splashPng = generatedRes.splashPng,
                    configJsonBytes = generatedRes.configJsonBytes,
                    outputAab = aabOut,
                    signerAlias = spec.keyAlias,
                    privateKey = spec.privateKey,
                    certificates = spec.certificateChain,
                )
                signedAab = aabOut
            }

            // Stage 7: Validating
            if (signedApk != null) {
                logAndEmit(BuildStep.VALIDATING, 91, "اعتبارسنجی رمزنگاری امضا و ساختار ۱۰گانه فایل APK...")
                val validation = OutputValidator.validateApk(signedApk, expectedSpec = spec)
                if (!validation.isValid) {
                    signedApk.delete()
                    throw IllegalStateException("اعتبارسنجی خروجی APK ناموفق بود: ${validation.summary}")
                }
                logAndEmit(BuildStep.VALIDATING, 94, "خروجی APK تأیید شد: ${validation.summary}")
                artifacts.add(
                    GeneratedArtifactInfo(
                        format = BuildOutputFormat.APK,
                        filePath = signedApk.absolutePath,
                        fileSizeBytes = signedApk.length(),
                        isSigned = validation.isSigned,
                        isVerified = validation.isValid,
                        sha256CertFingerprint = validation.certSha256.ifEmpty { certSha256 },
                        validationSummary = validation.summary,
                    )
                )
            }
            if (signedAab != null) {
                logAndEmit(BuildStep.VALIDATING, 96, "اعتبارسنجی ساختار Protobuf و امضای CMS بسته AAB...")
                val validation = OutputValidator.validateAab(signedAab, expectedSpec = spec)
                if (!validation.isValid) {
                    signedAab.delete()
                    throw IllegalStateException("اعتبارسنجی خروجی AAB ناموفق بود: ${validation.summary}")
                }
                logAndEmit(BuildStep.VALIDATING, 98, "خروجی AAB تأیید شد: ${validation.summary}")
                artifacts.add(
                    GeneratedArtifactInfo(
                        format = BuildOutputFormat.AAB,
                        filePath = signedAab.absolutePath,
                        fileSizeBytes = signedAab.length(),
                        isSigned = validation.isSigned,
                        isVerified = validation.isValid,
                        sha256CertFingerprint = validation.certSha256.ifEmpty { certSha256 },
                        validationSummary = validation.summary,
                    )
                )
            }

            val totalMs = System.currentTimeMillis() - startTime
            logAndEmit(BuildStep.COMPLETED, 100, "ساخت با موفقیت کامل شد (${artifacts.size} خروجی معتبر در ${totalMs}ms).")

            BuildExecutionResult(
                success = true,
                artifacts = artifacts,
                logs = logs,
                durationMs = totalMs,
            )
        } catch (cancelled: BuildCancelledException) {
            artifacts.forEach { File(it.filePath).delete() }
            val msg = cancelled.message ?: "عملیات ساخت توسط کاربر لغو شد."
            logs.add(
                BuildLogEntry(
                    timestamp = System.currentTimeMillis(),
                    level = "WARN",
                    step = currentStep,
                    message = msg,
                )
            )
            BuildExecutionResult(
                success = false,
                artifacts = emptyList(),
                logs = logs,
                durationMs = System.currentTimeMillis() - startTime,
                errorMessage = msg,
                cancelled = true,
            )
        } catch (t: Throwable) {
            artifacts.forEach { File(it.filePath).delete() }
            val errMsg = t.message ?: t.javaClass.simpleName
            logs.add(
                BuildLogEntry(
                    timestamp = System.currentTimeMillis(),
                    level = "ERROR",
                    step = currentStep,
                    message = errMsg,
                )
            )
            BuildExecutionResult(
                success = false,
                artifacts = emptyList(),
                logs = logs,
                durationMs = System.currentTimeMillis() - startTime,
                errorMessage = errMsg,
                cancelled = false,
            )
        } finally {
            BuildStorageManager.cleanupWorkDir(workDir)
        }
    }
}
