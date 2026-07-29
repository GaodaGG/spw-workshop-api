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

/**
 * 接收宿主计算的响度，并返回当前音源的目标增益
 *
 * 回调以最高 10 Hz 合并投递，且不会在实时音频回调中执行。Provider 必须将
 * [LoudnessSnapshot.sourceToken] 视为不透明标识，不得在不同 token 之间复用状态
 */
@SinceApi("1.16", "0.2.0-dev02")
@UnstableSpwWorkshopApi
interface LoudnessGainProvider {
    /** Provider 在当前插件内唯一且稳定的标识 */
    val id: String

    /** 候选优先级，数值越大越优先 */
    val priority: Int
        get() = 0

    /**
     * 返回目标增益，返回 `null` 表示将当前音源恢复到 0 dB
     */
    fun onLoudness(snapshot: LoudnessSnapshot): GainTarget?
}

/**
 * 宿主为单个音源计算的 EBU R128 风格响度数据
 *
 * 可空的测量值为 `null` 时，表示当前尚未得到该项数据
 *
 * @property sourceToken 音源不透明标识，仅在当前播放生命周期内有效
 * @property positionMs 当前音源的播放位置，单位为毫秒
 * @property momentaryLufs 约 400 ms 时间窗的瞬时响度
 * @property shortTermLufs 约 3 秒时间窗的短期响度
 * @property integratedLufs 使用门限累计得到的综合响度
 * @property truePeakDbtp 真峰值，单位为 dBTP
 */
@SinceApi("1.16", "0.2.0-dev02")
@UnstableSpwWorkshopApi
data class LoudnessSnapshot(
    val sourceToken: String,
    val positionMs: Long,
    val momentaryLufs: Double?,
    val shortTermLufs: Double?,
    val integratedLufs: Double?,
    val truePeakDbtp: Double?
)

/**
 * 插件请求的每音源目标增益
 *
 * 宿主会将 [gainDb] 限制在 -24..+12 dB，并确保 [rampMs] 不小于 50 ms
 *
 * @property gainDb 目标增益，单位为 dB
 * @property rampMs 到达目标增益所使用的斜坡时长，单位为毫秒
 */
@SinceApi("1.16", "0.2.0-dev02")
@UnstableSpwWorkshopApi
data class GainTarget(
    val gainDb: Double,
    val rampMs: Long = MINIMUM_RAMP_MS
) {
    init {
        require(gainDb.isFinite()) { "gainDb must be finite" }
    }

    companion object {
        const val MINIMUM_GAIN_DB: Double = -24.0
        const val MAXIMUM_GAIN_DB: Double = 12.0
        const val MINIMUM_RAMP_MS: Long = 50
    }
}
