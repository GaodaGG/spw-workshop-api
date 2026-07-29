/*
 * SPW Workshop API
 * Copyright (C) 2026 Moriafly
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

@file:OptIn(com.xuncorp.spw.workshop.api.UnstableSpwWorkshopApi::class)
@file:Suppress("unused")

package com.xuncorp.spw.workshop.api.audio

import com.xuncorp.spw.workshop.api.SinceApi
import com.xuncorp.spw.workshop.api.UnstableSpwWorkshopApi

/**
 * 每音源音频管线的扩展入口
 *
 * 注册成功不代表 Provider 一定会被选中。候选选择、任务调度、信任决策以及活动 Session
 * 的生命周期均由宿主管理
 */
@SinceApi("1.16", "0.2.0-dev02")
@UnstableSpwWorkshopApi
interface AudioPipeline {
    /**
     * 注册安全响度增益 Provider
     *
     * 返回的注册令牌应由插件保存，并在插件停止时关闭
     */
    fun registerGainProvider(provider: LoudnessGainProvider): AudioRegistration

    /**
     * 注册自定义音频解码器
     *
     * 解码器是否参与候选选择还取决于 Manifest 能力声明和用户授权
     */
    fun registerDecoder(provider: AudioDecoderProvider): AudioRegistration

    /**
     * 注册受信任的实时 PCM Processor 工厂
     *
     * Processor 在每个音源进入 Tempo、SRC 和 Mixer 之前执行
     */
    fun registerProcessor(factory: AudioProcessorFactory): AudioRegistration
}

/**
 * 单次音频管线注册的令牌
 *
 * [close] 必须幂等。该方法返回后，对应 Provider 不会再被选用于新任务。宿主会等待已经进入的
 * 实时回调结束，再释放其资源
 */
@SinceApi("1.16", "0.2.0-dev02")
@UnstableSpwWorkshopApi
fun interface AudioRegistration : AutoCloseable {
    override fun close()
}
