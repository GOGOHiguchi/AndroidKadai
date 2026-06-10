package com.example.androidkadai.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "users")
data class UserSetting(
    @PrimaryKey val userId: Int = 1,
    val userName: String = "プレイヤー1",
    val quizType: String = "WORDS"
)

@Entity(tableName = "games")
data class GameSession(
    @PrimaryKey(autoGenerate = true) val gameId: Long = 0,
    val playedAt: Long = System.currentTimeMillis(),
    val isCorrect: Boolean
)

@Entity(tableName = "articles")
data class WikiArticle(
    @PrimaryKey val articleId: Long,
    val title: String,
    val value: Int,
    val isFavorite: Boolean = false,
    val extract: String = "",
    val url: String = "",
    val thumbnailUrl: String = "",
    val unitType: String = "文字" // ✨ 追加：「文字」または「回閲覧」を保存する
)

@Entity(tableName = "game_article_cross_ref", primaryKeys = ["gameId", "articleId"])
data class GameArticleCrossRef(
    val gameId: Long,
    val articleId: Long
)

// ✨ 新設：ハイスコアランキング用テーブル（名前の重複を防ぐユニーク制約の布石付き）
@androidx.room.Entity(
    tableName = "player_scores",
    indices = [androidx.room.Index(value = ["userName"], unique = true)]
)
data class PlayerScore(
    @androidx.room.PrimaryKey(autoGenerate = true) val id: Long = 0,
    val userName: String,
    val highScore: Int, // 10問中のトータル正解数
    val maxCombo: Int   // 最大連続正解数
)