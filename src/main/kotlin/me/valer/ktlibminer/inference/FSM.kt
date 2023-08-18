package me.valer.ktlibminer.inference

import com.google.gson.GsonBuilder
import guru.nidi.graphviz.attribute.Attributes
import guru.nidi.graphviz.attribute.Label
import guru.nidi.graphviz.engine.Format
import guru.nidi.graphviz.engine.Graphviz
import guru.nidi.graphviz.model.Factory.mutGraph
import guru.nidi.graphviz.model.Factory.mutNode
import guru.nidi.graphviz.model.Link
import guru.nidi.graphviz.model.MutableNode
import kotlinx.serialization.json.Json
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.JsonConfiguration
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardOpenOption

const val initial = "initial"
const val finish = "doublecircle"

class FSM(val info: String, val edgesDot: Collection<Link>, val nodesDot: Collection<MutableNode>) {
    val states = HashSet<State>()
    var shifts = HashSet<Shift>()
    private var unhandled = true
    val json = Json { prettyPrint = true }


    init {
        nodesDot.forEach {
            if (it.name().toString() == initial) {
                return@forEach
            } else if (it.name().toString() == "0") {
                states.add(State(it.name().toString(), StateType.INIT))
            } else if (it.attrs().get("shape") == finish) {
                states.add(State(it.name().toString(), StateType.FIN))
            } else {
                states.add(State(it.name().toString(), StateType.DEF))
            }
        }
        edgesDot.forEach {
            if (it.from()!!.name().toString() == initial) return@forEach
            shifts.add(
                Shift(
                    it.from()!!.name().toString(),
                    it.to().name().toString(),
                    (it.attrs().get("label") ?: "").toString().split("\\n ").toHashSet()
                )
            )
        }
    }

    fun unionEnd() {
        if (!unhandled) return
        states.forEach {
            val transition =
                shifts.filter { shift -> it.type == StateType.FIN && shift.from == it.name && shift.to != it.name }
            if (transition.isNotEmpty()) it.type = StateType.DEF
        }
        val finStates = states.filter { it.type == StateType.FIN }.toMutableList()
        val finState = finStates.removeLastOrNull()

        finStates.forEach { state ->
            shifts.forEach { shift ->
                if (shift.to == state.name) shift.to = finState!!.name
                if (shift.from == state.name) shift.from = finState!!.name
            }
        }
        if (finState != null) {
            unionWith(finState.name)
        }
        shifts = shifts.distinct().toHashSet()
        states.removeAll(finStates.toSet())
        unhandled = false
    }

    fun unionWith(finName: String) {
        val cycleShifts = shifts.filter { it.to == it.from && it.to == finName }
        val unionWith = hashSetOf<String>()
        cycleShifts.forEach { unionWith.addAll(it.with) }
        cycleShifts.forEach { it.with = unionWith }
    }

    fun toJson(filePath: Path) {
        val `class` = if (info.endsWith("__s")) info.dropLast(3)
        else info
        val automaton = Automaton(info, `class`, shifts, states)
        val strJson = json.encodeToString(automaton)
        Files.deleteIfExists(filePath)
        Files.write(
            filePath,
            listOf(strJson),
            StandardCharsets.UTF_8,
            StandardOpenOption.CREATE,
            StandardOpenOption.WRITE
        )
    }

    fun toDot(filePath: Path) {
        val g = mutGraph("example1").setDirected(true).use { _, _ ->
            states.forEach {
                if (it.type == StateType.FIN) mutNode(it.name).add(Attributes.attr("shape", finish))
                else mutNode(it.name)
            }
            shifts.forEach {
                val link = mutNode(it.from).linkTo(mutNode(it.to)).with(Label.of(it.withToLabel()))
                mutNode(it.from).addLink(link)
            }
        }
        println(g.nodes())
        Graphviz.fromGraph(g).render(Format.DOT).toFile(filePath.toFile())
    }

    @Serializable
    data class Shift(var from: String, var to: String, var with: HashSet<String>) {
        override fun toString(): String = "{from: ${from}, to: ${to}, with: ${with}}"

        fun withToLabel(): String = with.joinToString("\\n ")
    }

    @Serializable
    data class State(val name: String, var type: StateType) {
        override fun toString(): String = "{name: ${name}, type: ${type}}"
    }

    @Serializable
    data class Automaton(val name: String, val `class`: String, val shifts: Set<Shift>, val states: Set<State>)
}