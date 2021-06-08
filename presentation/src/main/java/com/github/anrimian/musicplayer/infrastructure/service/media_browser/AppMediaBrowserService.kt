package com.github.anrimian.musicplayer.infrastructure.service.media_browser

import android.os.Bundle
import android.support.v4.media.MediaBrowserCompat
import android.support.v4.media.MediaDescriptionCompat
import androidx.media.MediaBrowserServiceCompat
import com.github.anrimian.musicplayer.R
import com.github.anrimian.musicplayer.di.Components

const val SHUFFLE_ALL_AND_PLAY_ACTION_ID = "shuffle_all_and_play_action_id"

private const val ROOT_ID = "root_id"

private const val COMPOSITIONS_NODE_ID = "compositions_node_id"
private const val FOLDERS_NODE_ID = "folders_node_id"
private const val ARTISTS_NODE_ID = "artists_node_id"
private const val ALBUMS_NODE_ID = "albums_node_id"

//app starts play after android auto start
class AppMediaBrowserService: MediaBrowserServiceCompat() {

    override fun onCreate() {
        super.onCreate()
        val mediaSession = Components.getAppComponent()
            .mediaSessionHandler()
            .getMediaSession()
        this.sessionToken = mediaSession.sessionToken
    }

    override fun onGetRoot(
        clientPackageName: String,
        clientUid: Int,
        rootHints: Bundle?
    ): BrowserRoot {
        return BrowserRoot(ROOT_ID, null)
    }

    override fun onLoadChildren(
        parentId: String,
        result: Result<List<MediaBrowserCompat.MediaItem>>
    ) {
        if (parentId == ROOT_ID) {
            val mediaItems = listOf(
                actionItem(R.string.shuffle_all_and_play, SHUFFLE_ALL_AND_PLAY_ACTION_ID),
                browsableItem(R.string.compositions, COMPOSITIONS_NODE_ID),
                browsableItem(R.string.folders, FOLDERS_NODE_ID),
                browsableItem(R.string.artists, ARTISTS_NODE_ID),
                browsableItem(R.string.albums, ALBUMS_NODE_ID),
            )
            result.sendResult(mediaItems)
            return
        }
        result.sendResult(emptyList())
    }

    private fun actionItem(titleResId: Int, mediaId: String) =
        actionItem(getString(titleResId), mediaId)

    private fun actionItem(title: CharSequence?, mediaId: String) = MediaBrowserCompat.MediaItem(
        MediaDescriptionCompat.Builder()
            .setTitle(title)
            .setMediaId(mediaId)
            .build(),
        MediaBrowserCompat.MediaItem.FLAG_PLAYABLE
    )

    private fun browsableItem(titleResId: Int, mediaId: String) =
        browsableItem(getString(titleResId), mediaId)

    private fun browsableItem(title: CharSequence?, mediaId: String) = MediaBrowserCompat.MediaItem(
        MediaDescriptionCompat.Builder()
            .setTitle(title)
            .setMediaId(mediaId)
            .build(),
        MediaBrowserCompat.MediaItem.FLAG_BROWSABLE
    )

}