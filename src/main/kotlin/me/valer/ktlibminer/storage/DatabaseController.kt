package me.valer.ktlibminer.storage

import java.sql.DriverManager
import kotlin.math.sqrt

object DatabaseController {
    var dbUri = "jdbc:sqlite:libminer.db"

    fun initDB() {
        DriverManager.getConnection(dbUri).use { conn ->
            val stmt = conn.createStatement()
            stmt.executeUpdate("drop table if exists sequences")
            stmt.executeUpdate("create table if not exists sequences(id integer primary key, json_data string, class string, samples integer)")
        }
    }

    fun addData(inputData: String, inputClass: String, inputCount: Int = 1) {
        DriverManager.getConnection(dbUri).use { conn ->
            var stmt = conn.prepareStatement("select samples from sequences where json_data=?")
            stmt.setString(1, inputData)
            val res = stmt.executeQuery()
            if (res.next()) {
                val count = res.getInt("samples") + inputCount
                stmt = conn.prepareStatement("update sequences set samples=? where json_data=?")
                stmt.setInt(1, count)
                stmt.setString(2, inputData)
                stmt.execute()
            } else {
                stmt = conn.prepareStatement("insert into sequences(json_data, class, samples) values(?,?,?)")
                stmt.setInt(3, inputCount)
                stmt.setString(2, inputClass)
                stmt.setString(1, inputData)
                stmt.execute()
            }
        }
    }

    fun getTracesForClass(inputClass: String): MutableList<String> {
        val jsonTraces = mutableListOf<String>()
        DriverManager.getConnection(dbUri).use { conn ->
            val stmt = conn.prepareStatement("select json_data from sequences where class=?")
            stmt.setString(1, inputClass)
            val res = stmt.executeQuery()
            while (res.next()) {
                val jsonData = res.getString("json_data")
                jsonTraces.add(jsonData)
            }
        }
        return jsonTraces
    }

    fun getClasses(): HashSet<String> {
        val classes = hashSetOf<String>()
        DriverManager.getConnection(dbUri).use { conn ->
            val stmt = conn.createStatement()
            val res = stmt.executeQuery("select distinct class from sequences")
            while (res.next()) {
                val className = res.getString("class")
                classes.add(className)
            }
        }
        return classes
    }

    fun clearError(): Boolean {
        val classes = getClasses()
        println(classes)
        for (cls in classes) {
            DriverManager.getConnection(dbUri).use { conn ->
                val stmtD =
                    conn.prepareStatement("select (AVG(samples*samples) - AVG(samples)*AVG(samples)) as disp from sequences where class = ?")
                stmtD.setString(1, cls)
                val dsp = stmtD.executeQuery()
                var d: Double? = null
                var m: Double? = null
                if (dsp.next()) d = sqrt(dsp.getDouble("disp")) * 3
                val stmtM = conn.prepareStatement("select AVG(samples) as mean from sequences where class = ?")
                stmtM.setString(1, cls)
                val mean = stmtM.executeQuery()
                if (mean.next()) m = mean.getDouble("mean")
                val stmtDel = conn.prepareStatement("delete from sequences where class = ? and samples < ?")
                if (m != null && d != null) {
                    println(m)
                    println(d)
                    stmtDel.setString(1, cls)
                    stmtDel.setDouble(2, m - d)
                    stmtDel.execute()
                } else return false
            }
        }
        return true
    }
}