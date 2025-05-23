package com.fincare.emitocare

import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide

class RecommendationAdapter(private val list: List<Recommendation>) :
    RecyclerView.Adapter<RecommendationAdapter.RecommendationViewHolder>() {

    class RecommendationViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val imageView: ImageView = view.findViewById(R.id.recommendationImage)
        val textView: TextView = view.findViewById(R.id.recommendationText)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecommendationViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_recommendation, parent, false)
        return RecommendationViewHolder(view)
    }

    override fun onBindViewHolder(holder: RecommendationViewHolder, position: Int) {
        val item = list[position]
        holder.textView.text = item.text

        val imageUrl = item.imageUrl
        Log.d("GlideURL", "Image URL: $imageUrl")

        if (!imageUrl.isNullOrEmpty()) {
            Glide.with(holder.itemView.context)
                .load(imageUrl)
                .placeholder(android.R.drawable.ic_menu_report_image) // placeholder while loading
                .error(android.R.drawable.ic_dialog_alert) // fallback if load fails
                .into(holder.imageView)
        } else {
            holder.imageView.setImageResource(android.R.drawable.ic_dialog_alert)
        }
    }

    override fun getItemCount(): Int = list.size
}
