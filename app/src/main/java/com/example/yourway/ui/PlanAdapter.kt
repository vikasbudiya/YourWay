package com.example.yourway.ui

import android.graphics.Color
import android.view.Gravity
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.yourway.model.PaintingPlan
import com.example.yourway.model.paintingPlans

class PlanAdapter(
    private val onBuy: (PaintingPlan, Int) -> Unit
) : RecyclerView.Adapter<PlanAdapter.PlanViewHolder>() {
    private val quantities = mutableMapOf<String, Int>()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PlanViewHolder {
        val context = parent.context
        val card = NeonUi.card(context)

        val image = ImageView(context).apply {
            scaleType = ImageView.ScaleType.CENTER_CROP
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                NeonUi.dp(context, 116)
            )
        }

        val title = NeonUi.title(context, "", 20f)
        val price = NeonUi.label(context, "", 14f, NeonUi.NEON)
        val profit = NeonUi.label(context, "", 14f)
        val days = NeonUi.label(context, "", 13f)

        val quantityRow = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        val minus = NeonUi.compactButton(context, "-")
        val quantity = NeonUi.title(context, "1", 18f).apply {
            gravity = Gravity.CENTER
            layoutParams = LinearLayout.LayoutParams(NeonUi.dp(context, 44), ViewGroup.LayoutParams.WRAP_CONTENT)
        }
        val plus = NeonUi.compactButton(context, "+")
        quantityRow.addView(NeonUi.label(context, "Quantity", 13f).apply {
            layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
        })
        quantityRow.addView(minus)
        quantityRow.addView(quantity)
        quantityRow.addView(plus)

        val buy = NeonUi.button(context, "Buy painting")

        card.addView(image)
        card.addView(title)
        card.addView(price)
        card.addView(profit)
        card.addView(days)
        card.addView(quantityRow)
        card.addView(buy)

        return PlanViewHolder(card, image, title, price, profit, days, quantity, minus, plus, buy)
    }

    override fun getItemCount(): Int = paintingPlans.size

    override fun onBindViewHolder(holder: PlanViewHolder, position: Int) {
        val plan = paintingPlans[position]
        val context = holder.itemView.context
        val quantity = quantities[plan.id] ?: 1

        if (plan.imageUrl.isBlank()) {
            holder.image.setImageDrawable(null)
            holder.image.background = NeonUi.paintingDrawable(context, position)
        } else {
            Glide.with(holder.image).load(plan.imageUrl).into(holder.image)
        }

        holder.title.text = plan.name
        holder.price.text = "Price ${NeonUi.currency(plan.price)}"
        holder.profit.text = "Daily profit ${NeonUi.currency(plan.dailyProfit)}"
        holder.days.text = "${plan.durationDays} days | Total ${NeonUi.currency(plan.dailyProfit * plan.durationDays * quantity)}"
        holder.quantity.text = quantity.toString()

        holder.minus.setOnClickListener {
            quantities[plan.id] = (quantity - 1).coerceAtLeast(1)
            val adapterPosition = holder.bindingAdapterPosition
            if (adapterPosition != RecyclerView.NO_POSITION) notifyItemChanged(adapterPosition)
        }
        holder.plus.setOnClickListener {
            quantities[plan.id] = (quantity + 1).coerceAtMost(20)
            val adapterPosition = holder.bindingAdapterPosition
            if (adapterPosition != RecyclerView.NO_POSITION) notifyItemChanged(adapterPosition)
        }
        holder.buy.setOnClickListener { onBuy(plan, quantities[plan.id] ?: 1) }
    }

    class PlanViewHolder(
        root: LinearLayout,
        val image: ImageView,
        val title: TextView,
        val price: TextView,
        val profit: TextView,
        val days: TextView,
        val quantity: TextView,
        val minus: TextView,
        val plus: TextView,
        val buy: TextView
    ) : RecyclerView.ViewHolder(root)
}
