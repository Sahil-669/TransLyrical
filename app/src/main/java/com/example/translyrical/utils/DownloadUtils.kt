package com.example.translyrical.utils

import android.app.DownloadManager
import android.content.Context
import android.net.Uri
import android.os.Environment
import android.widget.Toast

fun downloadSongToDevice(
    context: Context,
    audioUri: Uri?,
    title: String,
    artist: String,
    headers: Map<String, String>?
) {
    if (audioUri == null || audioUri.scheme == "content") {
        Toast.makeText(context, "File is already on your device.", Toast.LENGTH_SHORT).show()
        return
    }

    try {
        val request = DownloadManager.Request(audioUri).apply {
            setTitle("$title - $artist")
            setDescription("Downloading via TransLyrical")

            setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
            setDestinationInExternalPublicDir(
                Environment.DIRECTORY_MUSIC,
                "$title - $artist.m4a"
            )
            headers?.forEach { (key, value) ->
                addRequestHeader(key, value)
            }
        }
        val manager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
        manager.enqueue(request)
        Toast.makeText(context, "Download started...", Toast.LENGTH_SHORT).show()
    } catch (e: Exception) {
        Toast.makeText(context, "Download failed: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
    }
}