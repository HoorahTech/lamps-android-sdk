# Android-instantiated provider.
-keep class com.lamps.sdk.noah.NoahProvider { *; }
-keep class com.lamps.sdk.data.sdk.provider.ISdkProvider$* { *; }
-keep class com.lamps.sdk.data.sdk.channel.RewardAdSdkLoadCallback$* { *; }
-keep class com.lamps.sdk.reward.RewardAdShowCallback$* { *; }

# ABI shared with core and the sdk runtime.
-keep interface com.lamps.sdk.data.sdk.provider.ISdkProvider { public *; }
-keep class com.lamps.sdk.data.sdk.channel.RewardAdSdkLoadCallback { public *; }
-keep class com.lamps.sdk.data.sdk.channel.RewardVideoAd { public *; }
-keep enum com.lamps.sdk.data.sdk.channel.SdkChannel { public *; }
-keep interface com.lamps.sdk.data.sdk.channel.SdkInitCallback { public *; }
-keep class com.lamps.sdk.reward.LampsRewardAd { public *; }
-keep interface com.lamps.sdk.reward.RewardAdShowCallback { public *; }
-keep class com.lamps.sdk.data.init.ChannelInfoResponse { public *; }
-keep class com.lamps.sdk.data.init.RewardSlotResponse { public *; }
-keep enum com.lamps.sdk.core.LampsErrorCode { public *; }
-keep class com.lamps.sdk.data.sdk.reward.RewardAdErrorCode { public *; }
