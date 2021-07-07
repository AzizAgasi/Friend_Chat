package com.techdot.friendchat.adapter

import android.renderscript.ScriptGroup
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.firebase.ui.database.FirebaseRecyclerAdapter
import com.firebase.ui.database.FirebaseRecyclerOptions
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.ktx.storage
import com.techdot.friendchat.MainActivity.Companion.ANONYMOUS
import com.techdot.friendchat.R
import com.techdot.friendchat.databinding.MessageBinding
import com.techdot.friendchat.model.ChatMessage

class ChatAdapter(
    private val options: FirebaseRecyclerOptions<ChatMessage>,
    private val currentUserName: String?
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

    inner class MessageViewHolder(
        private val binding: MessageBinding):
        RecyclerView.ViewHolder(binding.root) {

            fun bind(item: ChatMessage) {
                if (item.name != currentUserName) {
                    binding.messageTextView.text = item.text
                    binding.messengerTextView.text = item.name ?: ANONYMOUS

                    if (item.photoUrl != null) {
                        loadImageIntoView(binding.messengerImageView, item.photoUrl!!)
                    } else {
                        binding.messengerImageView.setImageResource(R.drawable.user)
                    }
                } else {
                    binding.messageTextView.text = item.text
                    binding.messengerTextView.visibility = View.GONE
                    binding.messengerImageView.visibility = View.GONE
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