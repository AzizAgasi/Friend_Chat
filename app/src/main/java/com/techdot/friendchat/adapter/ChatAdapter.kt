package com.techdot.friendchat.adapter

import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.firebase.ui.database.FirebaseRecyclerAdapter
import com.firebase.ui.database.FirebaseRecyclerOptions
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.ktx.storage
import com.techdot.friendchat.MainActivity.Companion.ANONYMOUS
import com.techdot.friendchat.R
import com.techdot.friendchat.databinding.MessageBinding
import com.techdot.friendchat.databinding.MessageSentBinding
import com.techdot.friendchat.model.ChatMessage

class ChatAdapter(
    private val options: FirebaseRecyclerOptions<ChatMessage>,
    private val currentUserName: String?
): FirebaseRecyclerAdapter<ChatMessage, RecyclerView.ViewHolder>(options) {


    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        if (viewType == VIEW_TYPE_MESSAGE_RECEIVED) {
            val view = inflater.inflate(R.layout.message, parent, false)
            val binding = MessageBinding.bind(view)
            return MessageViewHolder(binding)
        } else {
            val view = inflater.inflate(R.layout.message_sent, parent, false)
            val binding = MessageSentBinding.bind(view)
            return MessageSentViewHolder(binding)
        }
    }

    override fun onBindViewHolder(
        holder: RecyclerView.ViewHolder,
        position: Int,
        model: ChatMessage
    ) {
        val message: ChatMessage = getItem(position)
        if (message.name != currentUserName) {
            (holder as MessageViewHolder).bind(model)
        } else {
            (holder as MessageSentViewHolder).bind(model)
        }
    }

    inner class MessageViewHolder(
        private val binding: MessageBinding):
        RecyclerView.ViewHolder(binding.root) {

            fun bind(item: ChatMessage) {
                binding.messageTextView.text = item.text
                binding.messengerTextView.text = item.name ?: ANONYMOUS

                if (item.photoUrl != null) {
                    loadImageIntoView(binding.messengerImageView, item.photoUrl!!)
                } else {
                    binding.messengerImageView.setImageResource(R.drawable.user)
                }
            }
        }

    inner class MessageSentViewHolder(
        private val binding: MessageSentBinding):
    RecyclerView.ViewHolder(binding.root) {

        fun bind(item: ChatMessage) {
            binding.sentMessage.text = item.text
        }
    }

    private fun loadImageIntoView(view: ImageView, url:String) {
        if (url.startsWith("gs://")) {
            val storageReference = Firebase.storage.getReferenceFromUrl(url)
            storageReference.downloadUrl
                .addOnSuccessListener { uri ->
                    val downloadUrl = uri.toString()
                    Glide.with(view.context)
                        .load(downloadUrl)
                        .into(view)
                }
                .addOnFailureListener { e ->
                    Log.w(
                        TAG,
                        "Getting download url was not successful.",
                        e
                    )
                }
        } else {
            Glide.with(view.context).load(url).into(view)
        }
    }

    override fun getItemViewType(position: Int): Int {
        val message: ChatMessage = getItem(position)
        if (message.name == currentUserName) {
            return VIEW_TYPE_MESSAGE_SENT
        } else {
            return VIEW_TYPE_MESSAGE_RECEIVED
        }
    }

    companion object {
        const val TAG = "MessageAdapter"
        const val VIEW_TYPE_MESSAGE_RECEIVED = 52
        const val VIEW_TYPE_MESSAGE_SENT = 53
    }
}