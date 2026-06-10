package com.example.androidkadai

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import com.example.androidkadai.data.AppDatabase
import com.example.androidkadai.data.WikiArticle
import com.example.androidkadai.databinding.ActivityMyPageBinding
import kotlinx.coroutines.launch

class MyPageActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMyPageBinding
    private var currentDisplayArticles: List<WikiArticle> = emptyList()
    private var isFavoritesTab = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMyPageBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // ✨ 追加: メイン画面に戻るボタンの処理
        binding.btnBackToMain.setOnClickListener {
            finish() // 現在のマイページを閉じて、前の画面（MainActivity）に戻る
        }

        binding.btnTabHistory.setOnClickListener {
            isFavoritesTab = false
            loadData()
        }
        binding.btnTabFavorites.setOnClickListener {
            isFavoritesTab = true
            loadData()
        }

        loadData()

        binding.listViewHistory.setOnItemClickListener { _, _, position, _ ->
            val article = currentDisplayArticles[position]
            showArticleDetailDialog(article)
        }

        binding.btnEditMock.setOnClickListener {
            Toast.makeText(this, "リスト内の記事を長押しすると、お気に入り状態を編集(Edit)できます", Toast.LENGTH_LONG).show()
        }

        binding.listViewHistory.setOnItemLongClickListener { _, _, position, _ ->
            val article = currentDisplayArticles[position]
            toggleFavoriteStatus(article)
            true
        }
    }

    private fun loadData() {
        val dao = AppDatabase.getDatabase(this).appDao()
        lifecycleScope.launch {
            if (isFavoritesTab) {
                currentDisplayArticles = dao.getFavoriteArticles()
                // ✨ リスト表示の日本語化（例：⭐ 記事タイトル (1,250文字)）
                val list = currentDisplayArticles.map { "⭐ ${it.title} (${String.format("%,d", it.value)}${it.unitType})" }
                binding.listViewHistory.adapter = ArrayAdapter(this@MyPageActivity, android.R.layout.simple_list_item_1, list)
            } else {
                val games = dao.getAllGames()
                val articlesList = mutableListOf<WikiArticle>()
                games.forEach { game ->
                    val withArticles = dao.getGameWithArticles(game.gameId)
                    articlesList.addAll(withArticles.articles)
                }
                currentDisplayArticles = articlesList.distinctBy { it.articleId }
                // ✨ リスト表示の日本語化（例：📄 記事タイトル (500回閲覧)）
                val list = currentDisplayArticles.map { "📄 ${it.title} (${String.format("%,d", it.value)}${it.unitType})" }
                binding.listViewHistory.adapter = ArrayAdapter(this@MyPageActivity, android.R.layout.simple_list_item_1, list)
            }
        }
    }

    private fun toggleFavoriteStatus(article: WikiArticle) {
        val dao = AppDatabase.getDatabase(this).appDao()
        lifecycleScope.launch {
            val nextStatus = !article.isFavorite
            dao.updateFavoriteStatus(article.articleId, nextStatus)
            val msg = if (nextStatus) "お気に入りに登録しました" else "お気に入りから解除しました"
            Toast.makeText(this@MyPageActivity, msg, Toast.LENGTH_SHORT).show()
            loadData()
        }
    }

    // ✨ 大改造: ダイアログ内に画像表示エリアを動的追加
    private fun showArticleDetailDialog(article: WikiArticle) {
        val context = this
        val container = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(48, 24, 48, 24)
        }

        val imageView = ImageView(context).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                400
            ).apply {
                setMargins(0, 0, 0, 24)
            }
            // ✨ scaleType を CENTER_CROP から FIT_CENTER に変更！
            scaleType = ImageView.ScaleType.FIT_CENTER
            setBackgroundColor(android.graphics.Color.DKGRAY)
        }
        container.addView(imageView)

        val textView = TextView(context).apply {
            // ✨ ダイアログ内テキストの日本語化
            text = "【記録】: ${String.format("%,d", article.value)} ${article.unitType}\n\n【概要】:\n${article.extract.ifEmpty { "概要データがありません" }}"
            textSize = 16f
            setTextColor(android.graphics.Color.LTGRAY)
        }

        val scrollView = ScrollView(context).apply {
            addView(textView)
        }
        container.addView(scrollView)

        if (article.thumbnailUrl.isNotEmpty()) {
            Glide.with(this)
                .load(article.thumbnailUrl)
                .diskCacheStrategy(com.bumptech.glide.load.engine.DiskCacheStrategy.ALL)
                .into(imageView)
        }

        AlertDialog.Builder(this)
            .setTitle(article.title)
            .setView(container)
            // ✨ ボタン名も分かりやすく日本語化
            .setPositiveButton("Wikipediaを開く") { _, _ ->
                if (article.url.isNotEmpty()) {
                    startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(article.url)))
                } else {
                    Toast.makeText(this, "URLがありません", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("閉じる", null)
            .show()
    }
}