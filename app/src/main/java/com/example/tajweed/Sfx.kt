package com.example.tajweed

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.sin

// مؤثرات صوتية قصيرة تُولَّد برمجيًا (بلا ملفات صوت)
object Sfx {
    var enabled = true
    private const val SR = 22050

    private fun render(notes: List<Pair<Double, Int>>, harsh: Boolean = false): ShortArray {
        val sizes = notes.map { SR * it.second / 1000 }
        val out = ShortArray(sizes.sum())
        var off = 0
        notes.forEachIndexed { k, (f, _) ->
            val n = sizes[k]
            for (i in 0 until n) {
                val t = i.toDouble() / SR
                val env = min(1.0, i / (SR * 0.006)) * exp(-3.2 * i / n)
                val w = if (harsh) {
                    var a = 0.0
                    for (h in 1..5) a += sin(2 * PI * f * h * t) / h
                    a * 0.7
                } else {
                    sin(2 * PI * f * t) + 0.35 * sin(4 * PI * f * t) + 0.12 * sin(6 * PI * f * t)
                }
                out[off + i] = (w * env * 0.42 * 32767).toInt().coerceIn(-32767, 32767).toShort()
            }
            off += n
        }
        return out
    }

    private fun play(data: ShortArray) {
        if (!enabled) return
        Thread {
            try {
                val t = AudioTrack.Builder()
                    .setAudioAttributes(AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_GAME)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION).build())
                    .setAudioFormat(AudioFormat.Builder()
                        .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                        .setSampleRate(SR)
                        .setChannelMask(AudioFormat.CHANNEL_OUT_MONO).build())
                    .setBufferSizeInBytes(data.size * 2)
                    .setTransferMode(AudioTrack.MODE_STATIC)
                    .build()
                t.write(data, 0, data.size)
                t.play()
                Thread.sleep(data.size * 1000L / SR + 150)
                t.release()
            } catch (e: Exception) { }
        }.start()
    }

    // نغمة صاعدة، وكلما زادت الإجابات الصحيحة المتتالية ارتفعت درجة النغمة
    fun correct(level: Int = 1) {
        val r = 2.0.pow((min(level, 8) - 1) / 12.0)
        play(render(listOf(659.3 * r to 80, 784.0 * r to 80, 1046.5 * r to 240)))
    }
    fun wrong() = play(render(listOf(196.0 to 130, 146.8 to 260), harsh = true))
    fun win() = play(render(listOf(523.3 to 110, 659.3 to 110, 784.0 to 110, 1046.5 to 420)))
    fun lose() = play(render(listOf(392.0 to 170, 329.6 to 170, 261.6 to 400), harsh = true))
}
