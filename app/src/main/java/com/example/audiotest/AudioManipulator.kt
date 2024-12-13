package com.example.audiotest

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.AudioTrack
import android.media.MediaRecorder
import android.util.Log
import be.tarsos.dsp.AudioDispatcher
import be.tarsos.dsp.AudioEvent
import be.tarsos.dsp.AudioProcessor
import be.tarsos.dsp.io.android.AudioDispatcherFactory
import kotlin.math.sin

class AudioManipulator {
    private val TAG = "AudioManipulator"
    private var audioRecord: AudioRecord? = null
    private var audioTrack: AudioTrack? = null
    private var dispatcher: AudioDispatcher? = null
    private val sampleRate = 44100
    private val bufferSize = AudioRecord.getMinBufferSize(
        sampleRate,
        AudioFormat.CHANNEL_IN_MONO,
        AudioFormat.ENCODING_PCM_16BIT
    )

    fun start() {
        Log.d(TAG, "Starting audio manipulation...")
        try {
            audioRecord = AudioRecord(
                MediaRecorder.AudioSource.VOICE_COMMUNICATION,
                sampleRate,
                AudioFormat.CHANNEL_IN_MONO,
                AudioFormat.ENCODING_PCM_16BIT,
                bufferSize
            )

            if (audioRecord?.state != AudioRecord.STATE_INITIALIZED) {
                Log.e(TAG, "AudioRecord is not initialized")
                return
            }

            audioTrack = AudioTrack.Builder()
                .setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_VOICE_COMMUNICATION)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                        .build()
                )
                .setAudioFormat(
                    AudioFormat.Builder()
                        .setSampleRate(sampleRate)
                        .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                        .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                        .build()
                )
                .setBufferSizeInBytes(AudioTrack.getMinBufferSize(sampleRate, AudioFormat.CHANNEL_OUT_MONO, AudioFormat.ENCODING_PCM_16BIT))
                .build()

            audioTrack?.play()

            dispatcher = AudioDispatcherFactory.fromDefaultMicrophone(sampleRate, 1024, 0)

            // Edit audio via DSP fwk
            dispatcher?.addAudioProcessor(object : AudioProcessor {
                override fun process(audioEvent: AudioEvent): Boolean {
                    Log.d(TAG, "Processing audio")
                    val audioBuffer = audioEvent.floatBuffer
                    addNoise(audioBuffer)
                    addBeep(audioBuffer)
                    playProcessedAudio(audioBuffer)
                    return true
                }

                override fun processingFinished() {
                    Log.d(TAG, "Processing finished")
                }
            })

            Thread(dispatcher).start()
        } catch (e: Exception) {
            Log.e(TAG, "Error starting audio manipulation: ${e.message}")
        } catch (e: SecurityException) {
            Log.e(TAG, "Error starting audio manipulation, missing permission??: ${e.message}")
        }
    }

    fun stop() {
        Log.d(TAG, "Stopping audio manipulation...")
        try {
            dispatcher?.stop()
            audioRecord?.stop()
            audioRecord?.release()
            audioTrack?.stop()
            audioTrack?.release()
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping audio manipulation: ${e.message}")
        }
    }

    // Add a noise
    private fun addNoise(audioBuffer: FloatArray) {
        for (i in audioBuffer.indices) {
            audioBuffer[i] = (Math.random().toFloat() * 1.0f - 0.5f)
            //audioBuffer[i] = audioBuffer[i].coerceIn(-1.0f, 1.0f)
        }
    }

    // Add a beep
    private fun addBeep(audioBuffer: FloatArray, frequency: Float = 440.0f, sampleRate: Int = 44100) {
        var phase = 0.0
        val increment = 2 * Math.PI * frequency / sampleRate // Phase increment for sine wave

        for (i in audioBuffer.indices) {
            audioBuffer[i] = (sin(phase) * 0.8).toFloat()
            //audioBuffer[i] = audioBuffer[i].coerceIn(-1.0f, 1.0f)

            phase += increment

            if (phase >= 2 * Math.PI) {
                phase -= 2 * Math.PI
            }
        }
    }

    private fun playProcessedAudio(audioBuffer: FloatArray) {
        val byteBuffer = convertToPCM16(audioBuffer)
        audioTrack?.write(byteBuffer, 0, byteBuffer.size)
        Log.e(TAG, "Playing processed audio")
        audioTrack?.play()
    }

    private fun convertToPCM16(audioBuffer: FloatArray): ByteArray {
        val byteBuffer = ByteArray(audioBuffer.size * 2)
        for (i in audioBuffer.indices) {
            val value = (audioBuffer[i] * Short.MAX_VALUE).toInt()
            byteBuffer[i * 2] = (value and 0xFF).toByte()
            byteBuffer[i * 2 + 1] = ((value shr 8) and 0xFF).toByte()
        }
        return byteBuffer
    }
}
