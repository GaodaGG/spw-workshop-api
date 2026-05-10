@file:Suppress("unused")

package com.xuncorp.spw.workshop.api.pipeline.lyrics

import com.xuncorp.spw.workshop.api.PlaybackExtensionPoint

/**
 * 歌词提供者接口
 */
interface LyricsProvider {
    /**
     * 查找歌词
     *
     * @param mediaItem 媒体项
     * @return 歌词文本，如果找不到则返回 null
     */
    suspend fun findLyrics(mediaItem: PlaybackExtensionPoint.MediaItem): String?
}
