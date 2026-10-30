package com.ozyern.evolution.models

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.ozyern.evolution.utils.Utils
import java.time.LocalDateTime

@Entity(tableName = "submitted_update_file")
data class SubmittedUpdateFile(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,

    @ColumnInfo(index = true)
    val name: String?,

    @ColumnInfo("date_submitted", defaultValue = "CURRENT_TIMESTAMP")
    val dateSubmitted: String = LocalDateTime.now(Utils.ServerTimeZone).toString(),
)
