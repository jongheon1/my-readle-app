package com.jongheon.myreadle.domain.model

enum class Level(val displayName: String, val order: Int) {
    A2("A2", 0),
    B1("B1", 1),
    B2("B2", 2),
    C1("C1", 3);

    companion object {
        fun fromKey(key: String): Level? = entries.firstOrNull { it.name == key }
        val Default: Level = B1
    }
}
