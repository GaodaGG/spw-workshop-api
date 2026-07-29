# 音频管线

Workshop API 0.2 从 `WorkshopApi.audioPipeline` 提供三种扩展能力：

1. `LoudnessGainProvider` 接收宿主计算的响度，只返回每个音源的目标增益
2. `AudioDecoderProvider` 将宿主提供的编码字节流解码为交错 Float32 PCM
3. `AudioProcessorFactory` 为每个音源创建受信任的原地 PCM 处理器

这些接口从 SPW `1.16`、Workshop API `0.2.0-dev02` 开始提供，并带有
`@UnstableSpwWorkshopApi`。插件需要显式 opt-in。

## 管线与选择

宿主的固定顺序为：

`编码源 → 解码器 → 响度计量 → 每源增益 → PCM Processor → Tempo/SRC → Automix/Mixer → EQ → 用户音量 → 输出`

解码器按 `Preferred 插件 → 内建解码器 → Fallback 插件` 尝试。插件候选先按
`confidence` 降序、再按 `priority` 降序，最后按插件 ID、Provider ID 升序稳定排序。
`Preferred` 仅对已授权的受信任插件生效。Processor 的 `order` 越小越先执行；
相同 `order` 按插件 ID、Factory ID 升序执行。

`probe` 和 `open` 在 IO worker 执行。每次播放、分析和预加载都创建独立 Session；
`DecoderOpenRequest.purpose` 可用于区分三种用途。`initialFrame` 指定 Session 首次读取前应处于的帧。
Processor 可通过 `AudioProcessorContext.purpose` 区分相同的三种用途。

解码输出必须是 native-endian、interleaved Float32。`readFrames` 返回 `0..maxFrames`，
其中 0 表示 EOF。返回的帧数不能超过目标 Buffer 的可用容量。读取过程失败后宿主不会在播放中热切换解码器。

## 实时安全

`LoudnessGainProvider` 不在实时音频线程执行，通知以最高 10 Hz 合并投递。宿主将目标增益限制为
`-24..+12 dB`，并把斜坡限制为至少 50 ms。返回 `null` 会平滑恢复到 0 dB。

`AudioProcessor.process` 在实时音频回调执行。实现中不得执行 IO、获取竞争锁、启动协程、
调用播放器 API 或分配大对象，也不得保留 `MutablePcmBlock` 或其 Buffer。处理器只能原地修改样本，
不能改变格式或帧数。处理器抛出异常后，宿主会旁路该实例；Seek 后宿主会调用 `flush`。

Decoder 和 Processor 运行在桌面 JVM 的同一进程内，因此属于 experimental/trusted 能力。
宿主可以旁路抛出异常的 Processor，但无法安全中止卡死的回调，也无法隔离插件触发的
native crash。仅应安装并授权可信来源的插件。

注册返回的 `AudioRegistration` 必须保存到插件生命周期内。插件停止时调用 `close`；
该操作是幂等的。Session 和 Processor 的 `close` 同样必须幂等。

## Manifest

声明音频能力的插件应配置：

```text
Plugin-Api-Min: 2
Plugin-Api-Max: 2
Plugin-Capabilities: audio.gain,audio.decoder.trusted,audio.processor.trusted
```

只声明实际使用的能力。`audio.decoder.trusted` 和 `audio.processor.trusted` 需要宿主用户授权。
缺少 `Plugin-Api-Min` 的旧插件按 API level 1 处理；缺少 `Plugin-Api-Max`
表示不限制宿主上限；缺少能力字段不会获得敏感音频能力。

可编译示例位于 `example/src/main/kotlin/com/gg/example`：

- `AudioPipelineExamplePlugin` 是可直接打包的完整插件入口，演示注册失败回滚和停止时逆序注销
- `SafeLoudnessGainExample` 实现安全响度归一化
- `Unsigned8BitDecoderExample` 实现自定义单声道解码
- `GainProcessorExample` 实现受信任的 100 ms 实时原地增益斜坡

在仓库根目录执行以下命令即可验证并构建示例：

```shell
./gradlew :example:compileKotlin
./gradlew :example:plugin
```

生成的插件 Manifest 会声明：

```text
Plugin-Class: com.gg.example.AudioPipelineExamplePlugin
Plugin-Api-Min: 2
Plugin-Api-Max: 2
Plugin-Capabilities: audio.gain,audio.decoder.trusted,audio.processor.trusted
```
