package com.github.kr328.clash.design.adapter

import android.content.res.ColorStateList
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.github.kr328.clash.design.R
import com.github.kr328.clash.design.model.SitenavItem
import com.github.kr328.clash.design.util.resolveThemedColor

class SitenavAdapter(
    private val items: List<SitenavItem>,
    private val onClicked: (SitenavItem) -> Unit
) : RecyclerView.Adapter<SitenavAdapter.Holder>() {

    class Holder(view: View) : RecyclerView.ViewHolder(view) {
        val avatar: TextView? = view.findViewById(R.id.sitenav_item_avatar)
        val name: TextView = view.findViewById(R.id.sitenav_item_name)
    }

    private val colors = intArrayOf(
        0xFF5C6BC0.toInt(), // Pastel Indigo
        0xFF26A69A.toInt(), // Pastel Teal
        0xFFAB47BC.toInt(), // Pastel Deep Purple
        0xFFEC407A.toInt(), // Pastel Pink
        0xFFFF7043.toInt(), // Pastel Deep Orange
        0xFF29B6F6.toInt(), // Pastel Light Blue
        0xFF66BB6A.toInt(), // Pastel Green
        0xFF7E57C2.toInt(), // Pastel Purple
        0xFF26C6DA.toInt(), // Pastel Cyan
        0xFF78909C.toInt()  // Pastel Blue Grey
    )

    companion object {
        private const val VIEW_TYPE_GRID = 0
        private const val VIEW_TYPE_SINGLE = 1
    }

    override fun getItemViewType(position: Int): Int {
        return if (items[position].span == 12) VIEW_TYPE_SINGLE else VIEW_TYPE_GRID
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): Holder {
        val layout = if (viewType == VIEW_TYPE_SINGLE) {
            R.layout.adapter_sitenav_item_single
        } else {
            R.layout.adapter_sitenav_item
        }
        val view = LayoutInflater.from(parent.context).inflate(layout, parent, false)
        return Holder(view)
    }

    override fun onBindViewHolder(holder: Holder, position: Int) {
        val item = items[position]
        holder.name.text = item.name
        
        val context = holder.itemView.context
        val card = holder.itemView as com.google.android.material.card.MaterialCardView
        val density = context.resources.displayMetrics.density

        val isWebview = item.openMode == "webview"

        if (item.span == 12) {
            if (isWebview) {
                card.setCardBackgroundColor(0xFF2E7D32.toInt())
                card.strokeWidth = 0
                card.cardElevation = 3 * density
                holder.name.setTextColor(0xFFFFFFFF.toInt())
            } else {
                card.setCardBackgroundColor(0x00000000)
                card.strokeWidth = (1.0f * density).toInt()
                card.strokeColor = 0x4D2E7D32.toInt()
                card.cardElevation = 0f
                holder.name.setTextColor(0xFF2E7D32.toInt())
            }
        } else {
            if (isWebview) {
                val surfaceColor = context.resolveThemedColor(com.google.android.material.R.attr.colorSurface)
                card.setCardBackgroundColor(surfaceColor)
                card.strokeWidth = 0
                card.cardElevation = 2 * density
                val primaryText = context.resolveThemedColor(android.R.attr.textColorPrimary)
                holder.name.setTextColor(primaryText)
            } else {
                card.setCardBackgroundColor(0x00000000)
                val primaryColor = context.resolveThemedColor(com.google.android.material.R.attr.colorPrimary)
                card.strokeWidth = (1.0f * density).toInt()
                card.strokeColor = (primaryColor and 0x00FFFFFF) or 0x4D000000
                card.cardElevation = 0f
                holder.name.setTextColor(primaryColor)
            }
        }

        holder.avatar?.let { avatar ->
            avatar.text = item.name.firstOrNull()?.uppercase() ?: ""
            val color = colors[Math.abs(item.name.hashCode()) % colors.size]
            avatar.backgroundTintList = ColorStateList.valueOf(color)
        }

        holder.itemView.setOnClickListener {
            onClicked(item)
        }
    }

    override fun getItemCount(): Int = items.size
}

