package com.example.yourway.ui

import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import java.text.NumberFormat
import java.util.Locale

object NeonUi {
    const val BLACK = "#050706"
    const val SURFACE = "#111612"
    const val SURFACE_ALT = "#192019"
    const val NEON = "#39FF88"
    const val CYAN = "#6CE6FF"
    const val TEXT = "#F2FFF7"
    const val MUTED = "#A9B9AD"
    const val WARNING = "#FFD166"
    const val ERROR = "#FF5C7A"

    fun dp(context: Context, value: Int): Int = (value * context.resources.displayMetrics.density).toInt()

    fun root(context: Context, orientation: Int = LinearLayout.VERTICAL): LinearLayout {
        return LinearLayout(context).apply {
            this.orientation = orientation
            setBackgroundColor(Color.parseColor(BLACK))
            setPadding(dp(context, 20), dp(context, 20), dp(context, 20), dp(context, 16))
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
        }
    }

    fun card(context: Context, padding: Int = 16): LinearLayout {
        return LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(context, padding), dp(context, padding), dp(context, padding), dp(context, padding))
            background = glassDrawable(context)
            elevation = dp(context, 8).toFloat()
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply {
                setMargins(0, dp(context, 8), 0, dp(context, 8))
            }
        }
    }

    fun glassDrawable(context: Context, strokeColor: String = NEON): GradientDrawable {
        return GradientDrawable(GradientDrawable.Orientation.TL_BR, intArrayOf(
            Color.argb(220, 17, 22, 18),
            Color.argb(188, 25, 32, 25)
        )).apply {
            cornerRadius = dp(context, 8).toFloat()
            setStroke(dp(context, 1), Color.parseColor(strokeColor))
        }
    }

    fun paintingDrawable(context: Context, index: Int): GradientDrawable {
        val palettes = arrayOf(
            intArrayOf(Color.parseColor("#13381F"), Color.parseColor("#39FF88")),
            intArrayOf(Color.parseColor("#203617"), Color.parseColor("#FFD166")),
            intArrayOf(Color.parseColor("#142B3A"), Color.parseColor("#6CE6FF")),
            intArrayOf(Color.parseColor("#352314"), Color.parseColor("#FFB84D"))
        )
        return GradientDrawable(GradientDrawable.Orientation.TL_BR, palettes[index % palettes.size]).apply {
            cornerRadius = dp(context, 8).toFloat()
        }
    }

    fun title(context: Context, text: String, sizeSp: Float = 26f): TextView {
        return TextView(context).apply {
            this.text = text
            textSize = sizeSp
            setTextColor(Color.parseColor(TEXT))
            typeface = Typeface.DEFAULT_BOLD
            includeFontPadding = false
        }
    }

    fun label(context: Context, text: String, sizeSp: Float = 14f, color: String = MUTED): TextView {
        return TextView(context).apply {
            this.text = text
            textSize = sizeSp
            setTextColor(Color.parseColor(color))
            includeFontPadding = true
        }
    }

    fun metric(context: Context, metricLabel: String, value: String): LinearLayout {
        return LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_VERTICAL
            addView(label(context, metricLabel, 12f))
            addView(title(context, value, 20f))
        }
    }

    fun button(context: Context, text: String, filled: Boolean = true): MaterialButton {
        return MaterialButton(context).apply {
            this.text = text
            isAllCaps = false
            cornerRadius = dp(context, 8)
            minHeight = dp(context, 48)
            insetTop = 0
            insetBottom = 0
            setTextColor(if (filled) Color.parseColor(BLACK) else Color.parseColor(NEON))
            backgroundTintList = android.content.res.ColorStateList.valueOf(
                if (filled) Color.parseColor(NEON) else Color.TRANSPARENT
            )
            strokeColor = android.content.res.ColorStateList.valueOf(Color.parseColor(NEON))
            strokeWidth = if (filled) 0 else dp(context, 1)
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                dp(context, 52)
            ).apply { setMargins(0, dp(context, 8), 0, dp(context, 8)) }
        }
    }

    fun compactButton(context: Context, text: String): MaterialButton {
        return button(context, text).apply {
            minWidth = dp(context, 42)
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                dp(context, 42)
            ).apply { setMargins(dp(context, 4), 0, dp(context, 4), 0) }
        }
    }

    fun input(context: Context, hint: String, inputType: Int): Pair<TextInputLayout, TextInputEditText> {
        val editText = TextInputEditText(context).apply {
            this.inputType = inputType
            setTextColor(Color.parseColor(TEXT))
            setHintTextColor(Color.parseColor(MUTED))
        }
        val layout = TextInputLayout(context).apply {
            this.hint = hint
            boxBackgroundColor = Color.parseColor(SURFACE)
            boxStrokeColor = Color.parseColor(NEON)
            hintTextColor = android.content.res.ColorStateList.valueOf(Color.parseColor(MUTED))
            setBoxCornerRadii(dp(context, 8).toFloat(), dp(context, 8).toFloat(), dp(context, 8).toFloat(), dp(context, 8).toFloat())
            addView(editText)
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply { setMargins(0, dp(context, 6), 0, dp(context, 6)) }
        }
        return layout to editText
    }

    fun divider(context: Context): View {
        return View(context).apply {
            setBackgroundColor(Color.argb(70, 57, 255, 136))
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                dp(context, 1)
            ).apply { setMargins(0, dp(context, 12), 0, dp(context, 12)) }
        }
    }

    fun currency(amount: Double): String {
        return NumberFormat.getCurrencyInstance(Locale("en", "IN")).format(amount)
    }
}
