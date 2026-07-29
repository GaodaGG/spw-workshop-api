@file:OptIn(com.xuncorp.spw.workshop.api.UnstableSpwWorkshopApi::class)

package com.gg.example

import com.xuncorp.spw.workshop.api.WorkshopApi
import com.xuncorp.spw.workshop.api.audio.AudioPipeline
import com.xuncorp.spw.workshop.api.audio.AudioProcessor
import com.xuncorp.spw.workshop.api.audio.AudioProcessorContext
import com.xuncorp.spw.workshop.api.audio.AudioProcessorFactory
import com.xuncorp.spw.workshop.api.audio.AudioRegistration
import com.xuncorp.spw.workshop.api.audio.GainTarget
import com.xuncorp.spw.workshop.api.audio.LoudnessGainProvider
import com.xuncorp.spw.workshop.api.audio.LoudnessSnapshot
import com.xuncorp.spw.workshop.api.audio.MutablePcmBlock
import kotlin.math.pow

class AudioPipelineExamples : AutoCloseable {
    private val registrations = mutableListOf<AudioRegistration>()

    fun register(pipeline: AudioPipeline = WorkshopApi.audioPipeline) {
        close()
        registrations += pipeline.registerGainProvider(SafeLoudnessGainExample())
        registrations += pipeline.registerDecoder(Unsigned8BitDecoderExample())
        registrations += pipeline.registerProcessor(GainProcessorExample())
    }

    override fun close() {
        registrations.forEach(AudioRegistration::close)
        registrations.clear()
    }
}

class SafeLoudnessGainExample : LoudnessGainProvider {
    override val id: String = "safe-loudness-normalization"
    override val priority: Int = 100

    override fun onLoudness(snapshot: LoudnessSnapshot): GainTarget? {
        val integratedLufs = snapshot.integratedLufs
        return if (integratedLufs == null) {
            null
        } else {
            GainTarget(
                gainDb = TARGET_LUFS - integratedLufs,
                rampMs = 250
            )
        }
    }

    private companion object {
        const val TARGET_LUFS = -14.0
    }
}

class GainProcessorExample : AudioProcessorFactory {
    override val id: String = "trusted-gain"

    override fun create(context: AudioProcessorContext): AudioProcessor {
        return GainRampProcessor(
            targetGainDb = -1.0,
            sampleRateHz = context.format.sampleRateHz
        )
    }

    private class GainRampProcessor(
        targetGainDb: Double,
        sampleRateHz: Int
    ) : AudioProcessor {
        private val targetGain = 10.0.pow(targetGainDb / 20.0)
        private val rampFrames = (
            sampleRateHz.toLong() * RAMP_MS / MILLIS_PER_SECOND
            ).coerceAtLeast(1)
        private var currentGain = UNITY_GAIN
        private var remainingFrames = rampFrames
        private var gainStep = (targetGain - currentGain) / remainingFrames

        override fun process(block: MutablePcmBlock) {
            val buffer = block.buffer
            var index = buffer.position()
            var frame = 0
            while (frame < block.frameCount) {
                val frameEnd = index + block.format.channelCount
                while (index < frameEnd) {
                    buffer.put(index, (buffer.get(index) * currentGain).toFloat())
                    index++
                }
                advanceRamp()
                frame++
            }
        }

        override fun flush() {
            currentGain = UNITY_GAIN
            remainingFrames = rampFrames
            gainStep = (targetGain - currentGain) / remainingFrames
        }

        private fun advanceRamp() {
            if (remainingFrames > 0) {
                currentGain += gainStep
                remainingFrames--
                if (remainingFrames == 0L) {
                    currentGain = targetGain
                }
            }
        }

        private companion object {
            const val RAMP_MS = 100L
            const val MILLIS_PER_SECOND = 1_000L
            const val UNITY_GAIN = 1.0
        }
    }
}
