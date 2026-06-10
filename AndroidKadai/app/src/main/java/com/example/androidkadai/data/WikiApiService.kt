package com.example.androidkadai.data

import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Query

data class WikiResponse(val query: WikiQuery?)
data class WikiQuery(val pages: List<WikiPage>?)

data class WikiPage(
    val pageid: Long,
    val title: String,
    val length: Int, // 文字数
    val fullurl: String?, // URL
    val thumbnail: WikiThumbnail?,
    val pageviews: Map<String, Int>?,
    // ✨ 変更: prop=extracts をやめたため、extractの代わりに revisions から本文を取得します
    val revisions: List<WikiRevision>?
)

// ✨ 本文の生テキストをパースするための新しい階層構造
data class WikiRevision(
    val slots: WikiSlots?
)
data class WikiSlots(
    val main: WikiMainSlot?
)
data class WikiMainSlot(
    val content: String? // ここに記事の生本文（マークアップテキスト）が入ります
)

data class WikiThumbnail(val source: String)

interface WikiApiService {
    // ✨ 大改造: propを extracts から revisions に変更し、rvprop=content で本文を引っ張るようにしました
    @GET("w/api.php?action=query&format=json&formatversion=2&generator=random&grnnamespace=0&prop=revisions|info|pageimages|pageviews&rvprop=content&rvslots=main&inprop=url&pithumbsize=500")
    suspend fun getRandomArticles(
        @Query("grnlimit") limit: Int = 2
    ): WikiResponse

    companion object {
        private const val BASE_URL = "https://ja.wikipedia.org/"

        fun create(): WikiApiService {
            val client = OkHttpClient.Builder()
                .addInterceptor { chain ->
                    val request = chain.request().newBuilder()
                        .header("User-Agent", "WikiGameApp/1.1 (Contact: c241039@ivy.ac.jp) AndroidPlayer")
                        .build()
                    chain.proceed(request)
                }
                .build()

            return Retrofit.Builder()
                .baseUrl(BASE_URL)
                .client(client)
                .addConverterFactory(GsonConverterFactory.create())
                .build()
                .create(WikiApiService::class.java)
        }
    }
}