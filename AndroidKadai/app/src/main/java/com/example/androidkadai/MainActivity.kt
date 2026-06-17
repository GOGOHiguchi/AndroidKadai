package com.example.androidkadai

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.androidkadai.data.AppDatabase
import com.example.androidkadai.data.UserSetting
import com.example.androidkadai.databinding.ActivityMainBinding
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val dao = AppDatabase.getDatabase(this).appDao()

        // 既存設定の読み込み
        lifecycleScope.launch {
            val settings = dao.getUserSetting() ?: UserSetting()
            binding.etUserName.setText(settings.userName)

            if (settings.quizType == "PV") {
                binding.rbPv.isChecked = true
            } else {
                binding.rbWords.isChecked = true
            }
        }

        // 設定保存
        binding.btnSaveSettings.setOnClickListener {
            val name = binding.etUserName.text.toString().trim()
            val type = if (binding.rbPv.isChecked) "PV" else "WORDS"

            lifecycleScope.launch {
                // バリデーション
                val errorMessage = validateNewUserName(name, dao)

                if (errorMessage != null) {
                    Toast.makeText(
                        this@MainActivity,
                        errorMessage,
                        Toast.LENGTH_SHORT
                    ).show()
                    return@launch
                }

                dao.insertOrUpdateUser(
                    UserSetting(
                        userId = 1,
                        userName = name,
                        quizType = type
                    )
                )

                Toast.makeText(
                    this@MainActivity,
                    "設定を更新しました",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }

        binding.btnStartGame.setOnClickListener {
            startActivity(Intent(this, QuizActivity::class.java))
        }

        binding.btnMyPage.setOnClickListener {
            startActivity(Intent(this, MyPageActivity::class.java))
        }
    }

    // ✨ ここを追加：画面が表示される（戻ってくる）たびにランキングを最新化
    override fun onResume() {
        super.onResume()

        val dao = AppDatabase.getDatabase(this).appDao()

        lifecycleScope.launch {
            val topScores = dao.getRanking()

            // 1位の表示
            if (topScores.isNotEmpty()) {
                binding.tvHighScore1.text = "🥇 ${topScores[0].userName}  -  ${topScores[0].highScore} pts"
            } else {
                binding.tvHighScore1.text = "🥇 ---  -  0 pts"
            }

            // 2位の表示
            if (topScores.size > 1) {
                binding.tvHighScore2.text = "🥈 ${topScores[1].userName}  -  ${topScores[1].highScore} pts"
            } else {
                binding.tvHighScore2.text = "🥈 ---  -  0 pts"
            }

            // 3位の表示
            if (topScores.size > 2) {
                binding.tvHighScore3.text = "🥉 ${topScores[2].userName}  -  ${topScores[2].highScore} pts"
            } else {
                binding.tvHighScore3.text = "🥉 ---  -  0 pts"
            }
        }
    }
}

/**
 * ユーザー名が適切かどうかをチェックする関数
 * @return エラーメッセージ（問題がなければ null）
 */
suspend fun validateNewUserName(
    inputName: String,
    dao: com.example.androidkadai.data.AppDao
): String? {
    val cleanName = inputName.trim()

    // 1. 空白チェック
    if (cleanName.isEmpty()) {
        return "名前を入力してください。"
    }

    // 2. NGワードチェック
    val ngWords = listOf("あほ","アホ","カス", "NGワード")

    for (word in ngWords) {
        if (cleanName.contains(word, ignoreCase = true)) {
            return "不適切な単語が含まれているため、この名前は使用できません。"
        }
    }

    // 3. 重複チェック
    val isExist = dao.getPlayerScore(cleanName) != null

    if (isExist) {
        return "「${cleanName}」は既にランキングに存在します。別の名前にしてください。"
    }

    return null
}