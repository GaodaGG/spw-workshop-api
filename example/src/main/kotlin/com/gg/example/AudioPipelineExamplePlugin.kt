@file:OptIn(com.xuncorp.spw.workshop.api.UnstableSpwWorkshopApi::class)

package com.gg.example

import com.xuncorp.spw.workshop.api.PluginContext
import com.xuncorp.spw.workshop.api.SpwPlugin
import com.xuncorp.spw.workshop.api.WorkshopApi
import com.xuncorp.spw.workshop.api.audio.AudioRegistration

/**
 * 完整的音频管线示例插件
 *
 * 本示例注册以下三种扩展：
 *
 * 1. [SafeLoudnessGainExample]：由宿主计算响度，插件只返回目标增益
 * 2. [Unsigned8BitDecoderExample]：将 `.rawu8` 编码输入解码为单声道 Float32 PCM
 * 3. [GainProcessorExample]：在实时回调中执行 100 ms 原地增益斜坡
 *
 * Manifest 必须声明示例实际使用的三项音频能力。解码器和 Processor 还需要用户授予信任权限
 */
class AudioPipelineExamplePlugin(
    pluginContext: PluginContext
) : SpwPlugin(pluginContext) {
    private var registrations: List<AudioRegistration> = emptyList()

    override fun start() {
        check(registrations.isEmpty()) { "插件已经启动" }

        val audioPipeline = WorkshopApi.audioPipeline
        val openedRegistrations = mutableListOf<AudioRegistration>()
        try {
            openedRegistrations +=
                audioPipeline.registerGainProvider(SafeLoudnessGainExample())
            openedRegistrations +=
                audioPipeline.registerDecoder(Unsigned8BitDecoderExample())
            openedRegistrations +=
                audioPipeline.registerProcessor(GainProcessorExample())
            registrations = openedRegistrations.toList()
        } catch (throwable: Throwable) {
            try {
                closeAll(openedRegistrations)
            } catch (closeFailure: Throwable) {
                throwable.addSuppressed(closeFailure)
            }
            throw throwable
        }
    }

    override fun stop() {
        val activeRegistrations = registrations
        registrations = emptyList()
        closeAll(activeRegistrations)
    }

    /**
     * 按注册的相反顺序关闭令牌，并确保其中一个关闭失败时仍会继续回收其余令牌
     */
    private fun closeAll(registrations: List<AudioRegistration>) {
        var firstFailure: Throwable? = null
        registrations.asReversed().forEach { registration ->
            try {
                registration.close()
            } catch (throwable: Throwable) {
                if (firstFailure == null) {
                    firstFailure = throwable
                } else {
                    firstFailure.addSuppressed(throwable)
                }
            }
        }
        firstFailure?.let { throw it }
    }
}
