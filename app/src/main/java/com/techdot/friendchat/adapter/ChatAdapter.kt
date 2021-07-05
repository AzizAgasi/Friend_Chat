package com.techdot.friendchat.adapter

import android.util.Log
import android.view.LayoutInflater
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
import com.techdot.friendchat.model.ChatMessage

class ChatAdapter(
    private val options: FirebaseRecyclerOptions<ChatMessage>
): FirebaseRecyclerAdapter<ChatMessage, RecyclerView.ViewHolder>(options) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        val view = inflater.inflate(R.layout.message, parent, false)
        val binding = MessageBinding.bind(view)
        return MessageViewHolder(binding)
    }

    override fun onBindViewHolder(
        holder: RecyclerView.ViewHolder,
        position: Int,
        model: ChatMessage
    ) {
        (holder as MessageViewHolder).bind(model)
    }

    inner class MessageViewHolder(private val binding: MessageBinding):
        RecyclerView.ViewHolder(binding.root) {

            fun bind(item: ChatMessage) {
                binding.message.text = item.text
                binding.username.text = item.name?: ANONYMOUS

                if (item.photoUrl != null) {
                    loadImageIntoView(binding.profilePic, item.photoUrl!!)
                } else {
                    binding.profilePic.setImageResource(R.drawable.user)
                }
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

    companion object {
        const val TAG = "MessageAdapter"
    }
}