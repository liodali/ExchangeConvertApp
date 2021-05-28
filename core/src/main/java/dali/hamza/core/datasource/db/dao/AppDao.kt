package dali.hamza.core.datasource.db.dao

import androidx.room.*

@Dao
interface AppDao<T> {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: T): Long


    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertOrIgnore(entity: T): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(listEntities: List<T>): List<Long>


    @Update
    suspend fun update(entity: T)

    @Update
    suspend fun updates(entities: List<T>)

    @Delete
    suspend fun delete(entity: T)

    @Delete
    suspend fun deleteAll(listEntities: List<T>)
}