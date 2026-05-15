package com.example.yourway.ui

import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.yourway.model.Investment

class InvestmentAdapter : RecyclerView.Adapter<InvestmentAdapter.InvestmentViewHolder>() {
    private val items = mutableListOf<Investment>()

    fun submitList(next: List<Investment>) {
        items.clear()
        items.addAll(next)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): InvestmentViewHolder {
        val context = parent.context
        val card = NeonUi.card(context, padding = 14)
        val title = NeonUi.title(context, "", 18f)
        val meta = NeonUi.label(context, "")
        val returns = NeonUi.label(context, "", 14f, NeonUi.NEON)
        card.addView(title)
        card.addView(meta)
        card.addView(returns)
        return InvestmentViewHolder(card, title, meta, returns)
    }

    override fun getItemCount(): Int = items.size

    override fun onBindViewHolder(holder: InvestmentViewHolder, position: Int) {
        val item = items[position]
        holder.title.text = "${item.planName} x${item.quantity}"
        holder.meta.text = "${item.remainingDays} days remaining | Daily ${NeonUi.currency(item.dailyProfit * item.quantity)}"
        holder.returns.text = "Projected total returns ${NeonUi.currency(item.totalReturns)}"
    }

    class InvestmentViewHolder(
        root: LinearLayout,
        val title: TextView,
        val meta: TextView,
        val returns: TextView
    ) : RecyclerView.ViewHolder(root)
}
