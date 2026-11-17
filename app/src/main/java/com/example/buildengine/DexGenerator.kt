package com.example.buildengine

import com.android.dx.command.dexer.Main
import java.io.File

/**
 * In-process Dalvik/ART DEX bytecode compiler powered by dalvik-dx.
 * Converts ECJ-compiled .class files into classes.dex without spawning child processes.
 */
object DexGenerator {

    data class DexResult(
        val success: Boolean,
        val dexFile: File,
        val errorMessage: String? = null,
    )

    fun compileToDex(
        classesRootDir: File,
        dexOutputFile: File,
        minSdk: Int = 24,
    ): DexResult {
        dexOutputFile.parentFile?.mkdirs()
        return try {
            val arguments = Main.Arguments().apply {
                outName = dexOutputFile.absolutePath
                fileNames = arrayOf(classesRootDir.absolutePath)
                minSdkVersion = minSdk
                strictNameCheck = false
                emptyOk = false
                verbose = false
            }
            val exitCode = Main.run(arguments)
            val valid = exitCode == 0 && dexOutputFile.exists() && dexOutputFile.length() > 112L
            DexResult(
                success = valid,
                dexFile = dexOutputFile,
                errorMessage = if (valid) null else "DX exited with code $exitCode",
            )
        } catch (t: Throwable) {
            DexResult(
                success = false,
                dexFile = dexOutputFile,
                errorMessage = t.message ?: t.javaClass.simpleName,
            )
        }
    }
}
