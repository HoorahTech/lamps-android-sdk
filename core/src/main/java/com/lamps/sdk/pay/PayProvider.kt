package com.lamps.sdk.pay

import android.content.ContentProvider
import android.content.ContentValues
import android.database.Cursor
import android.net.Uri
import com.lamps.sdk.utils.SdkLog

/**
 * Optional pay modules register themselves at process start.
 * The host only needs the Gradle dependency.
 */
abstract class PayProvider : ContentProvider() {
    protected abstract fun createChannel(): PayChannel

    override fun onCreate(): Boolean {
        val channel = createChannel()
        PayChannelRegistry.register(channel)
        SdkLog.i("pay channel registered: ${channel.channel} (${channel.javaClass.name})")
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
