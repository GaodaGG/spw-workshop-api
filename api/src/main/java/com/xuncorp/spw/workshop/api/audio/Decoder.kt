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
import java.nio.ByteBuffer
import java.nio.FloatBuffer

/**
 * 自定义编码音频解码器 Provider
 *
 * [probe] 和 [open] 均在 IO worker 上执行。Provider 必须支持并发调用，并为播放、分析和预加载
 * 分别创建独立的 [AudioDecoderSession]
 */
@SinceApi("1.16", "0.2.0-dev02")
@UnstableSpwWorkshopApi
interface AudioDecoderProvider {
    /** Provider 在当前插件内唯一且稳定的标识 */
    val id: String

    /** 候选优先级，数值越大越优先 */
    val priority: Int
        get() = 0

    /** 相对于内建解码器的候选策略 */
    val policy: DecoderPolicy
        get() = DecoderPolicy.Fallback

    /** 用于初步筛选候选的文件扩展名集合，不包含前导点 */
    val supportedExtensions: Set<String>
        get() = emptySet()

    /** 用于初步筛选候选的 MIME 类型集合 */
    val supportedMimeTypes: Set<String>
        get() = emptySet()

    /**
     * 低成本探测输入是否受支持
     *
     * Provider 不得关闭输入，也不得在方法返回后继续持有输入
     */
    fun probe(request: DecoderProbeRequest): DecoderProbeResult

    /**
     * 打开一个独立的解码 Session
     *
     * 仅当返回 [DecoderOpenResult.Opened] 时，Session 才能在方法返回后继续使用输入
     */
    fun open(request: DecoderOpenRequest): DecoderOpenResult
}

/**
 * 控制自定义解码器在内建解码器之前还是之后参与选择
 *
 * [Preferred] 解码器需要宿主用户显式授权。未获得信任授权的 [Preferred] 注册不会参与候选选择
 */
@SinceApi("1.16", "0.2.0-dev02")
@UnstableSpwWorkshopApi
enum class DecoderPolicy {
    /** 在内建解码器之前尝试 */
    Preferred,

    /** 仅在内建解码器无法打开输入后尝试 */
    Fallback
}

/**
 * 用于低成本判断输入是否受支持的探测请求
 *
 * [input] 归宿主所有。Provider 不得关闭该输入，也不得在 [AudioDecoderProvider.probe] 返回后持有它
 *
 * @property input 宿主提供的编码字节输入
 * @property fileName 原始文件名，无法获得时为 `null`
 * @property extension 不包含前导点的文件扩展名，无法获得时为 `null`
 * @property mimeType 输入的 MIME 类型，无法获得时为 `null`
 */
@SinceApi("1.16", "0.2.0-dev02")
@UnstableSpwWorkshopApi
data class DecoderProbeRequest(
    val input: DecoderInput,
    val fileName: String?,
    val extension: String?,
    val mimeType: String?
)

/**
 * 用于创建解码 Session 的打开请求
 *
 * 返回 [DecoderOpenResult.Opened] 后，Session 可以持有 [input] 直至自身关闭。返回其他结果时，
 * Provider 必须在 [AudioDecoderProvider.open] 返回前停止使用该输入
 *
 * @property input 宿主提供的编码字节输入
 * @property fileName 原始文件名，无法获得时为 `null`
 * @property extension 不包含前导点的文件扩展名，无法获得时为 `null`
 * @property mimeType 输入的 MIME 类型，无法获得时为 `null`
 * @property purpose 本次 Session 的用途
 * @property initialFrame Session 首次读取前应处于的 PCM 帧位置
 */
@SinceApi("1.16", "0.2.0-dev02")
@UnstableSpwWorkshopApi
data class DecoderOpenRequest(
    val input: DecoderInput,
    val fileName: String?,
    val extension: String?,
    val mimeType: String?,
    val purpose: DecoderPurpose = DecoderPurpose.Playback,
    val initialFrame: Long = 0
) {
    init {
        require(initialFrame >= 0) { "initialFrame must not be negative" }
    }
}

/**
 * 打开解码 Session 的宿主用途
 */
@SinceApi("1.16", "0.2.0-dev02")
@UnstableSpwWorkshopApi
enum class DecoderPurpose {
    /** 实际播放 */
    Playback,

    /** 音频分析 */
    Analysis,

    /** 下一音源预加载 */
    Preload
}

/**
 * 由宿主管理的编码字节输入
 *
 * [read] 采用 Channel 风格语义，在输入结束时返回 -1。[seekTo] 返回实际到达的绝对字节位置；
 * 当 [isSeekable] 为 `false` 时，该方法抛出 [UnsupportedOperationException]
 *
 * 插件只能通过此接口读取编码数据，不会获得文件句柄或原生资源句柄
 */
@SinceApi("1.16", "0.2.0-dev02")
@UnstableSpwWorkshopApi
interface DecoderInput {
    /** 当前绝对字节位置 */
    val position: Long

    /** 输入总字节数，未知时为 `null` */
    val length: Long?

    /** 是否支持按绝对字节位置寻址 */
    val isSeekable: Boolean

    /**
     * 将编码字节读入 [destination]
     *
     * 返回写入的字节数；输入结束时返回 -1
     */
    fun read(destination: ByteBuffer): Int

