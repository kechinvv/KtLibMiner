package me.valer.ktlibminer

import java.sql.DriverManager

object DatabaseController {
    var dbUri = "jdbc:sqlite:libminer.db"

    fun initDB() {
        DriverManager.getConnection(dbUri).use { conn ->
            val stmt = conn.createStatement()
            stmt.executeUpdate("drop table if exists sequences")
            stmt.executeUpdate("create table if not exists sequences(id, json_data string, samples integer)")
        }
    }

    fun addData(inputData: String, inputCount: Int = 1) {
        DriverManager.getConnection(dbUri).use { conn ->
            var stmt = conn.prepareStatement("select samples from sequences where json_data=?")
            stmt.setString(1, inputData)
            val res = stmt.executeQuery()
            if (res.next()) {
                val count = res.getInt("samples") + inputCount
                stmt = conn.prepareStatement("update sequences set samples=? where json_data=?")
                stmt.setInt(1, count)
                stmt.setString(2, inputData)
                stmt.executeQuery()
            } else {
                stmt = conn.prepareStatement("insert into sequences(json_data, samples) values(?,?)")
                stmt.setInt(2, inputCount)
                stmt.setString(1, inputData)
                stmt.executeQuery()
            }
        }
    }
}