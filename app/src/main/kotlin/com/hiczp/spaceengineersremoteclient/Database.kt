package com.hiczp.spaceengineersremoteclient

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import org.jetbrains.anko.db.*

private const val tableName = "profile"

data class Profile(
    val id: Int? = null,
    val name: String,
    val url: String,
    val securityKey: String
)

class DatabaseHelper(context: Context) : ManagedSQLiteOpenHelper(context, "database") {
    override fun onCreate(db: SQLiteDatabase) {
        db.createTable(
            tableName, true,
            "id" to INTEGER + PRIMARY_KEY + AUTOINCREMENT,
            "name" to TEXT + NOT_NULL,
            "url" to TEXT + NOT_NULL,
            "securityKey" to TEXT + NOT_NULL
        )
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {

    }
}

fun SQLiteDatabase.save(profile: Profile) = with(profile) {
    val id = profile.id
    val fields = arrayOf(
        "name" to name,
        "url" to url,
        "securityKey" to securityKey
    )
    if (id == null) {
        insert(tableName, *fields)
    } else {
        update(tableName, *fields).whereSimple("id=?", id.toString()).exec()
    }
}

private val profileParser = classParser<Profile>()

fun SQLiteDatabase.findAll() = select(tableName).exec {
    parseList(profileParser)
}

fun SQLiteDatabase.findById(id: Int) = select(tableName).whereSimple("id=?", id.toString()).exec {
    parseSingle(profileParser)
}