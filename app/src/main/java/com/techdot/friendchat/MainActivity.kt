package com.techdot.friendchat

import android.content.AbstractThreadedSyncAdapter
import android.content.Intent
import android.database.DataSetObserver
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.widget.ImageView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.firebase.ui.database.FirebaseRecyclerOptions
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase
import com.techdot.friendchat.adapter.ChatAdapter
import com.techdot.friendchat.databinding.ActivityMainBinding
import com.techdot.friendchat.model.ChatMessage
import com.techdot.friendchat.signIn.SignInActivity
import kotlin.properties.Delegates

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding

    private lateinit var auth: FirebaseAuth
    private lateinit var database: FirebaseDatabase

    private lateinit var manager: LinearLayoutManager
    private lateinit var adapter: ChatAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        auth = Firebase.auth

        if (auth.currentUser == null) {
            startActivity(Intent(this, SignInActivity::class.java))
            finish()
            return
        }

        database = Firebase.database
        val messagesRef = database.reference.child(MESSAGES_CHILD)

        val options = FirebaseRecyclerOptions.Builder<ChatMessage>()
            .setQuery(messagesRef, ChatMessage::class.java)
            .build()

        adapter = ChatAdapter(options, auth.currentUser?.displayName)
        manager = LinearLayoutManager(this)
        // To fill the list from the bottom of the view
        manager.stackFromEnd = true
        binding.messageRecyclerView.layoutManager = manager
        binding.messageRecyclerView.adapter = adapter

        // Scroll down when a new message arrives
        adapter.registerAdapterDataObserver(
            ScrollToBottomObserver(binding.messageRecyclerView, adapter, manager)
        )

        binding.messageInput.addTextChangedListener(SendButtonObserver(binding.sendButton))

        binding.sendButton.setOnClickListener {
            val message = ChatMessage(
                binding.messageInput.text.toString(),
                getUserName(),
                getPhotoUrl()
            )
            database.reference.child(MESSAGES_CHILD).push().setValue(message)
            binding.messageInput.setText("")
        }

    }

    override fun onStart() {
        super.onStart()
        if (auth.currentUser == null) {
            startActivity(Intent(this, SignInActivity::class.java))
            finish()
            return
        }
    }

    public override fun onPause() {
        adapter.stopListening()
        super.onPause()
    }

    public override fun onResume() {
        super.onResume()
        adapter.startListening()
    }

    private fun getPhotoUrl(): String? {
        val user = auth.currentUser
        return user?.photoUrl?.toString()
    }

    private fun getUserName(): String? {
        val user = auth.currentUser
        return if (user != null) {
            user.displayName
        } else ANONYMOUS
    }

    companion object {
        const val ANONYMOUS = "Anonymous"
        const val MESSAGES_CHILD = "messages"
    }

    inner class ScrollToBottomObserver(
        private val recycler: RecyclerView,
        private val adapter: ChatAdapter,
        private val manager: LinearLayoutManager
    ): RecyclerView.AdapterDataObserver() {

        override fun onItemRangeInserted(positionStart: Int, itemCount: Int) {
            super.onItemRangeInserted(positionStart, itemCount)

            val count = adapter.itemCount
            val lastVisiblePosition = manager.findLastCompletelyVisibleItemPosition()
            // If the list is initially loading,
            // or the user is at the bottom of the list
            // scroll to the bottom of the list to show newly added messages
            val loading = lastVisiblePosition == -1
            val atBottom = positionStart >= count && lastVisiblePosition == positionStart -1

            if (loading || atBottom) {
                recycler.scrollToPosition(positionStart)
            }
        }
    }

    inner class SendButtonObserver(private val button: ImageView): TextWatcher {

        override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit

        override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
            if (s.toString().trim().isNotEmpty()) {
                button.isEnabled = true
                button.setImageResource(R.drawable.ic_baseline_send_24_enabled)
            } else {
                button.isEnabled = false
                button.setImageResource(R.drawable.ic_baseline_send_24)
            }
        }

        override fun afterTextChanged(s: Editable?) = Unit
    }
}