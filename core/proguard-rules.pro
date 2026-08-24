# Public API.
-keep public class com.lamps.sdk.LampsSdk { public *; }
-keep public class com.lamps.sdk.config.LampsConfig { public *; }
-keep public class com.lamps.sdk.config.LampsConfig$Builder { public *; }
-keep class com.lamps.sdk.config.LampsConfig$Companion { *; }
-keep public interface com.lamps.sdk.core.InitCallback { public *; }
-keep public interface com.lamps.sdk.core.OaidProvider { public *; }
-keep public class com.lamps.sdk.view.GameCenterView { public *; }

# Android-instantiated provider.
-keep class com.lamps.sdk.provider.LampsProvider { *; }

# Internal ABI shared with optional channel AARs.
-keep class com.lamps.sdk.provider.** { *; }
-keep class com.lamps.sdk.data.sdk.provider.** { *; }
-keep class com.lamps.sdk.data.sdk.channel.** { *; }
-keep class com.lamps.sdk.reward.** { *; }
-keep class com.lamps.sdk.data.init.ChannelInfoResponse { *; }
-keep class com.lamps.sdk.data.init.RewardSlotResponse { *; }
-keep class com.lamps.sdk.core.LampsErrorCode { *; }
-keep class com.lamps.sdk.utils.SdkLog { *; }
-keep class com.lamps.sdk.data.sdk.reward.RewardAdErrorCode { *; }

# Methods invoked by the H5 bridge.
-keepclassmembers class * {
    @android.webkit.JavascriptInterface <methods>;
}
