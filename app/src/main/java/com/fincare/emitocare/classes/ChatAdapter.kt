package com.fincare.emitocare

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.fincare.emitocare.R
import java.text.SimpleDateFormat
import java.util.*

class ChatAdapter(private val messages: List<Message>) : RecyclerView.Adapter<ChatAdapter.ChatViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ChatViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(
                if (viewType == 1) R.layout.item_user_message
                else R.layout.item_bot_message,
                parent, false
            )
        return ChatViewHolder(view)
    }

    override fun onBindViewHolder(holder: ChatViewHolder, position: Int) {
        val message = messages[position]
        holder.bind(message)
    }

    override fun getItemCount(): Int = messages.size

    override fun getItemViewType(position: Int): Int {
        return if (messages[position].isUser) 1 else 0
    }

    inner class ChatViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val messageText: TextView = itemView.findViewById(R.id.messageText)
        private val timestampText: TextView = itemView.findViewById(R.id.timestampText)

        fun bind(message: Message) {
            messageText.text = message.text
            val time = message.timestamp ?: System.currentTimeMillis()
            val formattedTime = SimpleDateFormat("hh:mm a", Locale.getDefault()).format(Date(time))
            timestampText.text = formattedTime
        }
    }
}
