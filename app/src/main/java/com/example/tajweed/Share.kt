package com.example.tajweed

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Shader
import androidx.core.content.FileProvider
import androidx.core.content.res.ResourcesCompat
import java.io.File
import java.io.FileOutputStream

// يرسم بطاقة صورة بتقدم المتعلم ويفتح قائمة المشاركة
fun shareProgress(ctx: Context, st: Store) {
    try {
        val size = 1080
        val bmp = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val c = Canvas(bmp)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        paint.shader = LinearGradient(0f, 0f, 0f, size.toFloat(), 0xFF14523D.toInt(), 0xFF2A9272.toInt(), Shader.TileMode.CLAMP)
        c.drawRect(0f, 0f, size.toFloat(), size.toFloat(), paint)
        paint.shader = null
        paint.typeface = ResourcesCompat.getFont(ctx, R.font.qomra_bold)
        paint.textAlign = Paint.Align.CENTER
        fun text(s: String, y: Float, sz: Float, col: Int) {
            paint.textSize = sz; paint.color = col; c.drawText(s, size / 2f, y, paint)
        }
        text("تجويد", 220f, 130f, 0xFFFFFFFF.toInt())
        text("🔥 ${st.streak} يوم متتالي", 440f, 84f, 0xFFFFE08A.toInt())
        text("⭐ ${st.xp} نقطة", 580f, 84f, 0xFFFFE08A.toInt())
        text("أكملت ${st.stars.count { it > 0 }} من ${lessons.size} درسًا", 720f, 66f, 0xFFFAF5E9.toInt())
        text("🏅 ${badges.count { it.ok(st) }} من ${badges.size} إنجازًا", 830f, 66f, 0xFFFAF5E9.toInt())
        text("تعلّم أحكام التجويد خطوة خطوة", 980f, 48f, 0xFFD4A63A.toInt())
        val dir = File(ctx.cacheDir, "share").apply { mkdirs() }
        val f = File(dir, "progress.png")
        FileOutputStream(f).use { bmp.compress(Bitmap.CompressFormat.PNG, 100, it) }
        val uri = FileProvider.getUriForFile(ctx, ctx.packageName + ".files", f)
        val send = Intent(Intent.ACTION_SEND).apply {
            type = "image/png"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_TEXT, "تقدمي في تطبيق تجويد")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        ctx.startActivity(Intent.createChooser(send, "شارك تقدمك").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    } catch (e: Exception) { }
}
