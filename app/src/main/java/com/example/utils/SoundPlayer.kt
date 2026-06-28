package com.example.utils

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioTrack
import android.media.RingtoneManager
import android.net.Uri
import android.util.Log

object SoundPlayer {

    fun playSound(context: Context, option: Int) {
        when (option) {
            1 -> { // Silent / صامت
                // No audio playback
            }
            2 -> { // Default Notification Sound
                playSystemNotification(context)
            }
            3 -> { // Bubble Pop Effect (Synthesized)
                playBubblePop()
            }
            4 -> { // Classic Chime (Synthesized)
                playClassicChime()
            }
        }
    }

    private fun playSystemNotification(context: Context) {
        try {
            val notificationUri: Uri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
            val ringtone = RingtoneManager.getRingtone(context, notificationUri)
            ringtone?.play()
        } catch (e: Exception) {
            Log.e("SoundPlayer", "Failed to play default notification sound", e)
        }
    }

    private fun playBubblePop() {
        Thread {
            try {
                val sampleRate = 22050
                val durationMs = 150
                val numSamples = (sampleRate * (durationMs / 1000.0)).toInt()
                val buffer = ShortArray(numSamples)

                // High-to-low sweep to mimic a cute "pop" sound
                val startFreq = 850.0
                val endFreq = 180.0

                for (i in 0 until numSamples) {
                    val t = i.toDouble() / sampleRate
                    val progress = t / (durationMs / 1000.0)
                    val currentFreq = startFreq - (startFreq - endFreq) * progress
                    
                    val angle = 2.0 * Math.PI * currentFreq * t
                    val sineVal = Math.sin(angle)
                    
                    // Cute volume envelope (quick rise, exponential decay)
                    val envelope = if (progress < 0.08) {
                        progress / 0.08
                    } else {
                        Math.exp(-6.0 * (progress - 0.08))
                    }
                    
                    buffer[i] = (sineVal * 32767.0 * envelope).toInt().toShort()
                }

                val audioTrack = AudioTrack.Builder()
                    .setAudioAttributes(
                        AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_NOTIFICATION)
                            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                            .build()
                    )
                    .setAudioFormat(
                        AudioFormat.Builder()
                            .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                            .setSampleRate(sampleRate)
                            .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                            .build()
                    )
                    .setBufferSizeInBytes(buffer.size * 2)
                    .setTransferMode(AudioTrack.MODE_STATIC)
                    .build()

                audioTrack.write(buffer, 0, buffer.size)
                audioTrack.play()
                
                Thread.sleep(durationMs.toLong() + 50)
                audioTrack.stop()
                audioTrack.release()
            } catch (e: Exception) {
                Log.e("SoundPlayer", "Failed to synthesize pop sound", e)
            }
        }.start()
    }

    private fun playClassicChime() {
        Thread {
            try {
                val sampleRate = 22050
                val durationMs = 450
                val numSamples = (sampleRate * (durationMs / 1000.0)).toInt()
                val buffer = ShortArray(numSamples)

                // Frequencies for a classic sparkling bell chime (C5 + E5 + G5 harmonics)
                val freqC = 523.25
                val freqE = 659.25
                val freqG = 783.99

                for (i in 0 until numSamples) {
                    val t = i.toDouble() / sampleRate
                    val progress = t / (durationMs / 1000.0)
                    
                    val val1 = Math.sin(2.0 * Math.PI * freqC * t)
                    val val2 = Math.sin(2.0 * Math.PI * freqE * t)
                    val val3 = Math.sin(2.0 * Math.PI * freqG * t)
                    
                    val mixed = (val1 + 0.5 * val2 + 0.3 * val3) / 1.8
                    
                    // Sparkly envelope: fast attack, slow smooth decay
                    val envelope = if (progress < 0.04) {
                        progress / 0.04
                    } else {
                        Math.exp(-4.5 * (progress - 0.04))
                    }

                    buffer[i] = (mixed * 32767.0 * envelope).toInt().toShort()
                }

                val audioTrack = AudioTrack.Builder()
                    .setAudioAttributes(
                        AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_NOTIFICATION)
                            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                            .build()
                    )
                    .setAudioFormat(
                        AudioFormat.Builder()
                            .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                            .setSampleRate(sampleRate)
                            .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                            .build()
                    )
                    .setBufferSizeInBytes(buffer.size * 2)
                    .setTransferMode(AudioTrack.MODE_STATIC)
                    .build()

                audioTrack.write(buffer, 0, buffer.size)
                audioTrack.play()

                Thread.sleep(durationMs.toLong() + 50)
                audioTrack.stop()
                audioTrack.release()
            } catch (e: Exception) {
                Log.e("SoundPlayer", "Failed to synthesize chime sound", e)
            }
        }.start()
    }
}
