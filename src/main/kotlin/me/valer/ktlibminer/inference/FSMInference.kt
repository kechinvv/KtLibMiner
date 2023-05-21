package me.valer.ktlibminer.inference

import com.google.gson.JsonElement
import com.google.gson.JsonObject
import guru.nidi.graphviz.parse.Parser
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

class FSMInference(val source: String, val dest: String = source) {


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
        val filePathOut = Path(dest, klass + "Out.dot").toString()
        inferenceFSM(filePathIn, filePathOut)
        if (toJson) dotToJson(filePathOut, klass)
    }

    fun inferenceFSM(pathIn: String, pathOut: String, k: Int = 2) {
        Mint.main(
            arrayOf(
                "-input",
                pathIn,
                "-k",
                k.toString(),
                "-visout",
                pathOut
            )
        )
    }

    fun createInputFile(methods: HashSet<String>, klass: String): String {
        val path = Path(source, klass + "In.txt").toString()
        try {
            Files.write(Paths.get(path), listOf("types\n"), StandardCharsets.UTF_8, StandardOpenOption.WRITE)
            methods.forEach {
                Files.write(Paths.get(path), listOf("${it}\n"), StandardCharsets.UTF_8, StandardOpenOption.APPEND)
            }
            Files.write(Paths.get(path), listOf("end\n"), StandardCharsets.UTF_8, StandardOpenOption.APPEND)
        } catch (_: IOException) {
        }
        return path
    }

    fun updateFileTrace(jsonTrace: String, filePath: String) {
        val realTrace = jsonToTrace(jsonTrace)
        try {
            Files.write(Paths.get(filePath), listOf("trace\n"), StandardCharsets.UTF_8, StandardOpenOption.APPEND)
            realTrace.forEach {
                Files.write(
                    Paths.get(filePath),
                    listOf("${it.methodName}\n"),
                    StandardCharsets.UTF_8,
                    StandardOpenOption.APPEND
                )
            }
            Files.write(Paths.get(filePath), listOf("end\n"), StandardCharsets.UTF_8, StandardOpenOption.APPEND)
        } catch (_: IOException) {
        }
    }

    fun dotToJson(path: String, klass: String) {
        val dot = File(path).inputStream()
        val g =  Parser().read(dot)
        val rootJson = JsonObject()
        val a = g.nodes()
        g.edges().forEach {
            println(it.name())
            println(it.attrs())
        }
        g.nodes().forEach {
            println(it.name())
        }
    }

}