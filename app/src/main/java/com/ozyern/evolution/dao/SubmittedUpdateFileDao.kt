package com.ozyern.evolution.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.ozyern.evolution.models.SubmittedUpdateFile

@Dao
interface SubmittedUpdateFileDao {

    @Query("SELECT EXISTS(SELECT 1 FROM `submitted_update_file` WHERE `name` = :url LIMIT 1)")
    fun isUrlAlreadySubmitted(url: String?): Boolean

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(vararg submittedUpdateFile: SubmittedUpdateFile)
}
