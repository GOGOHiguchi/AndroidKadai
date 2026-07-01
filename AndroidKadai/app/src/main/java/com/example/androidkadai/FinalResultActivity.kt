package com.example.androidkadai

import android.content.Intent
import android.os.Bundle
import android.widget.ArrayAdapter
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.androidkadai.data.AppDatabase
import com.example.androidkadai.data.PlayerScore
import com.example.androidkadai.databinding.ActivityFinalResultBinding
import kotlinx.coroutines.launch

class FinalResultActivity : AppCompatActivity() {
    private lateinit var binding: ActivityFinalResultBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityFinalResultBinding.inflate(layoutInflater)
        setContentView(binding.root)
        val totalScore = intent.getIntExtra("TOTAL_SCORE", 0)
        val maxCombo = intent.getIntExtra("MAX_COMBO", 0)
        binding.tvYourScore.text = "今回のスコア: ${totalScore}pt\n正解数: ${totalScore} / 10 問\n最大連続正解: ${maxCombo} コンボ"

        val dao = AppDatabase.getDatabase(this).appDao()

        lifecycleScope.launch {
            // 1. 現在設定されているユーザー名を取得
            val settings = dao.getUserSetting()
            val currentUserName = settings?.userName ?: "プレイヤー1"

            // 2. そのユーザーの過去のハイスコアデータを取得
            val existingScore = dao.getPlayerScore(currentUserName)

            if (existingScore == null) {
                // 過去データがない（初プレイ）なら新規保存
                dao.insertOrUpdateScore(
                    PlayerScore(userName = currentUserName, highScore = totalScore, maxCombo = maxCombo)
                )
            } else {
                // ✨ 判定：今回のスコアが、過去のハイスコアを上回っていた場合のみ上書き更新！
                if (totalScore > existingScore.highScore ||
                    (totalScore == existingScore.highScore && maxCombo > existingScore.maxCombo)) {

                    dao.insertOrUpdateScore(
                        existingScore.copy(highScore = totalScore, maxCombo = maxCombo)
                    )
                }
            }

            // 3. 最新のランキングをスコア順で読み込んでListViewにセット
            val rankingList = dao.getRanking()
            val displayList = rankingList.mapIndexed { index, player ->
                "${index + 1}位: ${player.userName}  —  ${player.highScore}点 (${player.maxCombo}コンボ)"
            }

            binding.listViewRanking.adapter = ArrayAdapter(
                this@FinalResultActivity,
                android.R.layout.simple_list_item_1,
                displayList
            )
        }

        binding.btnBackToTitle.setOnClickListener {
            val intent = Intent(this, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_CLEAR_TOP // クイズ画面の履歴をクリアして戻る
            }
            startActivity(intent)
            finish()
        }
    }
}