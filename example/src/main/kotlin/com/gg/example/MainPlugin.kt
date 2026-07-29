@file:OptIn(UnstableSpwWorkshopApi::class)
@file:Suppress("unused")

package com.gg.example

import com.xuncorp.spw.workshop.api.PluginContext
import com.xuncorp.spw.workshop.api.SpwPlugin
import com.xuncorp.spw.workshop.api.UnstableSpwWorkshopApi
import com.xuncorp.spw.workshop.api.WorkshopApi
import com.xuncorp.spw.workshop.api.config.ConfigHelper

class MainPlugin(
    pluginContext: PluginContext
) : SpwPlugin(pluginContext) {
    private val eventBusExample = EventBusExample()
    private val lyricsProviderExample = LyricsProviderExample()
    private val audioPipelineExamples = AudioPipelineExamples()

    override fun start() {
        WorkshopApi.ui.toast("示例插件已启动", WorkshopApi.Ui.ToastType.Success)
        println(pluginContext.toString())

        // 启动事件总线监听（替代已弃用的 PlaybackExtensionPoint）
        eventBusExample.start()

        // 注册歌词提供者
        lyricsProviderExample.register()

        audioPipelineExamples.register()

        ConfigExample()
    }

    override fun stop() {
        // 取消事件订阅
        eventBusExample.stop()

        // 注销歌词提供者
        lyricsProviderExample.unregister()

        audioPipelineExamples.close()

        WorkshopApi.ui.toast("示例插件已停止", WorkshopApi.Ui.ToastType.Warning)
    }

    override fun delete() {
        WorkshopApi.ui.toast("示例插件已删除", WorkshopApi.Ui.ToastType.Error)
    }

    override fun update() {
        WorkshopApi.ui.toast("示例插件已更新", WorkshopApi.Ui.ToastType.Success)
    }

    companion object {
        @JvmStatic
        @JvmName("onExampleButtonClick")
        fun onExampleButtonClick() {
            val configHelper: ConfigHelper =
                WorkshopApi.manager
                    .createConfigManager()
                    .getConfig("folder/config.json")

            configHelper.reload()
            configHelper.get("example.edittext", "试试在上方的输入框输入点什么？").let {
                WorkshopApi.ui.toast(
                    "配置项 example.edittext 的值是: $it",
                    WorkshopApi.Ui.ToastType.Success
                )
            }
        }
    }
}
