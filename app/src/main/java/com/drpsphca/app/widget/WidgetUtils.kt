package com.drpsphca.app.widget

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Typeface
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import java.io.File
import java.io.FileOutputStream
import java.net.URL

object WidgetUtils {

    fun textToBitmap(
        context: Context,
        text: String,
        fontSizeSp: Float,
        color: Color,
        fontResId: Int,
        maxWidthPx: Int
    ): Bitmap {
        val density = context.resources.displayMetrics.density
        val fontSizePx = fontSizeSp * density
        
        val typeface = try {
            androidx.core.content.res.ResourcesCompat.getFont(context, fontResId)
        } catch (e: Exception) {
            Typeface.DEFAULT
        }

        val textPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            this.typeface = typeface
            this.textSize = fontSizePx
            this.color = color.toArgb()
        }

        val alignment = Layout.Alignment.ALIGN_NORMAL
        val spacingMultiplier = 1.0f
        val spacingAddition = 0.0f
        val includePadding = false

        val staticLayout = StaticLayout.Builder.obtain(text, 0, text.length, textPaint, maxWidthPx)
            .setAlignment(alignment)
            .setLineSpacing(spacingAddition, spacingMultiplier)
            .setIncludePad(includePadding)
            .build()

        val bitmap = Bitmap.createBitmap(
            maxWidthPx,
            staticLayout.height,
            Bitmap.Config.ARGB_8888
        )

        val canvas = Canvas(bitmap)
        staticLayout.draw(canvas)

        return bitmap
    }

    fun downloadAndSaveImage(context: Context, url: String, fileName: String): String? {
        return try {
            val connection = URL(url).openConnection()
            connection.doInput = true
            connection.connect()
            val input = connection.getInputStream()
            val originalBitmap = BitmapFactory.decodeStream(input)
            
            // Resize to reasonable size for widget (max 600px width)
            val maxWidth = 600
            val resizedBitmap = if (originalBitmap.width > maxWidth) {
                val aspectRatio = originalBitmap.height.toDouble() / originalBitmap.width.toDouble()
                Bitmap.createScaledBitmap(originalBitmap, maxWidth, (maxWidth * aspectRatio).toInt(), true)
            } else {
                originalBitmap
            }

            val file = File(context.filesDir, fileName)
            val out = FileOutputStream(file)
            resizedBitmap.compress(Bitmap.CompressFormat.JPEG, 85, out)
            out.flush()
            out.close()
            file.absolutePath
        } catch (e: Exception) {
            null
        }
    }
}
