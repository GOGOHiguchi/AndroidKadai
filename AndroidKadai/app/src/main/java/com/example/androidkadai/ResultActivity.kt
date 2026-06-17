package com.example.androidkadai

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.androidkadai.data.AppDatabase
import com.example.androidkadai.data.GameArticleCrossRef
import com.example.androidkadai.data.GameSession
import com.example.androidkadai.data.WikiArticle
import com.example.androidkadai.databinding.ActivityResultBinding
import kotlinx.coroutines.launch

class ResultActivity : AppCompatActivity() {
    private lateinit var binding: ActivityResultBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityResultBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val currentCount = intent.getIntExtra("CURRENT_COUNT", 1)
        var correctCount = intent.getIntExtra("CORRECT_COUNT", 0)
        val quizType = intent.getStringExtra("QUIZ_TYPE") ?: "WORDS"
        val selectedId = intent.getLongExtra("SELECTED_ID", 0)

        val currentCombo = intent.getIntExtra("CURRENT_COMBO", 0)
        val maxCombo = intent.getIntExtra("MAX_COMBO", 0)

        // ✨ 追加：前画面から現在のスコアを受け取る
        val currentScore = intent.getIntExtra("CURRENT_SCORE", 0)

        val id1 = intent.getLongExtra("ID_1", 0)
        val title1 = intent.getStringExtra("TITLE_1") ?: ""
        val val1 = intent.getIntExtra("VALUE_1", 0)
        val ext1 = intent.getStringExtra("EXTRACT_1") ?: ""
        val url1 = intent.getStringExtra("URL_1") ?: ""
        val thumb1 = intent.getStringExtra("THUMB_1") ?: ""

        val id2 = intent.getLongExtra("ID_2", 0)
        val title2 = intent.getStringExtra("TITLE_2") ?: ""
        val val2 = intent.getIntExtra("VALUE_2", 0)
        val ext2 = intent.getStringExtra("EXTRACT_2") ?: ""
        val url2 = intent.getStringExtra("URL_2") ?: ""
        val thumb2 = intent.getStringExtra("THUMB_2") ?: ""

        val correctId = if (val1 > val2) id1 else id2
        val isCorrect = (selectedId == correctId)

        var newCombo = currentCombo
        var newMaxCombo = maxCombo

        // ✨ 追加：計算用のスコア変数
        var newScore = currentScore

        // ✨ 変更：正解時のスコア計算を実装
        if (isCorrect) {
            correctCount++
            newCombo++
            if (newCombo > newMaxCombo) newMaxCombo = newCombo // 最高コンボを更新

            // ご希望の計算式：基本点(1) + (コンボ数 - 1)
            val earnedPoint = 1 + (newCombo - 1)
            newScore += earnedPoint

            binding.tvResult.text = "正解！ (${newCombo} 連続! / +${earnedPoint}pt)"
        } else {
            newCombo = 0 // コンボリセット
            binding.tvResult.text = "不正解..."
        }

        val unitLabel = if (quizType == "PV") "回閲覧" else "文字"
        binding.tvDetails.text = "$title1: %,d $unitLabel\n\n$title2: %,d $unitLabel".format(val1, val2)

        if (currentCount >= 10) binding.btnNextQuiz.text = "総合結果を見る"

        val dao = AppDatabase.getDatabase(this).appDao()

        // 履歴自動保存
        lifecycleScope.launch {
            val gameId = dao.insertGame(GameSession(isCorrect = isCorrect))
            dao.insertArticles(listOf(
                WikiArticle(id1, title1, val1, isFavorite = false, ext1, url1, thumb1, unitLabel),
                WikiArticle(id2, title2, val2, isFavorite = false, ext2, url2, thumb2, unitLabel)
            ))
            dao.insertGameArticleCrossRef(GameArticleCrossRef(gameId, id1))
            dao.insertGameArticleCrossRef(GameArticleCrossRef(gameId, id2))
        }

        // Favoriteボタン
        binding.btnSave.setOnClickListener {
            lifecycleScope.launch {
                dao.insertArticles(listOf(
                    WikiArticle(id1, title1, val1, isFavorite = true, ext1, url1, thumb1, unitLabel),
                    WikiArticle(id2, title2, val2, isFavorite = true, ext2, url2, thumb2, unitLabel)
                ))
                Toast.makeText(this@ResultActivity, "❤ Favoriteに登録しました", Toast.LENGTH_SHORT).show()
            }
        }

        binding.btnNextQuiz.setOnClickListener {
            if (currentCount >= 10) {
                // 最終結果画面へ
                val intent = Intent(this, FinalResultActivity::class.java).apply {
                    putExtra("TOTAL_CORRECT", correctCount)
                    putExtra("MAX_COMBO", newMaxCombo)
                    // ✨ 追加：最終結果画面へ合計スコアを渡す
                    putExtra("TOTAL_SCORE", newScore)
                }
                startActivity(intent)
            } else {
                // 次の問題へ
                val intent = Intent(this, QuizActivity::class.java).apply {
                    putExtra("CURRENT_COUNT", currentCount + 1)
                    putExtra("CORRECT_COUNT", correctCount)
                    putExtra("CURRENT_COMBO", newCombo)
                    putExtra("MAX_COMBO", newMaxCombo)
                    putExtra("QUIZ_TYPE", quizType)
                    // ✨ 追加：次のクイズ画面へ最新のスコアを引き継ぐ
                    putExtra("CURRENT_SCORE", newScore)
                }
                startActivity(intent)
            }
            finish()
        }
    }
}