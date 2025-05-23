package com.fincare.emitocare.Fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.fincare.emitocare.R
import com.fincare.emitocare.HomeItem
import com.fincare.emitocare.HomeAdapter
import com.fincare.emitocare.MoodStorage
import java.text.SimpleDateFormat
import java.util.*

class HomeFragment : Fragment() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: HomeAdapter
    private lateinit var textViewMotivation: TextView

    // List of motivational quotes
    private val quotes = listOf(
        "Believe in yourself and all that you are.",
        "Every day is a new beginning. Take a deep breath and start again.",
        "Don’t stop when you’re tired. Stop when you’re done.",
        "Difficult roads often lead to beautiful destinations.",
        "Your future is created by what you do today, not tomorrow.",
        "Stay positive, work hard, and make it happen."
    )

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.fragment_home, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        textViewMotivation = view.findViewById(R.id.textViewMotivation)
        recyclerView = view.findViewById(R.id.recyclerView)

        // Set the daily motivational quote
        textViewMotivation.text = getDailyQuote()
        textViewMotivation.isSelected = true  // Enables marquee scrolling
        textViewMotivation.requestFocus()  // This ensures focus for marquee effect


        recyclerView.layoutManager = GridLayoutManager(requireContext(), 2)

        val userMood = MoodStorage.getMood(requireContext())
        val items = getRecommendations(userMood)

        adapter = HomeAdapter(items) { item ->
            findNavController().navigate(item.navAction)
        }
        recyclerView.adapter = adapter
    }

    // Function to get a different quote every day
    private fun getDailyQuote(): String {
        val dateFormat = SimpleDateFormat("yyyyMMdd", Locale.getDefault())
        val dateKey = dateFormat.format(Date()).toInt()  // Unique key for each day
        return quotes[dateKey % quotes.size]  // Rotate quotes daily
    }

    private fun getRecommendations(mood: String?): List<HomeItem> {
        return when (mood) {
            "happy" -> listOf(
                HomeItem("Music", R.drawable.ic_music, R.id.action_homeFragment_to_musicFragment),
                HomeItem("Travel", R.drawable.ic_travel, R.id.action_homeFragment_to_travelFragment),
                HomeItem("Exercise", R.drawable.ic_exercise, R.id.action_homeFragment_to_exerciseFragment),
                HomeItem("Food", R.drawable.ic_food, R.id.action_homeFragment_to_foodFragment),
                HomeItem("Stories", R.drawable.ic_stories, R.id.action_homeFragment_to_storiesFragment)
            )
            "sad" -> listOf(
                HomeItem("Music", R.drawable.ic_music, R.id.action_homeFragment_to_musicFragment),
                HomeItem("Stories", R.drawable.ic_stories, R.id.action_homeFragment_to_storiesFragment),
                HomeItem("Food", R.drawable.ic_food, R.id.action_homeFragment_to_foodFragment)
            )
            "stressed" -> listOf(
                HomeItem("Exercise", R.drawable.ic_exercise, R.id.action_homeFragment_to_exerciseFragment),
                HomeItem("Travel", R.drawable.ic_travel, R.id.action_homeFragment_to_travelFragment),
                HomeItem("Music", R.drawable.ic_music, R.id.action_homeFragment_to_musicFragment)
            )
            else -> listOf(
                HomeItem("Exercise", R.drawable.ic_exercise, R.id.action_homeFragment_to_exerciseFragment),
                HomeItem("Food", R.drawable.ic_food, R.id.action_homeFragment_to_foodFragment),
                HomeItem("Travel", R.drawable.ic_travel, R.id.action_homeFragment_to_travelFragment),
                HomeItem("Music", R.drawable.ic_music, R.id.action_homeFragment_to_musicFragment),
                HomeItem("Stories", R.drawable.ic_stories, R.id.action_homeFragment_to_storiesFragment)
            )
        }
    }
}
