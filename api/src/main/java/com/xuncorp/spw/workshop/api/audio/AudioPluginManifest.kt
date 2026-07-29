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

@file:Suppress("unused")

package com.xuncorp.spw.workshop.api.audio

import com.xuncorp.spw.workshop.api.SinceApi
import com.xuncorp.spw.workshop.api.UnstableSpwWorkshopApi

/**
 * 用于声明 API 兼容范围和插件能力的 Manifest 常量
 */
@SinceApi("1.16", "0.2.0-dev02")
@UnstableSpwWorkshopApi
object WorkshopPluginManifest {
    /**
     * Workshop API 0.2 实现的 API level
     */
    const val CURRENT_API_LEVEL: Int = 2

    /**
     * 旧插件未声明 [API_MIN_ATTRIBUTE] 时采用的 API level
     */
    const val LEGACY_API_LEVEL: Int = 1

    /** 插件支持的最低 API level 对应的 Manifest 属性名 */
    const val API_MIN_ATTRIBUTE: String = "Plugin-Api-Min"

    /** 插件支持的最高 API level 对应的 Manifest 属性名 */
    const val API_MAX_ATTRIBUTE: String = "Plugin-Api-Max"

    /** 插件能力列表对应的 Manifest 属性名 */
    const val CAPABILITIES_ATTRIBUTE: String = "Plugin-Capabilities"

    /** 使用安全响度增益能力时需要声明的能力值 */
    const val AUDIO_GAIN_CAPABILITY: String = "audio.gain"

    /** 使用受信任自定义解码器时需要声明的能力值 */
    const val AUDIO_DECODER_TRUSTED_CAPABILITY: String = "audio.decoder.trusted"

    /** 使用受信任实时 PCM Processor 时需要声明的能力值 */
    const val AUDIO_PROCESSOR_TRUSTED_CAPABILITY: String = "audio.processor.trusted"
}
