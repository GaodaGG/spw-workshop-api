@file:Suppress("unused")

package com.xuncorp.spw.workshop.api.event

import kotlinx.coroutines.flow.Flow

/**
 * SPW 创意工坊事件总线
 *
 * 插件通过订阅这些 Flow 来接收播放状态变化通知
 */
interface PlaybackEventBus {
    /**
     * 播放状态变化事件
     */
    val playbackStateChanged: Flow<PlaybackState>

    /**
     * 正在播放状态变化事件
     */
    val isPlayingChanged: Flow<Boolean>

    /**
     * 跳转事件
     */
    val seekTo: Flow<Long>

    /**
     * 播放位置更新事件（每秒）
     */
    val positionUpdated: Flow<Long>

    /**
     * 当前歌词行更新事件
     */
    val lyricsLineUpdated: Flow<LyricsLine?>
}

/**
 * 播放状态
 *
 * @property state 播放器状态
 * @property isPlaying 是否正在播放
 */
data class PlaybackState(
    val state: State,
    val isPlaying: Boolean
) {
    enum class State {
        Idle,
        Buffering,
        Ready,
        Ended
    }
}

/**
 * 歌词行
 *
 * @property startTime 开始时间
 * @property endTime 结束时间
 * @property lyricsCells 歌词单元
 * @property pureMainText 纯文本（主要歌词文本）
 * @property pureSubText 纯翻译文本
 */
data class LyricsLine(
    val startTime: Long,
    val endTime: Long,
    val lyricsCells: List<Cell>,
    val pureMainText: String,
    val pureSubText: String?
) {
    /**
     * 卡拉 OK 歌词中最小的可滚动单元
     *
     * @property startTime 开始时间戳
     * @property endTime 结束时间戳
     * @property text 文本
     */
    data class Cell(
        val startTime: Long,
        val endTime: Long,
        val text: String
    )
}
