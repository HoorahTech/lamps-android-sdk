package com.hupu.games

import android.app.Activity
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import com.lamps.sdk.LampsSdk
import com.lamps.sdk.reward.LampsRewardAd
import com.lamps.sdk.reward.RewardAdLoadCallback
import com.lamps.sdk.reward.RewardAdShowCallback
import com.lamps.sdk.webview.LampsWebViewActivity

class MainActivity : Activity() {
    private lateinit var showRewardButton: Button
    private lateinit var rewardStatusView: TextView
    private var rewardFlowInProgress = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        showRewardButton = findViewById(R.id.showRewardVideoButton)
        rewardStatusView = findViewById(R.id.rewardStatusView)
        findViewById<Button>(R.id.openWebViewButton).setOnClickListener {
            startActivity(
                LampsWebViewActivity.buildIntent(
                    context = this,
                    url = DEMO_URL
                )
            )
        }
        SdkToolsBinder.bind(this, findViewById(R.id.openSdkToolsButton))
        showRewardButton.setOnClickListener { loadAndShowReward() }
    }

    private fun loadAndShowReward() {
        if (rewardFlowInProgress) {
            Toast.makeText(this, "Reward ad is loading or showing", Toast.LENGTH_SHORT).show()
            return
        }
        if (!LampsSdk.isSdkReady()) {
            updateRewardStatus("SDK is not ready")
            Toast.makeText(this, "SDK is not ready", Toast.LENGTH_SHORT).show()
            return
        }
        rewardFlowInProgress = true
        showRewardButton.isEnabled = false
        updateRewardStatus("Loading...")
        LampsSdk.loadReward(this, object : RewardAdLoadCallback {
            override fun onAdLoadSuccess(ad: LampsRewardAd) {
                updateRewardStatus(
                    "Loaded ${ad.channelName} slot=${ad.slotId} price=${ad.price}, showing..."
                )
                ad.show(this@MainActivity, createShowCallback())
            }

            override fun onAdLoadFailed(code: Int, message: String?) {
                finishRewardFlow("Load failed code=$code message=${message.orEmpty()}")
            }
        })
    }

    private fun createShowCallback(): RewardAdShowCallback {
        return object : RewardAdShowCallback {
            private var rewarded = false

            override fun onAdShown() {
                updateRewardStatus("Shown")
            }

            override fun onAdRewarded() {
                rewarded = true
                updateRewardStatus("Rewarded")
            }

            override fun onAdClosed() {
                finishRewardFlow(if (rewarded) "Closed, rewarded" else "Closed, not rewarded")
            }

            override fun onAdShowFailed(code: Int, message: String?) {
                finishRewardFlow("Show failed code=$code message=${message.orEmpty()}")
            }
        }
    }

    private fun finishRewardFlow(status: String) {
        rewardFlowInProgress = false
        showRewardButton.isEnabled = true
        updateRewardStatus(status)
        Toast.makeText(this, status, Toast.LENGTH_SHORT).show()
    }

    private fun updateRewardStatus(status: String) {
        rewardStatusView.text = status
    }

    private companion object {
        const val DEMO_URL =
            "https://www.hoorahgo.com/mg/index.html#/play?demo=nba-career-sim"
    }
}
