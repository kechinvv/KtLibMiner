package me.valer.ktlibminer.inference

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import guru.nidi.graphviz.parse.Parser
import me.valer.ktlibminer.config.Configurations
import me.valer.ktlibminer.storage.DatabaseController.getClasses
import me.valer.ktlibminer.storage.DatabaseController.getMethodsForClass
import me.valer.ktlibminer.storage.DatabaseController.getTraceById
import me.valer.ktlibminer.storage.DatabaseController.getTracesIdForClass
import mint.app.Mint
import java.io.IOException
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.nio.file.StandardOpenOption
import kotlin.io.path.Path

class FSMInference(val mintFilesPath: String, val jsonAndDotFilesPath: String = mintFilesPath) {


    fun inferenceAll(toJson: Boolean = Configurations.toJson, unionEnd: Boolean = Configurations.unionEnd) {
        val klasses = getClasses()
        klasses.forEach {
            inferenceByClass(it, toJson, unionEnd)
        }
    }

    fun inferenceByClass(klass: String, toJson: Boolean = true, unionEnd: Boolean = Configurations.unionEnd) {
        val ids = getTracesIdForClass(klass)
        val methods = getMethodsForClass(klass)
        val klassStr = klass.replace(".", "+")
        val filePathIn = createInputFile(methods, klassStr)
        ids.forEach {
            val trace = getTraceById(it)
            updateFileTrace(trace!!, filePathIn)
        }

        val filePathOutDot = Path(jsonAndDotFilesPath, klassStr + "Out.dot")
        Files.deleteIfExists(filePathOutDot)
        inferenceFSM(filePathIn.toString(), filePathOutDot.toString())

        val filePathOut = Path(jsonAndDotFilesPath, "$klassStr.json")
          val fsm = dotToFSM(filePathOutDot, klass)
        if (unionEnd) {
            val filePathOutUnionDot = Path(jsonAndDotFilesPath, klassStr + "OutUnion.dot")
            fsm.unionEnd()
            fsm.toDot(filePathOutUnionDot)
        }
         if (toJson) fsm.toJson(filePathOut)
    }

    fun inferenceFSM(pathIn: String, pathOut: String, k: Int = Configurations.kAlg) {
        Files.createDirectories(Paths.get(jsonAndDotFilesPath))
        Mint.main(
            arrayOf(
                "-input",
                pathIn,
                "-k",
                k.toString(),
                "-strategy",
                "ktails",
                "-visout",
                pathOut
            )
        )
    }

    fun createInputFile(methods: HashSet<String>, klass: String): Path {
        val path = Path(mintFilesPath, klass + "In.txt")
        try {
            Files.deleteIfExists(path)
            Files.write(
                path,
                listOf("types"),
                StandardCharsets.UTF_8,
                StandardOpenOption.CREATE,
                StandardOpenOption.WRITE
            )
            methods.forEach {
                Files.write(path, listOf(it), StandardCharsets.UTF_8, StandardOpenOption.APPEND)
            }
            // Files.write(Paths.get(path), listOf("end"), StandardCharsets.UTF_8, StandardOpenOption.APPEND)
        } catch (e: IOException) {
            println(e)
        }
        return path
    }

    fun updateFileTrace(jsonTrace: String, filePath: Path) {
        val realTrace: List<String> = Gson().fromJson(jsonTrace, object : TypeToken<List<String>>() {}.type)
        try {
            Files.write(filePath, listOf("trace"), StandardCharsets.UTF_8, StandardOpenOption.APPEND)
            realTrace.forEach {
                Files.write(
                    filePath,
                    listOf(it),
                    StandardCharsets.UTF_8,
                    StandardOpenOption.APPEND
                )
            }
            // Files.write(Paths.get(filePath), listOf("end"), StandardCharsets.UTF_8, StandardOpenOption.APPEND)
        } catch (e: IOException) {
            println(e)
        }
    }

    fun dotToFSM(pathDot: Path, klass: String): FSM {
        val dot = pathDot.toFile().inputStream()
        val g = Parser().read(dot)
        return FSM(klass, g.edges(), g.nodes())
    }

}