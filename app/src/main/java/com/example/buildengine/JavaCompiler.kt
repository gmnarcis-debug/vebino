package com.example.buildengine

import android.content.Context
import org.eclipse.jdt.core.compiler.CompilationProgress
import org.eclipse.jdt.internal.compiler.batch.Main
import java.io.File
import java.io.PrintWriter
import java.io.StringWriter

/**
 * In-process Java 8 compiler backed by Eclipse JDT Core (ECJ) and our embedded
 * compact android-stubs.jar so Java sources compile directly inside Android's sandbox.
 */
object JavaCompiler {

    data class CompileResult(
        val success: Boolean,
        val classFiles: List<File>,
        val diagnostics: String,
    )

    fun extractStubsJar(context: Context, workDir: File): File {
        val stubsFile = File(workDir, "android-stubs.jar")
        if (!stubsFile.exists() || stubsFile.length() < 1024) {
            context.assets.open("compiler/android-stubs.jar").use { input ->
                stubsFile.outputStream().use { output ->
                    input.copyTo(output)
                }
            }
        }
        return stubsFile
    }

    fun compile(
        sourceFiles: List<File>,
        stubsJar: File,
        classesOutDir: File,
        isCancelled: () -> Boolean = { false },
    ): CompileResult {
        classesOutDir.mkdirs()
        val outWriter = StringWriter()
        val errWriter = StringWriter()

        // فقط از فایل Jar داخل assets استفاده می‌کنیم تا مشکل دسترسی اندروید 10+ پیش نیاید
        val classpathString = stubsJar.absolutePath

        val args = mutableListOf(
            "-1.8",
            "-nowarn",
            "-proc:none",
            "-encoding", "UTF-8",
            "-bootclasspath", classpathString,
            "-classpath", classpathString,
            "-d", classesOutDir.absolutePath,
        )
        sourceFiles.forEach { args.add(it.absolutePath) }

        val progress = object : CompilationProgress() {
            override fun begin(remainingWork: Int) {}
            override fun done() {}
            override fun isCanceled(): Boolean = isCancelled()
            override fun setTaskName(name: String?) {}
            override fun worked(workIncrement: Int, remainingWork: Int) {}
        }

        return try {
            val ok = Main.compile(
                args.toTypedArray(),
                PrintWriter(outWriter),
                PrintWriter(errWriter),
                progress,
            )

            val classFiles = classesOutDir.walkTopDown().filter { it.isFile && it.extension == "class" }.toList()
            val diag = buildString {
                val o = outWriter.toString().trim()
                val e = errWriter.toString().trim()
                if (o.isNotEmpty()) appendLine(o)
                if (e.isNotEmpty()) appendLine(e)
            }.trim()

            CompileResult(
                success = ok && classFiles.isNotEmpty(),
                classFiles = classFiles,
                diagnostics = diag,
            )
        } catch (t: Throwable) {
            val sw = StringWriter()
            t.printStackTrace(PrintWriter(sw))
            CompileResult(
                success = false,
                classFiles = emptyList(),
                diagnostics = "خطای داخلی کامپایلر ECJ:\n${t.message}\n\n${sw.toString()}"
            )
        }
    }
}