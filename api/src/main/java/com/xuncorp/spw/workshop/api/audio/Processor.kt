/*
 * SPW Workshop API
 * Copyright (C) 2026 Moriafly
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 */

@file:OptIn(com.xuncorp.spw.workshop.api.UnstableSpwWorkshopApi::class)
@file:Suppress("unused")

package com.xuncorp.spw.workshop.api.audio

import com.xuncorp.spw.workshop.api.SinceApi
import com.xuncorp.spw.workshop.api.UnstableSpwWorkshopApi
import java.nio.ByteOrder
import java.nio.FloatBuffer

/**
 * 为单个音源创建受信任 PCM Processor 的工厂
 *
 * [order] 越小的工厂越先执行。相同 [order] 按插件 ID、[id] 升序稳定排序。
 * [create] 返回 `null` 表示不为当前上下文创建 Processor
 */
@SinceApi("1.16", "0.2.0-dev02")
@UnstableSpwWorkshopApi
interface AudioProcessorFactory {
    /** Factory 在当前插件内唯一且稳定的标识 */
    val id: String

    /** Processor 在每音源处理链中的顺序，数值越小越先执行 */
    val order: Int
        get() = 0

    /**
     * 为一个音源创建独立的 Processor
     *
     * 该方法不在实时音频回调中执行
     */
    fun create(context: AudioProcessorContext): AudioProcessor?
}

/**
 * 单个每音源 Processor 实例的创建上下文
 *
 * @property sourceToken 音源不透明标识，仅在当前播放生命周期内有效
 * @property format Processor 将接收的 PCM 格式
 * @property totalFrames PCM 总帧数，未知时为 `null`
 * @property metadata 可选的音频元数据
 * @property purpose 当前实例的用途
 */
@SinceApi("1.16", "0.2.0-dev02")
@UnstableSpwWorkshopApi
data class AudioProcessorContext(
    val sourceToken: String,
    val format: PcmFormat,
    val totalFrames: Long?,
    val metadata: Map<String, String> = emptyMap(),
    val purpose: DecoderPurpose = DecoderPurpose.Playback
)

/**
 * 在实时音频回调中原地处理 PCM 的 Processor
 *
 * [process] 在音频回调中执行，不得进行 IO、获取竞争锁、启动协程、调用播放器 API 或分配大对象。
 * Processor 不得持有 block 或其 Buffer，不得改变格式和帧数。抛出异常后，宿主会旁路当前实例。
 * Seek 后宿主会调用 [flush]。[close] 必须幂等
 */
@SinceApi("1.16", "0.2.0-dev02")
@UnstableSpwWorkshopApi
interface AudioProcessor : AutoCloseable {
    /** 原地处理一个借用的 PCM 数据块 */
    fun process(block: MutablePcmBlock)

    /** 清除 Processor 的历史状态，宿主在 Seek 后调用 */
    fun flush() {}

    override fun close() {}
}

/**
 * 借用的 native-endian、交错 Float32 PCM 数据块
 *
 * 从 Buffer 当前 position 开始，恰有
 * `frameCount * format.channelCount` 个样本有效。Processor 不得保留当前对象或 [buffer]
 *
 * @property buffer 可原地修改的 direct [FloatBuffer]
 * @property format 当前 PCM 格式
 * @property frameCount 当前数据块的帧数
 * @property startFrame 当前数据块在音源中的起始帧
 */
@SinceApi("1.16", "0.2.0-dev02")
@UnstableSpwWorkshopApi
class MutablePcmBlock(
    val buffer: FloatBuffer,
    val format: PcmFormat,
    val frameCount: Int,
    val startFrame: Long
) {
    init {
        require(buffer.isDirect) { "buffer must be direct" }
        require(buffer.order() == ByteOrder.nativeOrder()) {
            "buffer must use native byte order"
        }
        require(frameCount >= 0) { "frameCount must not be negative" }
        require(startFrame >= 0) { "startFrame must not be negative" }
        require(
            buffer.remaining().toLong() >=
                frameCount.toLong() * format.channelCount.toLong()
        ) {
            "buffer does not contain frameCount * channelCount samples"
        }
    }
}
