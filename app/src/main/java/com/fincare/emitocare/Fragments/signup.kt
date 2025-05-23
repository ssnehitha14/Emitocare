package com.fincare.emitocare.Fragments

import android.app.TimePickerDialog
import android.os.Bundle
import android.text.TextUtils
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.fincare.emitocare.R
import com.fincare.emitocare.databinding.FragmentSignupBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.util.*

class SignUpFragment : Fragment() {

    private var _binding: FragmentSignupBinding? = null
    private val binding get() = _binding!!

    private lateinit var auth: FirebaseAuth
    private lateinit var firestore: FirebaseFirestore

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSignupBinding.inflate(inflater, container, false)

        auth = FirebaseAuth.getInstance()
        firestore = FirebaseFirestore.getInstance()

        setupTimePickers()
        setupCreateAccount()

        return binding.root
    }

    private fun setupTimePickers() {
        val timeSetListener = { editText: android.widget.EditText ->
            val cal = Calendar.getInstance()
            TimePickerDialog(
                requireContext(),
                { _, hour, minute ->
                    val amPm = if (hour >= 12) "PM" else "AM"
                    val hour12 = if (hour % 12 == 0) 12 else hour % 12
                    val formatted = String.format("%02d:%02d %s", hour12, minute, amPm)
                    editText.setText(formatted)
                },
                cal.get(Calendar.HOUR_OF_DAY),
                cal.get(Calendar.MINUTE),
                false // Use 12-hour format
            ).show()
        }

        binding.etStartTime.setOnClickListener { timeSetListener(binding.etStartTime) }
        binding.etEndTime.setOnClickListener { timeSetListener(binding.etEndTime) }
    }

    private fun setupCreateAccount() {
        binding.btnCreateAccount.setOnClickListener {
            val name = binding.etName.text.toString().trim()
            val email = binding.etEmail.text.toString().trim()
            val password = binding.etPassword.text.toString().trim()
            val phone = binding.etPhone.text.toString().trim()
            val workplace = binding.etWorkplace.text.toString().trim()
            val startTime = binding.etStartTime.text.toString().trim()
            val endTime = binding.etEndTime.text.toString().trim()
            val friendName = binding.etFriendName.text.toString().trim()
            val friendPhone = binding.etFriendPhone.text.toString().trim()

            if (listOf(
                    name, email, password, phone, workplace,
                    startTime, endTime, friendName, friendPhone
                ).any { TextUtils.isEmpty(it) }
            ) {
                Toast.makeText(requireContext(), "Please fill all fields", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            auth.createUserWithEmailAndPassword(email, password)
                .addOnSuccessListener { result ->
                    val uid = result.user?.uid ?: return@addOnSuccessListener
                    val userData = hashMapOf(
                        "uid" to uid,
                        "name" to name,
                        "email" to email,
                        "phone" to phone,
                        "workplace" to workplace,
                        "workStartTime" to startTime,
                        "workEndTime" to endTime,
                        "friend" to mapOf("name" to friendName, "phone" to friendPhone)
                    )

                    firestore.collection("users").document(uid)
                        .set(userData)
                        .addOnSuccessListener {
                            Toast.makeText(requireContext(), "Account created!", Toast.LENGTH_SHORT).show()
                            // TODO: Navigate to home or login
                        }
                        .addOnFailureListener {
                            Toast.makeText(requireContext(), "Error saving data: ${it.message}", Toast.LENGTH_SHORT).show()
                        }
                }
                .addOnFailureListener {
                    Toast.makeText(requireContext(), "Signup failed: ${it.message}", Toast.LENGTH_SHORT).show()
                }
        }

        binding.tvLogin.setOnClickListener {
            parentFragmentManager.beginTransaction()
                .replace(R.id.nav_host_fragment, LoginFragment())
                .addToBackStack(null)
                .commit()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
