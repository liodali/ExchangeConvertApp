package dali.hamza.echangecurrencyapp.widgets

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import kotlin.math.cos
import kotlin.math.sin

/**
 * Renders the widget background as a **circular wave blob** — the shape
 * from `ui-design.pen` frame "Widget — Market Rate (wave blob)": a filled
 * wavy circle `r = R + A·sin(waves·θ)`, drawn as two layers (palette-tinted
 * halo + dark surface body).
 *
 * RemoteViews cannot draw arbitrary shapes, so the blob is rendered as a
 * bitmap at placement/refresh time and served via ImageProvider(Bitmap).
 * 256 px keeps it far under the ~1 MB Binder transaction limit.
 */
object WaveBlobRenderer {

    /**
     * Sketch coordinates: 158×158 canvas. M3-expressive soft scallop —
     * **7 lobes radiating from the center** (`sin(7θ)` centers a crest at
     * top). Rendered larger + softer (R 64/68, A 3.5) so the silhouette
     * reads clearly CIRCULAR, and the Glance Image uses ContentScale.Fit —
     * never stretched into the (often non-square) widget bounds.
     */
    private const val SKETCH_SIZE = 158f
    private const val BODY_RADIUS = 64f
    private const val HALO_RADIUS = 68f
    private const val AMPLITUDE = 3.5f
    private const val WAVES = 7

    /** Body fill — the Sovereign dark surface from the design tokens. */
    private const val BODY_COLOR = 0xFF2A2A2A.toInt()

    /** 20% alpha halo, matching the palette `accentSoft` (0x33) tokens. */
    private const val HALO_ALPHA = 0x33

    fun render(sizePx: Int = 256, accentArgb: Int): Bitmap {
        val bitmap = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val scale = sizePx / SKETCH_SIZE
        val center = sizePx / 2f

        val haloPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = (accentArgb and 0x00FFFFFF) or (HALO_ALPHA shl 24)
        }
        val bodyPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = BODY_COLOR }

        canvas.drawPath(wavePath(center, scale, HALO_RADIUS), haloPaint)
        canvas.drawPath(wavePath(center, scale, BODY_RADIUS), bodyPaint)
        return bitmap
    }

    private fun wavePath(center: Float, scale: Float, radius: Float): Path = Path().apply {
        for (degrees in 0..359 step 2) {
            val theta = Math.toRadians(degrees.toDouble())
            val r = (radius + AMPLITUDE * sin(WAVES * theta)).toFloat() * scale
            val x = center + r * cos(theta).toFloat()
            val y = center + r * sin(theta).toFloat()
            if (degrees == 0) moveTo(x, y) else lineTo(x, y)
        }
        close()
    }
}

/** Widget tap deep link: open the app on the HISTORY page for this pair. */
internal fun pairHistoryIntent(
    context: Context,
    base: String,
    quote: String,
): Intent = Intent(context, dali.hamza.echangecurrencyapp.ui.MainActivity::class.java)
    .putExtra(dali.hamza.shared.ui.DeepLinkNav.EXTRA_ROUTE, "history")
    .putExtra(dali.hamza.shared.ui.DeepLinkNav.EXTRA_PAIR_BASE, base)
    .putExtra(dali.hamza.shared.ui.DeepLinkNav.EXTRA_PAIR_QUOTE, quote)
