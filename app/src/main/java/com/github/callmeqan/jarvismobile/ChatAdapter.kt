package com.github.callmeqan.jarvismobile
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
class ChatAdapter(private val messages: MutableList<String>) : RecyclerView.Adapter<ChatAdapter.ChatViewHolder>() {

    // ViewHolder represents each row in the RecyclerView
    class ChatViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val textView: TextView = view.findViewById(R.id.chat_message)  // This will be the text view in each row
    }

    // Create new views (invoked by the layout manager)
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ChatViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.chat_item, parent, false)
        return ChatViewHolder(view)
    }

    // Bind data to the views
    override fun onBindViewHolder(holder: ChatViewHolder, position: Int) {
        holder.textView.text = messages[position]  // Set the message text to the TextView
    }

    // Return the size of the data
    override fun getItemCount(): Int {
        return messages.size  // Return the number of messages
    }

    // Method to add a new message
    fun addMessage(message: String) {
        messages.add(message)  // Add new message to the list
        notifyItemInserted(messages.size - 1)  // Notify the adapter that a new item has been inserted
    }
}
