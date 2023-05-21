package me.valer.ktlibminer.inference

import guru.nidi.graphviz.model.Link
import guru.nidi.graphviz.model.MutableNode

class JsonFSM(val className: String, val edgesDot: Collection<Link>, val nodesDot: Collection<MutableNode>) {
    val states = HashSet<State>()
    val shifts = HashSet<Shift>()
    private val initial = "initial"
    private val finish = "doublecircle"

    init {
        nodesDot.forEach {
            if (it.name().toString() == initial) {
                states.add(State(initial, StateType.INIT))
            } else if (it.attrs().get("shape") == finish) {
                states.add(State(it.name().toString(), StateType.FIN))
            } else {
                states.add(State(it.name().toString(), StateType.DEF))
            }
        }
        edgesDot.forEach {
            shifts.add(
                Shift(
                    it.from()!!.name().toString(),
                    it.to().name().toString(),
                    (it.attrs().get("label") ?: "").toString().split("\\n ")
                )
            )
        }


    }

    data class Shift(val from: String, val to: String, val with: List<String>){
        override fun toString(): String = "{from: ${from}, to: ${to}, with: ${with}}"
    }
    data class State(val name: String, val type: StateType){
        override fun toString(): String = "{name: ${name}, type: ${type}}"
    }
}