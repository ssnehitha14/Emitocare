package com.fincare.emitocare.Fragments

import android.Manifest
import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.telephony.SmsManager
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.fincare.emitocare.ChatAdapter
import com.fincare.emitocare.ChatGPTService
import com.fincare.emitocare.Message
import com.fincare.emitocare.R
import com.fincare.emitocare.UnsplashService
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.json.JSONObject

class ChatFragment : Fragment() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var editTextMessage: EditText
    private lateinit var buttonSend: Button
    private lateinit var chatAdapter: ChatAdapter
    private val messagesList = mutableListOf<Message>()

    private var initialMood: String = ""
    private var lastDetectedMood: String = ""
    private var friendPhoneNumber: String = ""
    private var moodCheckJob: Job? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_chat, container, false)

        recyclerView = view.findViewById(R.id.recyclerViewChat)
        editTextMessage = view.findViewById(R.id.editTextMessage)
        buttonSend = view.findViewById(R.id.buttonSend)

        chatAdapter = ChatAdapter(messagesList)
        recyclerView.adapter = chatAdapter
        recyclerView.layoutManager = LinearLayoutManager(requireContext())

        loadMessagesFromFirestore()
        fetchFriendDetails()
        requestSmsPermission()

        buttonSend.setOnClickListener {
            val userMessage = editTextMessage.text.toString().trim()
            if (userMessage.isNotEmpty()) {
                addMessage(userMessage, true)
                editTextMessage.text.clear()

                ChatGPTService.getChatResponse(userMessage, requireContext()) { responseText, recommendations ->
                    requireActivity().runOnUiThread {
                        addMessage(responseText, false)

                        val detectedMood = detectMoodFromResponse(responseText)
                        if (initialMood.isEmpty()) initialMood = detectedMood
                        lastDetectedMood = detectedMood

                        scheduleMoodConsistencyCheck()

                        if (recommendations.length() > 0) {
                            saveRecommendationsToFirestore(detectedMood, recommendations)
                        }
                    }
                }
            }
        }

        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        scheduleMoodConsistencyCheck()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        moodCheckJob?.cancel()
    }

    private fun addMessage(message: String, isUser: Boolean) {
        val timestamp = System.currentTimeMillis()
        val messageObj = Message(
            text = message,
            sender = if (isUser) "user" else "bot",
            timestamp = timestamp,
            isUser = isUser
        )

        messagesList.add(messageObj)
        chatAdapter.notifyItemInserted(messagesList.size - 1)
        recyclerView.scrollToPosition(messagesList.size - 1)

        saveMessageToFirestore(messageObj)
    }

    private fun saveMessageToFirestore(message: Message) {
        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return
        val db = FirebaseFirestore.getInstance()
        db.collection("users").document(userId).collection("chats").add(message)
    }

    private fun loadMessagesFromFirestore() {
        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return
        val db = FirebaseFirestore.getInstance()
        db.collection("users").document(userId).collection("chats")
            .orderBy("timestamp", Query.Direction.ASCENDING).get()
            .addOnSuccessListener { documents ->
                messagesList.clear()
                documents.forEach { messagesList.add(it.toObject(Message::class.java)) }
                chatAdapter.notifyDataSetChanged()
                recyclerView.scrollToPosition(messagesList.size - 1)
            }
    }

    private fun detectMoodFromResponse(response: String): String = when {
        response.contains("sad", ignoreCase = true) -> "Sad"
        response.contains("stressed", ignoreCase = true) -> "Stressed"
        response.contains("anxious", ignoreCase = true) -> "Anxious"
        response.contains("depressed", ignoreCase = true) -> "Depressed"
        response.contains("happy", ignoreCase = true) -> "Happy"
        else -> "Neutral"
    }

    private fun saveRecommendationsToFirestore(mood: String, recommendations: JSONObject) {
        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return
        val db = FirebaseFirestore.getInstance()
        val recRef = db.collection("users").document(userId).collection("recommendations")

        val data = hashMapOf<String, Any>(
            "timestamp" to System.currentTimeMillis(),
            "mood" to mood
        )

        val keys = recommendations.keys().asSequence().toList()
        var completed = 0

        for (key in keys) {
            val value = recommendations.getString(key)

            // Store 'musicAudioUrl' directly, no image needed
            if (key == "musicAudioUrl") {
                data["musicAudioUrl"] = value
                completed++
                if (completed == keys.size) {
                    recRef.add(data)
                        .addOnSuccessListener { Log.d("ChatFragment", "Recommendations saved.") }
                        .addOnFailureListener { Log.e("ChatFragment", it.message ?: "") }
                }
            } else {
                data[key] = value
                // Generate image for other categories
                UnsplashService.fetchImageUrl(value) { imageUrl ->
                    if (imageUrl.isNotEmpty()) {
                        data["${key}_image_url"] = imageUrl
                    }
                    completed++
                    if (completed == keys.size) {
                        recRef.add(data)
                            .addOnSuccessListener { Log.d("ChatFragment", "Recommendations saved.") }
                            .addOnFailureListener { Log.e("ChatFragment", it.message ?: "") }
                    }
                }
            }
        }
    }


    private fun fetchFriendDetails() {
        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return
        FirebaseFirestore.getInstance()
            .collection("users").document(userId)
            .get()
            .addOnSuccessListener { document ->
                // read nested friend.phone
                val friendMap = document.get("friend") as? Map<*, *>
                friendPhoneNumber = friendMap?.get("phone") as? String ?: ""
                Log.d("ChatFragment", "Loaded friendPhoneNumber='$friendPhoneNumber'")
            }
            .addOnFailureListener {
                Log.e("ChatFragment", "Error loading friend details: ${it.message}")
            }
    }


    private fun requestSmsPermission() {
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.SEND_SMS)
            != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(requireActivity(), arrayOf(Manifest.permission.SEND_SMS), 1)
        }
    }

    private fun scheduleMoodConsistencyCheck() {
        moodCheckJob?.cancel()
        moodCheckJob = viewLifecycleOwner.lifecycleScope.launch {
            if (!isAdded) return@launch

            requireContext().getSharedPreferences("emitoPrefs", Context.MODE_PRIVATE).edit()
                .putString("initialMood", lastDetectedMood)
                .putLong("startTime", System.currentTimeMillis())
                .apply()

            // 20-minute test delay (20 minutes = 1200 seconds)
            delay(20 * 60 * 1000L)

            if (!isAdded) return@launch

            val prefs = requireContext().getSharedPreferences("emitoPrefs", Context.MODE_PRIVATE)
            val savedMood = prefs.getString("initialMood", "")
            val savedTime = prefs.getLong("startTime", 0)

            if (System.currentTimeMillis() - savedTime >= 20 * 60 * 1000L
                && lastDetectedMood == savedMood
            ) {
                sendSmsToFriendAutomatically()
                showCallFriendPopup()
            }
        }
    }

    private fun sendSmsToFriendAutomatically() {
        if (!isAdded) return

        // Validate phone number
        val to = friendPhoneNumber.trim()
        if (to.isEmpty() || !to.all { it.isDigit() }) {
            Log.e("ChatFragment", "Cannot send SMS: invalid phone number=\"$friendPhoneNumber\"")
            return
        }

        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.SEND_SMS)
            == PackageManager.PERMISSION_GRANTED
        ) {
            SmsManager.getDefault().sendTextMessage(
                to, null,
                "Hey! Your friend seems to be feeling $lastDetectedMood for a while. Please reach out.",
                null, null
            )
            Log.d("ChatFragment", "SMS sent to $to")
        } else {
            Log.e("ChatFragment", "SEND_SMS permission not granted.")
        }
    }

    private fun showCallFriendPopup() {
        if (!isAdded) return
        AlertDialog.Builder(requireContext())
            .setTitle("Checking in")
            .setMessage("You've been feeling the same way for a while. Call your friend?")
            .setPositiveButton("Call Friend") { _, _ ->
                startActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:$friendPhoneNumber")))
            }
            .setNegativeButton("Not now", null)
            .show()
    }
}
