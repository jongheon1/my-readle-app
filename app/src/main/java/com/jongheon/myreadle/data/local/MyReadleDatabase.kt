package com.jongheon.myreadle.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.jongheon.myreadle.data.local.dao.ArticleDao
import com.jongheon.myreadle.data.local.entity.ArticleEntity

@Database(
    entities = [ArticleEntity::class],
    version = 1,
    exportSchema = false,
)
abstract class MyReadleDatabase : RoomDatabase() {
    abstract fun articleDao(): ArticleDao

    companion object {
        fun build(context: Context): MyReadleDatabase = Room.databaseBuilder(
            context.applicationContext,
            MyReadleDatabase::class.java,
            "my-readle.db",
        )
            .fallbackToDestructiveMigration()
            .build()
    }
}
