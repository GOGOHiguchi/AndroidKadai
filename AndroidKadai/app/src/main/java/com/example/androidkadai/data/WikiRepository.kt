package com.example.androidkadai.data

import android.util.Log
import kotlinx.coroutines.delay
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class WikiRepository(private val apiService: WikiApiService) {

    // ③ 同時通信禁止用の排他制御（UnityのisApiBusyに相当）
    private val mutex = Mutex()

    // ② 通信間隔制御用の変数
    private val API_INTERVAL = 2000L // 2秒
    private var lastApiCallTime = 0L

    // ⑤ 最大リトライ回数
    private val MAX_RETRIES = 2

    suspend fun fetchQuizArticles(): List<WikiPage>? {
        // 同時通信をブロック
        mutex.withLock {
            // 2秒のインターバル制御
            val passedTime = System.currentTimeMillis() - lastApiCallTime
            if (passedTime < API_INTERVAL) {
                delay(API_INTERVAL - passedTime)
            }

            var currentTry = 0
            while (currentTry <= MAX_RETRIES) {
                try {
                    val response = apiService.getRandomArticles(limit = 2)
                    lastApiCallTime = System.currentTimeMillis()

                    val pages = response.query?.pages
                    if (!pages.isNullOrEmpty() && pages.size >= 2) {
                        return pages
                    }
                } catch (e: Exception) {
                    currentTry++
                    if (currentTry <= MAX_RETRIES) {
                        // ⑤ Exponential Backoff (2秒, 4秒...)
                        val backoffTime = Math.pow(2.0, currentTry.toDouble()).toLong() * 1000
                        Log.w("WikiAPI", "通信エラー。${backoffTime / 1000}秒後にリトライします (${currentTry}/${MAX_RETRIES})")
                        delay(backoffTime)
                        continue
                    }
                    Log.e("WikiAPI", "リトライ上限を超過しました: ${e.message}")
                }
            }
            return null
        }
    }
}