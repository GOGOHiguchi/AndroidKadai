package com.example.androidkadai.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

// ✨ entities に UserSetting::class を追加し、version を 2 に上げます
@Database(
    entities = [UserSetting::class, GameSession::class, WikiArticle::class, GameArticleCrossRef::class,PlayerScore::class],
    version = 2,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun appDao(): AppDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "wiki_game_database"
                )
                    // ✨ 開発中にDBの構造（カラムやテーブル）を変えた際、
                    // アプリがクラッシュするのを防ぎ、自動で古いDBをリセットして新構造にする設定
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}