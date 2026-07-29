@file:OptIn(com.xuncorp.spw.workshop.api.UnstableSpwWorkshopApi::class)

package com.xuncorp.spw.workshop.api.audio

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer

class AudioContractsTest {
    @Test
    fun `decoder confidence must be in public range`() {
        assertThrows(IllegalArgumentException::class.java) {
            DecoderProbeResult.Supported(0)
        }
        assertThrows(IllegalArgumentException::class.java) {
            DecoderProbeResult.Supported(101)
        }
        assertEquals(1, DecoderProbeResult.Supported(1).confidence)
        assertEquals(100, DecoderProbeResult.Supported(100).confidence)
    }

    @Test
    fun `decoder open defaults to playback at first frame`() {
        val request = DecoderOpenRequest(
            input = NonSeekableInput,
            fileName = null,
            extension = null,
            mimeType = null
        )

        assertEquals(DecoderPurpose.Playback, request.purpose)
        assertEquals(0, request.initialFrame)
    }

    @Test
    fun `processor context defaults to playback`() {
        val context = AudioProcessorContext(
            sourceToken = "source",
            format = PcmFormat(sampleRateHz = 48_000, channelCount = 2),
            totalFrames = null
        )

        assertEquals(DecoderPurpose.Playback, context.purpose)
    }

    @Test
    fun `non seekable decoder input rejects seek`() {
        assertThrows(UnsupportedOperationException::class.java) {
            NonSeekableInput.seekTo(0)
        }
    }

    @Test
    fun `decoder session cannot report more frames than requested`() {
        val session = BoundedSilenceSession
        val destination = FloatBuffer.allocate(16)

        assertEquals(3, session.readFrames(destination, maxFrames = 3))
        assertEquals(6, destination.position())
    }

    @Test
    fun `pcm block validates available interleaved samples`() {
        val format = PcmFormat(sampleRateHz = 48_000, channelCount = 2)
        val directBuffer = ByteBuffer.allocateDirect(4 * Float.SIZE_BYTES)
            .order(ByteOrder.nativeOrder())
            .asFloatBuffer()

        assertThrows(IllegalArgumentException::class.java) {
            MutablePcmBlock(
                buffer = FloatBuffer.allocate(3),
                format = format,
                frameCount = 2,
                startFrame = 0
            )
        }
        assertThrows(IllegalArgumentException::class.java) {
            MutablePcmBlock(
                buffer = directBuffer,
                format = format,
                frameCount = 2,
                startFrame = -1
            )
        }
    }

    @Test
    fun `gain target rejects non finite gain`() {
        assertThrows(IllegalArgumentException::class.java) {
            GainTarget(Double.NaN)
        }
        assertThrows(IllegalArgumentException::class.java) {
            GainTarget(Double.POSITIVE_INFINITY)
        }
    }

    @Test
    fun `audio stream info rejects invalid optional measurements`() {
        assertThrows(IllegalArgumentException::class.java) {
            AudioStreamInfo(
                format = PcmFormat(sampleRateHz = 48_000, channelCount = 2),
                totalFrames = -1,
                isSeekable = false,
                codec = null,
                container = null,
                sourceBitDepth = null,
                bitrateBitsPerSecond = null
            )
        }
    }

    private data object NonSeekableInput : DecoderInput {
        override val position: Long = 0
        override val length: Long? = null
        override val isSeekable: Boolean = false

        override fun read(destination: java.nio.ByteBuffer): Int = -1
    }

    private data object BoundedSilenceSession : AudioDecoderSession {
        override val info = AudioStreamInfo(
            format = PcmFormat(sampleRateHz = 48_000, channelCount = 2),
            totalFrames = null,
            isSeekable = false,
            codec = "test",
            container = null,
            sourceBitDepth = null,
            bitrateBitsPerSecond = null
        )

        override fun readFrames(destination: FloatBuffer, maxFrames: Int): Int {
            val frames = minOf(maxFrames, destination.remaining() / info.format.channelCount)
            repeat(frames * info.format.channelCount) {
                destination.put(0f)
            }
            return frames
        }

        override fun seekToFrame(frame: Long): Long {
            throw UnsupportedOperationException("Session is not seekable")
        }

        override fun close() = Unit
    }
}
