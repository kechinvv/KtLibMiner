package me.valer.ktlibminer.storage

import java.sql.DriverManager
import kotlin.math.sqrt

object DatabaseController {
    var dbUri = "jdbc:sqlite:libminer.db"
    var conn = DriverManager.getConnection(dbUri)

    fun initDB() {
        openConnection()
        val stmt = conn.createStatement()
        stmt.executeUpdate("drop table if exists sequences")
        stmt.executeUpdate("drop table if exists methods")
        stmt.executeUpdate("create table if not exists sequences(id integer primary key, json_data string, class string, samples integer)")
        stmt.executeUpdate("create table if not exists methods(id integer primary key, method_name string, class string, UNIQUE(method_name,class))")
    }

    fun addMethod(methodName: String, inputClass: String) {
        try {
            val stmt = conn.prepareStatement("insert into methods(method_name, class) values(?,?)")
            stmt.setString(2, inputClass)
            stmt.setString(1, methodName)
            stmt.execute()
        } catch (_: Exception) {}
    }

    fun addTrace(inputData: String, inputClass: String, inputCount: Int = 1) {
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

    fun getMethodsForClass(inputClass: String): HashSet<String> {
        val methodNames = HashSet<String>()
        val stmt = conn.prepareStatement("select method_name from methods where class=?")
        stmt.setString(1, inputClass)
        val res = stmt.executeQuery()
        while (res.next()) {
            val name = res.getString("method_name")
            methodNames.add(name)
        }
        return methodNames
    }

    fun getTracesIdForClass(inputClass: String): HashSet<Int> {
        val idTraces = HashSet<Int>()
        val stmt = conn.prepareStatement("select id from sequences where class=?")
        stmt.setString(1, inputClass)
        val res = stmt.executeQuery()
        while (res.next()) {
            val id = res.getInt("id")
            idTraces.add(id)
        }

        return idTraces
    }


    fun getTracesForClass(inputClass: String): HashSet<String> {
        val jsonTraces = HashSet<String>()
        val stmt = conn.prepareStatement("select json_data from sequences where class=?")
        stmt.setString(1, inputClass)
        val res = stmt.executeQuery()
        while (res.next()) {
            val jsonData = res.getString("json_data")
            jsonTraces.add(jsonData)
        }

        return jsonTraces
    }

    fun getTraceById(id: Int): String? {
        val stmt = conn.prepareStatement("select json_data from sequences where id=?")
        stmt.setInt(1, id)
        val res = stmt.executeQuery()
        if (res.next()) {
            return res.getString("json_data")
        } else return null
    }

    fun getClasses(): HashSet<String> {
        val classes = hashSetOf<String>()
        val stmt = conn.createStatement()
        val res = stmt.executeQuery("select distinct class from sequences")
        while (res.next()) {
            val className = res.getString("class")
            classes.add(className)
        }

        return classes
    }

    fun clearError(): Boolean {
        val classes = getClasses()
        println(classes)
        for (cls in classes) {
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
        return true
    }

    fun openConnection() {
        if (conn.isClosed) conn = DriverManager.getConnection(dbUri)
    }

    fun closeConnection() {
        if (!conn.isClosed) conn.close()
    }
}