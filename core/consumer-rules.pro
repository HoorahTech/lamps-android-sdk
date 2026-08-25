# Runtime bridges consumed by the sdk and channel AARs.
-keep class com.lamps.sdk.core.SdkRuntime { public *; }
-keep class com.lamps.sdk.config.SdkConfig { public *; }
-keep class com.lamps.sdk.config.SdkConfig$Companion { *; }
-keep interface com.lamps.sdk.core.CoreInitCallback { public *; }
-keep interface com.lamps.sdk.core.CoreOaidProvider { public *; }
-keep class com.lamps.sdk.webview.LampsWebView { *; }
-keep class com.lamps.sdk.provider.LampsProvider { *; }
-keep class com.lamps.sdk.data.sdk.provider.ISdkProvider$* { *; }
-keep class com.lamps.sdk.data.sdk.channel.RewardAdSdkLoadCallback$* { *; }
-keep class com.lamps.sdk.reward.RewardAdShowCallback$* { *; }
-keep class com.lamps.sdk.debug.LampsSdkDebug { public *; }
-keep class com.lamps.sdk.debug.LampsSdkDebugItem { public *; }
-keep class com.lamps.sdk.debug.LampsSdkDebugSection { public *; }

# H5 JavaScript bridge methods are looked up by their declared names.
-keepclassmembers class * {
    @android.webkit.JavascriptInterface <methods>;
}
