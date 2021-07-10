package com.techdot.friendchat

import android.content.AbstractThreadedSyncAdapter
import android.content.Context
import android.content.Intent
import android.database.DataSetObserver
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import android.widget.ImageView
import androidx.core.view.size
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.firebase.ui.auth.AuthUI
import com.firebase.ui.database.FirebaseRecyclerOptions
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.database.ktx.database
import com.google.firebase.iid.FirebaseInstanceIdReceiver
import com.google.firebase.iid.internal.FirebaseInstanceIdInternal
import com.google.firebase.installations.FirebaseInstallations
import com.google.firebase.ktx.Firebase
import com.google.firebase.messaging.FirebaseMessaging
import com.google.gson.Gson
import com.techdot.friendchat.adapter.ChatAdapter
import com.techdot.friendchat.databinding.ActivityMainBinding
import com.techdot.friendchat.model.ChatMessage
import com.techdot.friendchat.notification.NotificationData
import com.techdot.friendchat.notification.NotificationUtils
import com.techdot.friendchat.notification.PushNotification
import com.techdot.friendchat.notification.RetrofitInstance
import com.techdot.friendchat.signIn.SignInActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlin.properties.Delegates

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding

    private lateinit var auth: FirebaseAuth
    private lateinit var database: FirebaseDatabase

    private lateinit var manager: LinearLayoutManager
    private lateinit var adapter: ChatAdapter

    val TOPIC = "/topics/messageTopic"

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

        NotificationUtils.sharedPref = getSharedPreferences("sharedPref", Context.MODE_PRIVATE)
        FirebaseInstallations.getInstance().id.addOnSuccessListener {
            NotificationUtils.token = it
        }
        FirebaseMessaging.getInstance().subscribeToTopic(TOPIC)

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
            PushNotification (
                NotificationData(
                    message.name,
                    "Message from ${message.name}",
                    message.text
                ), TOPIC
            ).also {
                sendNotification(it)
            }
            hideKeyboard(binding.messageInput)
        }

    }

    override fun onStart() {
        super.onStart()
        if (auth.currentUser == null) {
            startActivity(Intent(this, SignInActivity::class.java))
            finish()
            return
        }
        adapter.registerAdapterDataObserver(
            ScrollToBottomObserver(binding.messageRecyclerView, adapter, manager)
        )
    }

    public override fun onPause() {
        adapter.stopListening()
        super.onPause()
    }

    public override fun onResume() {
        super.onResume()
        adapter.startListening()
    }

    override fun onStop() {
        super.onStop()
        startService(Intent(this, NotificationUtils::class.java))
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
        const val TAG = "MainActivity"
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

    private fun sendNotification(notification: PushNotification) =
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val response = RetrofitInstance.api.postNotification(notification)
                if(response.isSuccessful) {
                    Log.d(TAG, "Response: ${Gson().toJson(response)}")
                } else {
                    Log.e(TAG, response.errorBody().toString())
                }
            } catch(e: Exception) {
                Log.e(TAG, e.toString())
            }
        }

    private fun hideKeyboard(editText: EditText) {
        val inputMethodManager = getSystemService(Context.INPUT_METHOD_SERVICE) as
                InputMethodManager
        inputMethodManager.hideSoftInputFromWindow(editText.windowToken, 0)
    }
}