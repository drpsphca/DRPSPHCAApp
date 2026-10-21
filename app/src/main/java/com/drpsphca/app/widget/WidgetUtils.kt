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
            connection.connectTimeout = 5000
            connection.readTimeout = 5000
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
        } catch (e: java.net.SocketException) {
            null
        } catch (e: java.io.IOException) {
            null
        } catch (e: Exception) {
            null
        }
    }

    fun getCircularBitmap(sourceBitmap: Bitmap): Bitmap {
        val size = sourceBitmap.width.coerceAtMost(sourceBitmap.height)
        val output = Bitmap.createBitmap(size, size, Bitmap.Config.RGB_565)
        val canvas = Canvas(output)
        val paint = Paint().apply {
            isAntiAlias = true
        }
        val rect = android.graphics.Rect(0, 0, size, size)
        val rectF = android.graphics.RectF(rect)
        canvas.drawARGB(0, 0, 0, 0)
        canvas.drawOval(rectF, paint)
        paint.xfermode = android.graphics.PorterDuffXfermode(android.graphics.PorterDuff.Mode.SRC_IN)
        
        val left = (sourceBitmap.width - size) / 2
        val top = (sourceBitmap.height - size) / 2
        val srcRect = android.graphics.Rect(left, top, left + size, top + size)
        canvas.drawBitmap(sourceBitmap, srcRect, rect, paint)
        return output
    }

    fun downloadAndSaveCircularImage(context: Context, url: String, fileName: String): String? {
        return try {
            val connection = URL(url).openConnection()
            connection.connectTimeout = 3000
            connection.readTimeout = 3000
            connection.doInput = true
            connection.connect()
            val input = connection.getInputStream()

            val options = BitmapFactory.Options().apply {
                inPreferredConfig = Bitmap.Config.RGB_565
            }
            val originalBitmap = BitmapFactory.decodeStream(input, null, options)
            if (originalBitmap == null) return null

            // Resize to max 48x48 pixels
            val maxSize = 48
            val width = originalBitmap.width
            val height = originalBitmap.height
            val bitmapToProcess = if (width > maxSize || height > maxSize) {
                val ratio = width.toFloat() / height.toFloat()
                val targetWidth = if (ratio > 1) maxSize else (maxSize * ratio).toInt()
                val targetHeight = if (ratio > 1) (maxSize / ratio).toInt() else maxSize
                Bitmap.createScaledBitmap(originalBitmap, targetWidth, targetHeight, true)
            } else {
                originalBitmap
            }

            val circularBitmap = getCircularBitmap(bitmapToProcess)
            val file = File(context.filesDir, fileName)
            val out = FileOutputStream(file)
            circularBitmap.compress(Bitmap.CompressFormat.JPEG, 60, out)
            out.flush()
            out.close()

            if (originalBitmap !== bitmapToProcess) {
                originalBitmap.recycle()
            }
            if (bitmapToProcess !== circularBitmap) {
                bitmapToProcess.recycle()
            }

            file.absolutePath
        } catch (e: java.net.SocketException) {
            null
        } catch (e: java.io.IOException) {
            null
        } catch (e: Exception) {
            null
        }
    }
}
