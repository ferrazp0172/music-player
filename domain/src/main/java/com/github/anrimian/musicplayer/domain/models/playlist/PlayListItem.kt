package com.github.anrimian.musicplayer.domain.models.playlist

import com.github.anrimian.musicplayer.domain.models.composition.Composition
import com.github.anrimian.musicplayer.domain.models.composition.CorruptionType
import com.github.anrimian.musicplayer.domain.models.composition.InitialSource
import java.util.Date

class PlayListItem(
    val itemId: Long,
    id: Long,
    title: String,
    artist: String?,
    album: String?,
    duration: Long,
    size: Long,
    comment: String?,
    storageId: Long?,
    dateAdded: Date,
    dateModified: Date,
    coverModifyTime: Date,
    corruptionType: CorruptionType?,
    isFileExists: Boolean,
    initialSource: InitialSource,
): Composition(
    id,
    title,
    artist,
    album,
    duration,
    size,
    comment,
    storageId,
    dateAdded,
    dateModified,
    coverModifyTime,
    corruptionType,
    isFileExists,
    initialSource
) {

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as PlayListItem

        return itemId == other.itemId
    }

    override fun hashCode(): Int {
        return itemId.hashCode()
    }

    override fun toString(): String {
        return "PlayListItem(itemId=$itemId, composition=${super.toString()})"
    }

}