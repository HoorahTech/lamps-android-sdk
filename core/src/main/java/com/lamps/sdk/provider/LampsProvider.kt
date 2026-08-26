package com.lamps.sdk.provider

import android.content.ContentProvider
import android.content.ContentValues
import android.database.Cursor
import android.net.Uri
import com.lamps.sdk.data.sdk.provider.ISdkProvider
import com.lamps.sdk.data.sdk.provider.SdkProviderRegistry
import com.lamps.sdk.utils.SdkLog

/**
 * 广告网络 module 用 ContentProvider 在进程启动时向 core 注册自身。
 * 宿主只需 Gradle 依赖对应 module，不必手动 register。
 */
abstract class LampsProvider : ContentProvider() {
    protected abstract fun createProvider(): ISdkProvider

    override fun onCreate(): Boolean {
        val provider = createProvider()
        SdkProviderRegistry.register(provider)
        SdkLog.i("provider registered: ${provider.name} (${provider.javaClass.name})")
        return false
    }

    override fun query(
        uri: Uri,
        projection: Array<out String>?,
        selection: String?,
        selectionArgs: Array<out String>?,
        sortOrder: String?
    ): Cursor? = null

    override fun getType(uri: Uri): String? = null

    override fun insert(uri: Uri, values: ContentValues?): Uri? = null

    override fun delete(uri: Uri, selection: String?, selectionArgs: Array<out String>?): Int = 0

    override fun update(
        uri: Uri,
        values: ContentValues?,
        selection: String?,
        selectionArgs: Array<out String>?
    ): Int = 0
}
