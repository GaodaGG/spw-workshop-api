@file:OptIn(UnstableSpwWorkshopApi::class)

package com.gg.example

import com.xuncorp.spw.workshop.api.PlaybackExtensionPoint
import com.xuncorp.spw.workshop.api.UnstableSpwWorkshopApi
import com.xuncorp.spw.workshop.api.WorkshopApi
import com.xuncorp.spw.workshop.api.pipeline.lyrics.LyricsProvider

/**
 * 歌词提供者示例
 *
 * 演示如何通过 [WorkshopApi.lyricsPipeline] 注册自定义歌词提供者。
 * 当 SPW 需要加载歌词时，会按优先级调用所有已注册的 [LyricsProvider]，
 * 第一个返回非 null 的结果即为最终歌词。
 */
class LyricsProviderExample : LyricsProvider {
    fun register() {
        WorkshopApi.lyricsPipeline.register(provider = this)
    }

    fun unregister() {
        WorkshopApi.lyricsPipeline.unregister(provider = this)
    }

    /**
     * 查找歌词
     *
     * @param mediaItem 当前播放的媒体项
     * @return 歌词文本（LRC 格式），如果找不到则返回 null
     */
    override suspend fun findLyrics(mediaItem: PlaybackExtensionPoint.MediaItem): String? {
        // 示例：为标题包含“示例”的歌曲提供内置歌词
        return if (mediaItem.title.contains("示例", ignoreCase = true)) {
            """
            [00:00.00]这是一首示例歌词
            [00:05.00]由示例插件提供歌词
            [00:10.00]展示歌词加载功能
            [00:15.00]感谢使用 SPW 创意工坊
            """.trimIndent()
        } else {
            // 返回 null 表示本提供者无法提供歌词，SPW 会继续调用下一个提供者
            null
        }
    }
}
