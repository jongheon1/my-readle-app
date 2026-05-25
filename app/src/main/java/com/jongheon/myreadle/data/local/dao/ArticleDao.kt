package com.jongheon.myreadle.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.jongheon.myreadle.data.local.entity.ArticleEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ArticleDao {

    @Query("SELECT * FROM articles ORDER BY date DESC, topicId ASC")
    fun observeAll(): Flow<List<ArticleEntity>>

    @Query("SELECT DISTINCT date FROM articles")
    suspend fun allDates(): List<String>

    @Query("SELECT * FROM articles WHERE topicId = :topicId LIMIT 1")
    suspend fun byId(topicId: String): ArticleEntity?

    @Query("SELECT * FROM articles WHERE topicId = :topicId LIMIT 1")
    fun observeById(topicId: String): Flow<ArticleEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(entities: List<ArticleEntity>)

    @Query("DELETE FROM articles WHERE date < :cutoffDate")
    suspend fun deleteOlderThan(cutoffDate: String)
}
