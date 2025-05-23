package com.fincare.emitocare.Fragments

import android.os.Bundle
import android.text.TextUtils
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.fincare.emitocare.R
import com.fincare.emitocare.databinding.FragmentProfileBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class ProfileFragment : Fragment() {

    private var _binding: FragmentProfileBinding? = null
    private val binding get() = _binding!!

    private lateinit var auth: FirebaseAuth
    private lateinit var firestore: FirebaseFirestore

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentProfileBinding.inflate(inflater, container, false)
        auth = FirebaseAuth.getInstance()
        firestore = FirebaseFirestore.getInstance()

        loadUserProfile()
        setupSaveButton()
        setupLogoutButton()

        return binding.root
    }

    private fun loadUserProfile() {
        val userId = auth.currentUser?.uid ?: return
        firestore.collection("users").document(userId).get()
            .addOnSuccessListener { document ->
                if (!document.exists()) return@addOnSuccessListener

                binding.etName.setText(document.getString("name"))
                binding.etEmail.setText(document.getString("email"))
                binding.etPhone.setText(document.getString("phone"))
                binding.etWorkplace.setText(document.getString("workplace"))
                binding.etStartTime.setText(document.getString("workStartTime"))
                binding.etEndTime.setText(document.getString("workEndTime"))

                // Load friend info from nested map
                val friendMap = document.get("friend") as? Map<*, *>
                binding.etFriendName.setText(friendMap?.get("name") as? String)
                binding.etFriendPhone.setText(friendMap?.get("phone") as? String)
            }
            .addOnFailureListener {
                Toast.makeText(requireContext(), "Failed to load profile", Toast.LENGTH_SHORT).show()
            }
    }

    private fun setupSaveButton() {
        binding.btnSaveChanges.setOnClickListener {
            val name = binding.etName.text.toString().trim()
            val email = binding.etEmail.text.toString().trim()
            val phone = binding.etPhone.text.toString().trim()
            val workplace = binding.etWorkplace.text.toString().trim()
            val startTime = binding.etStartTime.text.toString().trim()
            val endTime = binding.etEndTime.text.toString().trim()
            val friendName = binding.etFriendName.text.toString().trim()
            val friendPhone = binding.etFriendPhone.text.toString().trim()

            if (listOf(name, email, phone, friendName, friendPhone).any { TextUtils.isEmpty(it) }) {
                Toast.makeText(requireContext(), "Please fill in all required fields", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val userId = auth.currentUser?.uid ?: return@setOnClickListener
            val updatedUser = mutableMapOf<String, Any>(
                "name" to name,
                "email" to email,
                "phone" to phone,
                "workplace" to workplace,
                "workStartTime" to startTime,
                "workEndTime" to endTime,
                "friend" to mapOf(
                    "name" to friendName,
                    "phone" to friendPhone
                )
            )

            firestore.collection("users").document(userId).update(updatedUser)
                .addOnSuccessListener {
                    Toast.makeText(requireContext(), "Profile updated successfully", Toast.LENGTH_SHORT).show()
                }
                .addOnFailureListener {
                    Toast.makeText(requireContext(), "Failed to update profile", Toast.LENGTH_SHORT).show()
                }
        }
    }

    private fun setupLogoutButton() {
        binding.btnLogout.setOnClickListener {
            FirebaseAuth.getInstance().signOut()
            findNavController().navigate(R.id.action_profile_to_login)
        }
    }


}
