package com.ozyern.evolution.database

import androidx.room.AutoMigration
import androidx.room.Database
import androidx.room.DeleteColumn
import androidx.room.RenameColumn
import androidx.room.RewriteQueriesToDropUnusedColumns
import androidx.room.RoomDatabase
import androidx.room.migration.AutoMigrationSpec
import com.ozyern.evolution.dao.ArticleDao
import com.ozyern.evolution.dao.SubmittedUpdateFileDao
import com.ozyern.evolution.dao.UpdateDataDao
import com.ozyern.evolution.models.Article
import com.ozyern.evolution.models.SubmittedUpdateFile
import com.ozyern.evolution.models.UpdateData

@RewriteQueriesToDropUnusedColumns
@Database(
    version = 2,
    entities = [
        Article::class,
        SubmittedUpdateFile::class,
        UpdateData::class,
    ],
    autoMigrations = [
        AutoMigration(1, 2, LocalAppDb.NewsItemRemoveLanguageSpecificity::class),
    ],
)
abstract class LocalAppDb : RoomDatabase() {

    abstract fun articleDao(): ArticleDao
    abstract fun submittedUpdateFileDao(): SubmittedUpdateFileDao
    abstract fun updateDataDao(): UpdateDataDao

    @RenameColumn(NewsItem, "english_title", "title")
    @RenameColumn(NewsItem, "english_subtitle", "subtitle")
    @RenameColumn(NewsItem, "english_text", "text")
    @DeleteColumn(NewsItem, "dutch_title")
    @DeleteColumn(NewsItem, "dutch_subtitle")
    @DeleteColumn(NewsItem, "dutch_text")
    class NewsItemRemoveLanguageSpecificity : AutoMigrationSpec

    companion object {
        private const val NewsItem = "news_item"
    }
}
