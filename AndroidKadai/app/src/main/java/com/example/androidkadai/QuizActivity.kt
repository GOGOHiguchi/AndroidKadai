package com.example.androidkadai

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import com.example.androidkadai.data.AppDatabase
import com.example.androidkadai.data.WikiApiService
import com.example.androidkadai.data.WikiPage
import com.example.androidkadai.data.WikiRepository
import com.example.androidkadai.databinding.ActivityQuizBinding
import kotlinx.coroutines.launch

class QuizActivity : AppCompatActivity() {
    private lateinit var binding: ActivityQuizBinding
    private val repository = WikiRepository(WikiApiService.create())
    private var savedPages: List<WikiPage> = emptyList()

    private var currentCount = 1
    private var correctCount = 0
    private var currentQuizType = "WORDS" // デフォルト
    private var currentCombo = 0
    private var maxCombo = 0

    // ✨ 追加：現在のスコアを保持する変数
    private var currentScore = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityQuizBinding.inflate(layoutInflater)
        setContentView(binding.root)

        currentCount = intent.getIntExtra("CURRENT_COUNT", 1)
        correctCount = intent.getIntExtra("CORRECT_COUNT", 0)
        binding.tvProgress.text = "第 $currentCount / 10 問"
        currentCombo = intent.getIntExtra("CURRENT_COMBO", 0)
        maxCombo = intent.getIntExtra("MAX_COMBO", 0)

        // ✨ 追加：前画面からスコアを受け取る（初回は0）
        currentScore = intent.getIntExtra("CURRENT_SCORE", 0)

        // ユーザー設定からクイズタイプを取得してUIを切り替える
        val dao = AppDatabase.getDatabase(this).appDao()
        lifecycleScope.launch {
            val settings = dao.getUserSetting()
            currentQuizType = settings?.quizType ?: "WORDS"

            // クイズタイトルの動的変更
            binding.tvQuestionTitle.text = if (currentQuizType == "PV") {
                "閲覧数（PV数）が多いのはどっち？"
            } else {
                "文字数（情報量）が多いのはどっち？"
            }

            fetchQuizData()
        }

        binding.btnSelect1.setOnClickListener { goToResult(savedPages[0].pageid) }
        binding.btnSelect2.setOnClickListener { goToResult(savedPages[1].pageid) }
    }

    private fun fetchQuizData() {
        binding.progressBar.visibility = View.VISIBLE
        lifecycleScope.launch {
            val pages = repository.fetchQuizArticles()
            binding.progressBar.visibility = View.GONE

            if (pages != null && pages.size >= 2) {
                savedPages = pages

                binding.tvArticle1Title.text = pages[0].title
                val rawContent1 = pages[0].revisions?.firstOrNull()?.slots?.main?.content
                binding.tvArticle1Extract.text = formatExtract(rawContent1, pages[0].title)

                Glide.with(this@QuizActivity)
                    .load(pages[0].thumbnail?.source)
                    .diskCacheStrategy(com.bumptech.glide.load.engine.DiskCacheStrategy.ALL)
                    .dontAnimate()
                    .error(android.R.drawable.ic_menu_gallery)
                    .into(binding.ivArticle1)

                binding.tvArticle2Title.text = pages[1].title
                val rawContent2 = pages[1].revisions?.firstOrNull()?.slots?.main?.content
                binding.tvArticle2Extract.text = formatExtract(rawContent2, pages[1].title)

                Glide.with(this@QuizActivity)
                    .load(pages[1].thumbnail?.source)
                    .diskCacheStrategy(com.bumptech.glide.load.engine.DiskCacheStrategy.ALL)
                    .dontAnimate()
                    .error(android.R.drawable.ic_menu_gallery)
                    .into(binding.ivArticle2)
            } else {
                Toast.makeText(this@QuizActivity, "API通信に失敗しました", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun formatExtract(rawContent: String?, title: String): String {
        if (rawContent.isNullOrEmpty()) return "${title}に関するWikipediaの記事です。"

        var cleanText = rawContent
            .replace(Regex("\\{\\{.*?\\}\\}", RegexOption.DOT_MATCHES_ALL), "")
            .replace(Regex("\\{\\[(?s).*?\\}\\]"), "")
            .replace("[[", "").replace("]]", "")
            .replace(Regex("'''+"), "")
            .replace(Regex("<.*?>"), "")
            .replace(Regex("==.*?=="), "")
            .trim()

        if (cleanText.isEmpty()) {
            cleanText = rawContent
        }

        return if (cleanText.length > 50) {
            "${cleanText.take(50)}..."
        } else {
            cleanText
        }
    }

    private fun goToResult(selectedId: Long) {
        val intent = Intent(this, ResultActivity::class.java).apply {
            putExtra("CURRENT_COUNT", currentCount)
            putExtra("CORRECT_COUNT", correctCount)
            putExtra("QUIZ_TYPE", currentQuizType)
            putExtra("SELECTED_ID", selectedId)
            putExtra("CURRENT_COMBO", currentCombo)
            putExtra("MAX_COMBO", maxCombo)

            // ✨ 追加：リザルト画面に現在のスコアを渡す
            putExtra("CURRENT_SCORE", currentScore)

            // --- 記事1のデータ処理 ---
            putExtra("ID_1", savedPages[0].pageid)
            putExtra("TITLE_1", savedPages[0].title)

            val v1 = if (currentQuizType == "PV") {
                savedPages[0].pageviews?.values?.filterNotNull()?.sum() ?: 0
            } else {
                savedPages[0].length
            }
            putExtra("VALUE_1", v1)

            val rawText1 = savedPages[0].revisions?.firstOrNull()?.slots?.main?.content
            putExtra("EXTRACT_1", formatExtract(rawText1, savedPages[0].title))
            putExtra("URL_1", savedPages[0].fullurl ?: "")
            putExtra("THUMB_1", savedPages[0].thumbnail?.source ?: "")

            // --- 記事2のデータ処理 ---
            putExtra("ID_2", savedPages[1].pageid)
            putExtra("TITLE_2", savedPages[1].title)

            val v2 = if (currentQuizType == "PV") {
                savedPages[1].pageviews?.values?.filterNotNull()?.sum() ?: 0
            } else {
                savedPages[1].length
            }
            putExtra("VALUE_2", v2)

            val rawText2 = savedPages[1].revisions?.firstOrNull()?.slots?.main?.content
            putExtra("EXTRACT_2", formatExtract(rawText2, savedPages[1].title))
            putExtra("URL_2", savedPages[1].fullurl ?: "")
            putExtra("THUMB_2", savedPages[1].thumbnail?.source ?: "")
        }
        startActivity(intent)
        finish()
    }
}