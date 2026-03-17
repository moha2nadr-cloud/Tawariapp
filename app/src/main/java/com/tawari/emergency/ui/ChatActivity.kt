package com.tawari.emergency.ui

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.tawari.emergency.databinding.ActivityChatBinding
import com.tawari.emergency.models.ChatMessage
import com.tawari.emergency.network.ApiClient
import com.tawari.emergency.ui.adapters.ChatAdapter
import com.tawari.emergency.utils.PrefsManager
import kotlinx.coroutines.*
import java.util.*

class ChatActivity : AppCompatActivity() {

    private lateinit var binding: ActivityChatBinding
    private lateinit var apiClient: ApiClient
    private lateinit var prefsManager: PrefsManager
    private lateinit var chatAdapter: ChatAdapter

    private var agentName: String = ""
    private var sessionId: String = ""
    private var userName: String = ""
    private var pollingJob: Job? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityChatBinding.inflate(layoutInflater)
        setContentView(binding.root)

        apiClient = ApiClient(this)
        prefsManager = PrefsManager(this)

        agentName = intent.getStringExtra("agent_name") ?: ""
        userName = prefsManager.userName.ifEmpty { "User_${UUID.randomUUID().toString().take(6)}" }
        sessionId = "Android_${agentName}_${userName}"

        if (prefsManager.userName.isEmpty()) {
            showNameDialog()
        } else {
            setupUI()
            startPolling()
        }
    }

    private fun showNameDialog() {
        val input = android.widget.EditText(this).apply {
            hint = "اسمك"
            layoutDirection = View.LAYOUT_DIRECTION_RTL
        }

        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("أدخل اسمك")
            .setView(input)
            .setCancelable(false)
            .setPositiveButton("دخول") { _, _ ->
                val name = input.text.toString().trim()
                if (name.isNotEmpty()) {
                    userName = name
                    prefsManager.userName = name
                    sessionId = "Android_${agentName}_${userName}"
                    setupUI()
                    startPolling()
                } else {
                    finish()
                }
            }
            .setNegativeButton("إلغاء") { _, _ -> finish() }
            .show()
    }

    private fun setupUI() {
        binding.agentName.text = agentName
        binding.backButton.setOnClickListener { finish() }

        chatAdapter = ChatAdapter(userName)
        binding.chatRecyclerView.apply {
            layoutManager = LinearLayoutManager(this@ChatActivity).apply {
                stackFromEnd = true
            }
            adapter = chatAdapter
        }

        binding.sendButton.setOnClickListener {
            sendMessage()
        }

        binding.messageInput.setOnEditorActionListener { _, _, _ ->
            sendMessage()
            true
        }
    }

    private fun sendMessage() {
        val text = binding.messageInput.text.toString().trim()
        if (text.isEmpty()) return

        binding.messageInput.text?.clear()

        // Add message locally immediately
        val message = ChatMessage(from = userName, text = text)
        chatAdapter.addMessage(message)
        scrollToBottom()

        // Send to server
        lifecycleScope.launch {
            val result = apiClient.sendChatMessage(
                session = sessionId,
                from = userName,
                text = text
            )

            withContext(Dispatchers.Main) {
                result.onFailure {
                    Toast.makeText(
                        this@ChatActivity,
                        "فشل إرسال الرسالة",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
    }

    private fun startPolling() {
        pollingJob = lifecycleScope.launch {
            while (isActive) {
                loadMessages()
                delay(3000) // Poll every 3 seconds
            }
        }
    }

    private suspend fun loadMessages() {
        val result = apiClient.loadChatMessages(sessionId)
        withContext(Dispatchers.Main) {
            result.onSuccess { messages ->
                if (messages.isNotEmpty()) {
                    chatAdapter.submitList(messages)
                    scrollToBottom()
                }
            }
        }
    }

    private fun scrollToBottom() {
        binding.chatRecyclerView.post {
            val itemCount = chatAdapter.itemCount
            if (itemCount > 0) {
                binding.chatRecyclerView.smoothScrollToPosition(itemCount - 1)
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        pollingJob?.cancel()
    }
}
