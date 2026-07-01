package com.example.androidkadai.data

import androidx.room.*

@Dao
interface AppDao {
    // --- USERS TABLE (CRUD: U, R) ---
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateUser(user: UserSetting)

    @Query("SELECT * FROM users WHERE userId = 1")
    suspend fun getUserSetting(): UserSetting?

    // --- GAMES TABLE (CRUD: C, R, D) ---
    @Insert
    suspend fun insertGame(game: GameSession): Long

    @Delete
    suspend fun deleteGame(game: GameSession)

    @Query("SELECT * FROM games ORDER BY playedAt DESC")
    suspend fun getAllGames(): List<GameSession>

    // --- ARTICLES TABLE (CRUD: C, R, U, D) ---
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertArticles(articles: List<WikiArticle>)

    @Query("UPDATE articles SET isFavorite = :isFav WHERE articleId = :id")
    suspend fun updateFavoriteStatus(id: Long, isFav: Boolean) // カンプの「Edit」用

    @Query("SELECT * FROM articles WHERE isFavorite = 1")
    suspend fun getFavoriteArticles(): List<WikiArticle>

    // --- CROSS REFERENCE ---
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertGameArticleCrossRef(crossRef: GameArticleCrossRef)

    @Transaction
    @Query("SELECT * FROM games WHERE gameId = :gameId")
    suspend fun getGameWithArticles(gameId: Long): GameWithArticles

    @Query("SELECT * FROM player_scores ORDER BY highScore DESC, maxCombo DESC")
    suspend fun getRanking(): List<PlayerScore>

    @Query("SELECT * FROM player_scores WHERE userName = :name LIMIT 1")
    suspend fun getPlayerScore(name: String): PlayerScore?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateScore(score: PlayerScore)
}