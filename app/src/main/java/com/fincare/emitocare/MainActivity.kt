package com.fincare.emitocare

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.MenuItem
import android.view.View
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.content.ContextCompat
import androidx.core.app.NotificationManagerCompat
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.NavigationUI
import com.fincare.emitocare.WorkTimeScheduler
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.floatingactionbutton.FloatingActionButton

class MainActivity : AppCompatActivity() {

    private lateinit var bottomNavigationView: BottomNavigationView
    private val firestore = FirebaseFirestore.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // 1. Check login status and navigate accordingly
        val navHostFragment =
            supportFragmentManager.findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        val navController = navHostFragment.navController



        // 2. Toolbar & BottomNav setup
        val toolbar: Toolbar = findViewById(R.id.toolbar)
        setSupportActionBar(toolbar)
        bottomNavigationView = findViewById(R.id.bottom_nav)
        NavigationUI.setupWithNavController(bottomNavigationView, navController)

        // 3. FAB for chat
        val fabChat: FloatingActionButton = findViewById(R.id.fab_chat)
        fabChat.setOnClickListener { navController.navigate(R.id.chatFragment) }

        // 4. Permissions & scheduler
        checkAndRequestNotificationPermission()
        WorkTimeScheduler(this).scheduleWorkTimeNotifications()

        // 5. Show/hide nav and handle back button
        navController.addOnDestinationChangedListener { _, dest, _ ->
            when (dest.id) {
                R.id.signupFragment, R.id.chatFragment, R.id.profile -> {
                    bottomNavigationView.visibility = View.GONE
                    fabChat.visibility = View.GONE
                    supportActionBar?.setDisplayHomeAsUpEnabled(true)
                    if (dest.id == R.id.chatFragment) deleteOldMessages()
                }
                R.id.homeFragment -> {
                    bottomNavigationView.visibility = View.VISIBLE
                    fabChat.visibility = View.VISIBLE
                    supportActionBar?.setDisplayHomeAsUpEnabled(false)
                }
                R.id.login -> {
                    bottomNavigationView.visibility = View.GONE
                    fabChat.visibility = View.GONE
                    supportActionBar?.setDisplayHomeAsUpEnabled(false)
                }
                else -> {
                    bottomNavigationView.visibility = View.VISIBLE
                    fabChat.visibility = View.GONE
                    supportActionBar?.setDisplayHomeAsUpEnabled(true)
                }
            }
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        val navHostFragment =
            supportFragmentManager.findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        return navHostFragment.navController.navigateUp() || super.onSupportNavigateUp()
    }

    // Notification permission
    private fun checkAndRequestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.POST_NOTIFICATIONS
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    private val requestPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { /* no-op */ }

    // Delete old messages
    private fun deleteOldMessages() {
        val cutoff = System.currentTimeMillis() - 86_400_000L
        firestore.collection("users")
            .document(FirebaseAuth.getInstance().currentUser?.uid ?: return)
            .collection("chats")
            .whereLessThan("timestamp", cutoff)
            .get()
            .addOnSuccessListener { snap ->
                snap.documents.forEach {
                    it.reference.delete()
                }
            }
    }
}
