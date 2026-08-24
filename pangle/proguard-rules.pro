# The provider is instantiated from the merged AndroidManifest.
-keep class com.lamps.sdk.pangle.PangleProvider { *; }

# Internal ABI required while minifying the channel AAR.
-keep class com.lamps.sdk.provider.** { *; }
-keep class com.lamps.sdk.data.sdk.provider.** { *; }
-keep class com.lamps.sdk.data.sdk.channel.** { *; }
-keep class com.lamps.sdk.reward.** { *; }
-keep class com.lamps.sdk.data.init.ChannelInfoResponse { *; }
-keep class com.lamps.sdk.data.init.RewardSlotResponse { *; }
-keep class com.lamps.sdk.core.LampsErrorCode { *; }
-keep class com.lamps.sdk.utils.SdkLog { *; }
-keep class com.lamps.sdk.data.sdk.reward.RewardAdErrorCode { *; }
