package com.fincare.emitocare.Fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.fincare.emitocare.R
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query

class StoriesFragment : Fragment() {

    private lateinit var storyText: TextView

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_stories, container, false)
        storyText = view.findViewById(R.id.storyText)
        loadStoryRecommendation()
        return view
    }

    private fun loadStoryRecommendation() {
        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return
        val db = FirebaseFirestore.getInstance()
        val recRef = db.collection("users")
            .document(userId)
            .collection("recommendations")

        recRef
            .orderBy("timestamp", Query.Direction.ASCENDING)
            .limitToLast(1)
            .get()
            .addOnSuccessListener { docs ->
                if (!docs.isEmpty) {
                    val doc = docs.first()
                    val story = doc.getString("story")
                    story?.let { storyText.text = it }
                }
            }
    }
}
