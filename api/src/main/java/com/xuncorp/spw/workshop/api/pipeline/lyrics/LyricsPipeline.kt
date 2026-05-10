@file:Suppress("unused")

package com.xuncorp.spw.workshop.api.pipeline.lyrics

/**
 * 歌词管道接口
 *
 * 插件通过注册 [LyricsProvider] 来参与歌词查找流程
 * 宿主按优先级顺序执行所有已注册的 provider，第一个非 null 结果即为最终歌词
 */
interface LyricsPipeline {
    /**
     * 注册一个歌词提供者
     *
     * @param provider 歌词提供者
     */
    fun register(provider: LyricsProvider)

    /**
     * 注销一个歌词提供者
     *
     * @param provider 歌词提供者
     */
    fun unregister(provider: LyricsProvider)
}
