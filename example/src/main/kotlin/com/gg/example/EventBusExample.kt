@file:OptIn(UnstableSpwWorkshopApi::class)

package com.gg.example

import com.xuncorp.spw.workshop.api.UnstableSpwWorkshopApi
import com.xuncorp.spw.workshop.api.WorkshopApi
import com.xuncorp.spw.workshop.api.event.PlaybackState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

/**
 * 事件总线示例
 *
 * 演示如何通过 [WorkshopApi.events] 订阅播放状态变化。
 * 旧的 [com.xuncorp.spw.workshop.api.PlaybackExtensionPoint] 已弃用，
 * 请迁移到此基于 Flow 的事件总线机制。
 */
class EventBusExample {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    fun start() {
        // 播放状态变化（Idle / Buffering / Ready / Ended）
        scope.launch {
            WorkshopApi.playbackEventBus.playbackStateChanged.collectLatest { state ->
                when (state.state) {
                    PlaybackState.State.Idle -> {
                        WorkshopApi.ui.toast("播放器空闲", WorkshopApi.Ui.ToastType.Warning)
                    }

                    PlaybackState.State.Buffering -> {
                        WorkshopApi.ui.toast("正在缓冲音频", WorkshopApi.Ui.ToastType.Warning)
                    }

                    PlaybackState.State.Ready -> {
                        WorkshopApi.ui.toast("播放器就绪", WorkshopApi.Ui.ToastType.Success)
                    }

                    PlaybackState.State.Ended -> {
                        WorkshopApi.ui.toast("播放结束", WorkshopApi.Ui.ToastType.Success)
                    }
                }
            }
        }

        // 播放/暂停状态变化
        scope.launch {
            WorkshopApi.playbackEventBus.isPlayingChanged.collectLatest { isPlaying ->
                WorkshopApi.ui.toast(
                    "▶️ 播放状态: ${if (isPlaying) "播放中" else "已暂停"}",
                    WorkshopApi.Ui.ToastType.Success
                )
            }
        }

        // 进度跳转
        scope.launch {
            WorkshopApi.playbackEventBus.seekTo.collectLatest { position ->
                WorkshopApi.ui.toast(
                    "⏭️ 跳转到位置: ${formatTime(position)}",
                    WorkshopApi.Ui.ToastType.Success
                )
            }
        }

        // 播放位置更新（每秒）
        scope.launch {
            WorkshopApi.playbackEventBus.positionUpdated.collectLatest { position ->
                println("⏱️ 播放位置: ${formatTime(position)}")
            }
        }

        // 当前歌词行更新
        scope.launch {
            WorkshopApi.playbackEventBus.lyricsLineUpdated.collectLatest { lyricsLine ->
                if (lyricsLine != null) {
                    WorkshopApi.ui.toast(
                        "🎤 当前歌词: ${lyricsLine.pureMainText}",
                        WorkshopApi.Ui.ToastType.Success
                    )
                } else {
                    WorkshopApi.ui.toast("🎤 当前无歌词", WorkshopApi.Ui.ToastType.Warning)
                }
            }
        }
    }

    fun stop() {
        scope.cancel()
    }

    private fun formatTime(milliseconds: Long): String {
        val seconds = milliseconds / 1000
        val minutes = seconds / 60
        val remainingSeconds = seconds % 60
        return String.format("%02d:%02d", minutes, remainingSeconds)
    }
}
