package me.valer.ktlibminer.inference

import guru.nidi.graphviz.parse.Parser
import me.valer.ktlibminer.Configurations
import me.valer.ktlibminer.storage.DatabaseController.getClasses
import me.valer.ktlibminer.storage.DatabaseController.getMethodsForClass
import me.valer.ktlibminer.storage.DatabaseController.getTraceById
import me.valer.ktlibminer.storage.DatabaseController.getTracesIdForClass
import me.valer.ktlibminer.storage.Jsonator.jsonToTrace
import mint.app.Mint
import java.io.File
import java.io.IOException
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Paths
import java.nio.file.StandardOpenOption
import kotlin.io.path.Path

class FSMInference(val mintFilesPath: String, val jsonAndDotFilesPath: String = mintFilesPath) {


    fun inferenceAll() {
        val klasses = getClasses()
        klasses.forEach {
            inferenceByClass(it)
        }
    }

    fun inferenceByClass(klass: String, toJson: Boolean = true) {
        val ids = getTracesIdForClass(klass)
        val methods = getMethodsForClass(klass)
        val filePathIn = createInputFile(methods, klass)
        ids.forEach {
            val trace = getTraceById(it)
            updateFileTrace(trace!!, filePathIn)
        }
        val filePathOutDot = Path(jsonAndDotFilesPath, klass + "Out.dot").toString()
        inferenceFSM(filePathIn, filePathOutDot)
        val filePathOut = Path(jsonAndDotFilesPath, "$klass.json").toString()
        if (toJson) dotToJson(filePathOutDot, filePathOut, klass)
    }

    fun inferenceFSM(pathIn: String, pathOut: String, k: Int = 2) {
        Files.createDirectories(Paths.get(jsonAndDotFilesPath))
        Mint.main(
            arrayOf(
                "-input",
                pathIn,
                "-k",
                Configurations.kInf.toString(),
                "-visout",
                pathOut
            )
        )
    }

    fun createInputFile(methods: HashSet<String>, klass: String): String {
        val path = Path(mintFilesPath, klass + "In.txt").toString()
        try {
            Files.deleteIfExists(Paths.get(path))
            Files.write(
                Paths.get(path),
                listOf("types"),
                StandardCharsets.UTF_8,
                StandardOpenOption.CREATE,
                StandardOpenOption.WRITE
            )
            methods.forEach {
                Files.write(Paths.get(path), listOf(it), StandardCharsets.UTF_8, StandardOpenOption.APPEND)
            }
           // Files.write(Paths.get(path), listOf("end"), StandardCharsets.UTF_8, StandardOpenOption.APPEND)
        } catch (e: IOException) {
            println(e)
        }
        return path
    }

    fun updateFileTrace(jsonTrace: String, filePath: String) {
        val realTrace = jsonToTrace(jsonTrace)
        try {
            Files.write(Paths.get(filePath), listOf("trace"), StandardCharsets.UTF_8, StandardOpenOption.APPEND)
            realTrace.forEach {
                Files.write(
                    Paths.get(filePath),
                    listOf(it.methodName),
                    StandardCharsets.UTF_8,
                    StandardOpenOption.APPEND
                )
            }
           // Files.write(Paths.get(filePath), listOf("end"), StandardCharsets.UTF_8, StandardOpenOption.APPEND)
        } catch (e: IOException) {
            println(e)
        }
    }

    fun dotToJson(pathDot: String, pathJson: String, klass: String) {
        val dot = File(pathDot).inputStream()
        val g = Parser().read(dot)
        val fsm = FSM(klass, g.edges(), g.nodes())
        fsm.toJson(Path(pathJson))
    }

}