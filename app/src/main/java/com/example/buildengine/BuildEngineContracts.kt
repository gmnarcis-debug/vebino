package com.example.buildengine

import android.content.Context
import com.example.domain.model.BuildOutputFormat
import com.example.domain.model.EnvironmentCheckReport
import java.io.File
import java.security.PrivateKey
import java.security.cert.X509Certificate

/**
 * Clean Architecture contracts for WEBINO's modular on-device build pipeline.
 * Each stage has a single clear responsibility and is backed by real local JVM tools.
 */
interface BuildEnvironmentChecker {
    fun inspect(context: Context): EnvironmentCheckReport
}

interface BuildProjectGenerator {
    fun generateSources(spec: BuildSpec, srcRootDir: File): List<File>
}

interface BuildResourceGenerator {
    fun generate(spec: BuildSpec): ResourceGenerator.GeneratedResources
}

interface BuildManifestGenerator {
    fun buildBinaryManifest(spec: BuildSpec): ByteArray
}

interface BuildCompiler {
    fun compileJavaToDex(
        context: Context,
        sourceFiles: List<File>,
        workDir: File,
        minSdk: Int,
        isCancelled: () -> Boolean,
    ): File
}

interface BuildPackager {
    fun packageUnsignedApk(entries: List<ApkPackager.ApkEntry>, outputApk: File)
    fun packageSignedAab(
        spec: BuildSpec,
        manifestNode: AxmlCompiler.XmlNode,
        generatedRes: ResourceGenerator.GeneratedResources,
        dexBytes: ByteArray,
        outputAab: File,
    )
}

interface BuildSigner {
    fun signApk(
        unsignedApk: File,
        signedApk: File,
        signerName: String,
        privateKey: PrivateKey,
        certificates: List<X509Certificate>,
        minSdkVersion: Int,
    )
}

interface BuildValidator {
    fun validate(artifactFile: File, format: BuildOutputFormat, expectedSpec: BuildSpec? = null): OutputValidator.ValidationReport
}

/**
 * Default local JVM implementations wired directly to WEBINO's real on-device tools.
 */
object LocalBuildPipelineAdapters :
    BuildEnvironmentChecker,
    BuildProjectGenerator,
    BuildResourceGenerator,
    BuildManifestGenerator,
    BuildCompiler,
    BuildPackager,
    BuildSigner,
    BuildValidator {

    override fun inspect(context: Context): EnvironmentCheckReport = EnvironmentChecker.inspect(context)

    override fun generateSources(spec: BuildSpec, srcRootDir: File): List<File> =
        ProjectGenerator.generateSources(spec, srcRootDir)

    override fun generate(spec: BuildSpec): ResourceGenerator.GeneratedResources =
        ResourceGenerator.generate(spec)

    override fun buildBinaryManifest(spec: BuildSpec): ByteArray =
        AxmlCompiler.compile(AxmlCompiler.buildManifestNode(spec))

    override fun compileJavaToDex(
        context: Context,
        sourceFiles: List<File>,
        workDir: File,
        minSdk: Int,
        isCancelled: () -> Boolean,
    ): File {
        val stubsJar = JavaCompiler.extractStubsJar(context, workDir)
        val classesDir = File(workDir, "classes")
        val compileRes = JavaCompiler.compile(sourceFiles, stubsJar, classesDir, isCancelled)
        if (isCancelled()) throw BuildCancelledException()
        check(compileRes.success) { compileRes.diagnostics }
        val dexOut = File(workDir, "classes.dex")
        val dexRes = DexGenerator.compileToDex(classesDir, dexOut, minSdk)
        check(dexRes.success) { dexRes.errorMessage ?: "DEX compilation failed" }
        return dexOut
    }

    override fun packageUnsignedApk(entries: List<ApkPackager.ApkEntry>, outputApk: File) {
        ApkPackager.packageUnsignedApk(entries, outputApk)
    }

    override fun packageSignedAab(
        spec: BuildSpec,
        manifestNode: AxmlCompiler.XmlNode,
        generatedRes: ResourceGenerator.GeneratedResources,
        dexBytes: ByteArray,
        outputAab: File,
    ) {
        AabPackager.packageAndSignAab(
            bundleConfigPb = ProtoAabEncoder.encodeBundleConfig(),
            protoManifestPb = ProtoAabEncoder.encodeProtoXmlNode(manifestNode),
            protoResourcesPb = ProtoAabEncoder.encodeProtoResourceTable(
                packageName = spec.packageName,
                appName = spec.appName,
                primaryColorArgb = generatedRes.primaryColorArgb,
                backgroundColorArgb = generatedRes.backgroundColorArgb,
            ),
            dexBytes = dexBytes,
            iconPng = generatedRes.iconPng,
            roundIconPng = generatedRes.roundIconPng,
            splashPng = generatedRes.splashPng,
            configJsonBytes = generatedRes.configJsonBytes,
            outputAab = outputAab,
            signerAlias = spec.keyAlias,
            privateKey = spec.privateKey,
            certificates = spec.certificateChain,
        )
    }

    override fun signApk(
        unsignedApk: File,
        signedApk: File,
        signerName: String,
        privateKey: PrivateKey,
        certificates: List<X509Certificate>,
        minSdkVersion: Int,
    ) {
        ApkSigner.sign(unsignedApk, signedApk, signerName, privateKey, certificates, minSdkVersion)
    }

    override fun validate(
        artifactFile: File,
        format: BuildOutputFormat,
        expectedSpec: BuildSpec?,
    ): OutputValidator.ValidationReport =
        if (format == BuildOutputFormat.AAB) {
            OutputValidator.validateAab(artifactFile, expectedSpec)
        } else {
            OutputValidator.validateApk(artifactFile, expectedSpec)
        }
}
