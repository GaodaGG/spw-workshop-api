@file:OptIn(com.xuncorp.spw.workshop.api.UnstableSpwWorkshopApi::class)

package com.gg.example

import com.xuncorp.spw.workshop.api.audio.AudioDecoderProvider
import com.xuncorp.spw.workshop.api.audio.AudioDecoderSession
import com.xuncorp.spw.workshop.api.audio.AudioStreamInfo
import com.xuncorp.spw.workshop.api.audio.DecoderOpenRequest
import com.xuncorp.spw.workshop.api.audio.DecoderOpenResult
import com.xuncorp.spw.workshop.api.audio.DecoderPolicy
import com.xuncorp.spw.workshop.api.audio.DecoderProbeRequest
import com.xuncorp.spw.workshop.api.audio.DecoderProbeResult
import com.xuncorp.spw.workshop.api.audio.PcmFormat
import java.nio.ByteBuffer
import java.nio.FloatBuffer

class Unsigned8BitDecoderExample : AudioDecoderProvider {
    override val id: String = "unsigned-8-bit-mono"
    override val priority: Int = 10
    override val policy: DecoderPolicy = DecoderPolicy.Fallback
    override val supportedExtensions: Set<String> = setOf(EXTENSION)
    override val supportedMimeTypes: Set<String> = setOf(MIME_TYPE)

    override fun probe(request: DecoderProbeRequest): DecoderProbeResult {
        return if (
            request.extension.equals(EXTENSION, ignoreCase = true) ||
            request.mimeType.equals(MIME_TYPE, ignoreCase = true)
        ) {
            DecoderProbeResult.Supported(confidence = 100)
        } else {
            DecoderProbeResult.Unsupported
        }
    }

    override fun open(request: DecoderOpenRequest): DecoderOpenResult {
        val session = Unsigned8BitSession(request)
        return if (request.initialFrame == 0L) {
            DecoderOpenResult.Opened(session)
        } else if (request.input.isSeekable) {
            session.seekToFrame(request.initialFrame)
            DecoderOpenResult.Opened(session)
        } else {
            DecoderOpenResult.Failed(
                code = "initial_seek_unsupported",
                message = "The input cannot seek to the requested initial frame"
            )
        }
    }

    private class Unsigned8BitSession(
        private val request: DecoderOpenRequest
    ) : AudioDecoderSession {
        override val info = AudioStreamInfo(
            format = PcmFormat(sampleRateHz = SAMPLE_RATE_HZ, channelCount = 1),
            totalFrames = request.input.length,
            isSeekable = request.input.isSeekable,
            codec = "pcm-u8",
            container = "raw",
            sourceBitDepth = 8,
            bitrateBitsPerSecond = SAMPLE_RATE_HZ.toLong() * 8
        )

        private val encodedBuffer = ByteBuffer.allocateDirect(BUFFER_SIZE)

        override fun readFrames(destination: FloatBuffer, maxFrames: Int): Int {
            require(maxFrames >= 0) { "maxFrames must not be negative" }
            val requestedFrames = minOf(maxFrames, destination.remaining())
            var framesRead = 0
            while (framesRead < requestedFrames) {
                encodedBuffer.clear()
                encodedBuffer.limit(minOf(encodedBuffer.capacity(), requestedFrames - framesRead))
                val byteCount = request.input.read(encodedBuffer)
                if (byteCount <= 0) {
                    break
                }
                encodedBuffer.flip()
                repeat(byteCount) {
                    val unsignedSample = encodedBuffer.get().toInt() and 0xff
                    destination.put((unsignedSample - 128) / 128f)
                }
                framesRead += byteCount
            }
            return framesRead
        }

        override fun seekToFrame(frame: Long): Long {
            return request.input.seekTo(frame)
        }

        override fun close() = Unit
    }

    private companion object {
        const val EXTENSION = "rawu8"
        const val MIME_TYPE = "audio/x-raw-u8"
        const val SAMPLE_RATE_HZ = 48_000
        const val BUFFER_SIZE = 4_096
    }
}
