package com.example.yourway.ui

import android.graphics.Color
import android.view.Gravity
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.yourway.model.TransactionItem
import com.example.yourway.model.TransactionType
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class TransactionAdapter : RecyclerView.Adapter<TransactionAdapter.TransactionViewHolder>() {
    private val items = mutableListOf<TransactionItem>()
    private val formatter = SimpleDateFormat("dd MMM, hh:mm a", Locale.US)

    fun submitList(next: List<TransactionItem>) {
        items.clear()
        items.addAll(next)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TransactionViewHolder {
        val context = parent.context
        val row = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(0, NeonUi.dp(context, 10), 0, NeonUi.dp(context, 10))
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
        }
        val info = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
        }
        val title = NeonUi.title(context, "", 15f)
        val date = NeonUi.label(context, "", 12f)
        val amount = NeonUi.title(context, "", 15f)
        info.addView(title)
        info.addView(date)
        row.addView(info)
        row.addView(amount)
        return TransactionViewHolder(row, title, date, amount)
    }

    override fun getItemCount(): Int = items.size

    override fun onBindViewHolder(holder: TransactionViewHolder, position: Int) {
        val item = items[position]
        val sign = if (item.type == TransactionType.DEBIT || item.type == TransactionType.WITHDRAWAL) "-" else "+"
        val color = if (sign == "+") NeonUi.NEON else NeonUi.ERROR
        holder.title.text = item.title
        holder.date.text = listOfNotNull(
            formatter.format(Date(item.createdAt)),
            item.paymentMethod,
            item.referenceId,
            item.status
        ).joinToString(" | ")
        holder.amount.text = "$sign${NeonUi.currency(item.amount)}"
        holder.amount.setTextColor(Color.parseColor(color))
    }

    class TransactionViewHolder(
        root: LinearLayout,
        val title: TextView,
        val date: TextView,
        val amount: TextView
    ) : RecyclerView.ViewHolder(root)
}
