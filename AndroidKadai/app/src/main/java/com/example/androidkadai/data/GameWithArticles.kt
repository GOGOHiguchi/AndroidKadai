package com.example.androidkadai.data

import androidx.room.Embedded
import androidx.room.Junction
import androidx.room.Relation

// 1つのゲームに紐づく2つの記事を取得するためのクラス
data class GameWithArticles(
    @Embedded val game: GameSession,
    @Relation(
        parentColumn = "gameId",
        entityColumn = "articleId",
        associateBy = Junction(GameArticleCrossRef::class)
    )
    val articles: List<WikiArticle>
)