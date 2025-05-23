package com.fincare.emitocare.Fragments

import android.os.Bundle
import android.util.Log
import android.view.*
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.fincare.emitocare.R
import com.fincare.emitocare.Recommendation
import com.fincare.emitocare.RecommendationAdapter
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query

class TravelFragment : Fragment() {

    private lateinit var recyclerView: RecyclerView
    private val recommendations = mutableListOf<Recommendation>()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.fragment_travel, container, false)
        recyclerView = view.findViewById(R.id.travelRecyclerView)
        recyclerView.layoutManager = LinearLayoutManager(context)
        recyclerView.adapter = RecommendationAdapter(recommendations)
        fetchTravelRecommendations()
        return view
    }

    private fun fetchTravelRecommendations() {
        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return
        val db = FirebaseFirestore.getInstance()
        db.collection("users").document(userId).collection("recommendations")
            .orderBy("timestamp", Query.Direction.DESCENDING).limit(1).get()
            .addOnSuccessListener { documents ->
                for (document in documents) {
                    val text = document.getString("travel") ?: ""
                    val imageUrl = document.getString("travel_image_url") ?: ""
                    recommendations.clear()
                    recommendations.add(Recommendation(text, imageUrl))
                    recyclerView.adapter?.notifyDataSetChanged()
                }
            }
            .addOnFailureListener {
                Log.e("TravelFragment", "Error fetching data: ${it.message}")
            }
    }
}
