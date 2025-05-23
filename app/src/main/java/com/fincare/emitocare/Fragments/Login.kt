package com.fincare.emitocare.Fragments

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.fincare.emitocare.R
import com.google.firebase.auth.FirebaseAuth

class LoginFragment : Fragment() {

    private lateinit var auth: FirebaseAuth

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_login, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        auth = FirebaseAuth.getInstance() // Initialize Firebase Authentication

        val emailEditText = view.findViewById<EditText>(R.id.etEmail)
        val passwordEditText = view.findViewById<EditText>(R.id.etPassword)
        val btnLogin = view.findViewById<Button>(R.id.btnLogin)
        val btnSignup = view.findViewById<TextView>(R.id.btnSignup)

        // Login User
        btnLogin.setOnClickListener {
            val email = emailEditText.text.toString().trim()
            val password = passwordEditText.text.toString().trim()

            if (email.isNotEmpty() && password.isNotEmpty()) {
                loginUser(email, password)
            } else {
                Toast.makeText(requireContext(), "Please enter email and password", Toast.LENGTH_SHORT).show()
            }
        }

        // Navigate to Signup
        btnSignup.setOnClickListener {
            findNavController().navigate(R.id.action_login_to_signupFragment)
        }
    }

    private fun loginUser(email: String, password: String) {
        auth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener(requireActivity()) { task ->
                if (task.isSuccessful) {
                    Toast.makeText(requireContext(), "Login Successful", Toast.LENGTH_SHORT).show()
                    // Navigate to HomeFragment (Change to your desired fragment)
                    findNavController().navigate(R.id.action_login_to_home)
                } else {
                    Toast.makeText(requireContext(), "Login Failed: ${task.exception?.message}", Toast.LENGTH_SHORT).show()
                    Log.e("LoginError", "Error: ${task.exception?.message}")
                }
            }
    }
}