    /**
     * 移动到绝对字节位置，并返回实际到达的位置
     */
    fun seekTo(position: Long): Long {
        throw UnsupportedOperationException("This decoder input is not seekable")
    }
}

/**
 * 解码器探测结果
 */
@SinceApi("1.16", "0.2.0-dev02")
@UnstableSpwWorkshopApi
sealed interface DecoderProbeResult {
    /** Provider 不支持当前输入 */
    data object Unsupported : DecoderProbeResult

    /**
     * Provider 支持当前输入
     *
     * @property confidence 可信度，取值范围为 1..100
     */
    data class Supported(
        val confidence: Int
    ) : DecoderProbeResult {
        init {
            require(confidence in MINIMUM_CONFIDENCE..MAXIMUM_CONFIDENCE) {
                "confidence must be in $MINIMUM_CONFIDENCE..$MAXIMUM_CONFIDENCE"
            }
        }
    }

    companion object {
        /** 支持结果允许的最小可信度 */
        const val MINIMUM_CONFIDENCE: Int = 1

        /** 支持结果允许的最大可信度 */
        const val MAXIMUM_CONFIDENCE: Int = 100
    }
}

/**
 * 打开解码 Session 的结果
 */
@SinceApi("1.16", "0.2.0-dev02")
@UnstableSpwWorkshopApi
sealed interface DecoderOpenResult {
    /**
     * Provider 拒绝当前输入
     *
     * 宿主会继续尝试下一候选解码器
     */
    data object Declined : DecoderOpenResult

    /** 成功打开一个独立的解码 Session */
    data class Opened(
        val session: AudioDecoderSession
    ) : DecoderOpenResult

    /**
     * 显式的终止性失败
     *
     * 与普通异常或 [Declined] 不同，该结果会终止当前音源的解码器回退流程
     *
     * @property code 供日志和诊断使用的稳定错误码
     * @property message 面向开发者的错误说明
     */
    data class Failed(
        val code: String,
        val message: String
    ) : DecoderOpenResult
}

/**
 * 单个独立的解码器实例
 *
 * PCM 输出必须为 native-endian、交错排列的 Float32。[readFrames] 返回实际解码的帧数，
 * 输入结束时返回 0。目标 Buffer 必须能容纳
 * `maxFrames * info.format.channelCount` 个样本。读取错误属于终止性错误，宿主不会在播放途中
 * 热切换解码器。[close] 必须幂等
 */
@SinceApi("1.16", "0.2.0-dev02")
@UnstableSpwWorkshopApi
interface AudioDecoderSession : AutoCloseable {
    /** 当前解码流的格式和源信息 */
    val info: AudioStreamInfo

    /**
     * 将最多 [maxFrames] 帧 PCM 写入 [destination]
     *
     * 返回实际写入的帧数，返回 0 表示输入结束
     */
    fun readFrames(destination: FloatBuffer, maxFrames: Int): Int

    /**
     * 移动到指定 PCM 帧，并返回实际到达的帧位置
     */
    fun seekToFrame(frame: Long): Long

    override fun close()
}

/**
 * 解码得到的交错 Float32 PCM 格式
 *
 * @property sampleRateHz 采样率，单位为 Hz
 * @property channelCount 声道数
 * @property channelMask 扬声器位置位掩码，声道布局未知时为 0
 */
@SinceApi("1.16", "0.2.0-dev02")
@UnstableSpwWorkshopApi
data class PcmFormat(
    val sampleRateHz: Int,
    val channelCount: Int,
    val channelMask: Long = 0
) {
    init {
        require(sampleRateHz > 0) { "sampleRateHz must be positive" }
        require(channelCount > 0) { "channelCount must be positive" }
        require(channelMask >= 0) { "channelMask must not be negative" }
    }
}

/**
 * 解码音频流的信息
 *
 * @property format 解码输出的 PCM 格式
 * @property totalFrames PCM 总帧数，宿主据此结合采样率计算时长，未知时为 `null`
 * @property isSeekable Session 是否支持 [AudioDecoderSession.seekToFrame]
 * @property codec 编解码器名称，未知时为 `null`
 * @property container 容器格式名称，未知时为 `null`
 * @property sourceBitDepth 编码源位深，未知时为 `null`
 * @property bitrateBitsPerSecond 编码源码率，单位为 bit/s，未知时为 `null`
 * @property metadata 可选的音频元数据
 */
@SinceApi("1.16", "0.2.0-dev02")
@UnstableSpwWorkshopApi
data class AudioStreamInfo(
    val format: PcmFormat,
    val totalFrames: Long?,
    val isSeekable: Boolean,
    val codec: String?,
    val container: String?,
    val sourceBitDepth: Int?,
    val bitrateBitsPerSecond: Long?,
    val metadata: Map<String, String> = emptyMap()
) {
    init {
        require(totalFrames == null || totalFrames >= 0) {
            "totalFrames must be null or non-negative"
        }
        require(sourceBitDepth == null || sourceBitDepth > 0) {
            "sourceBitDepth must be null or positive"
        }
        require(bitrateBitsPerSecond == null || bitrateBitsPerSecond > 0) {
            "bitrateBitsPerSecond must be null or positive"
        }
    }
}
