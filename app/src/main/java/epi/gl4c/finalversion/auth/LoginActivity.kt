package epi.gl4c.finalversion.auth

import android.content.Intent
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import epi.gl4c.finalversion.main.MainActivity
import epi.gl4c.finalversion.databinding.ActivityLoginBinding

class LoginActivity : AppCompatActivity() {
    private lateinit var binding: ActivityLoginBinding
    private lateinit var auth: FirebaseAuth


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // ViewBinding setup
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Firebase Auth setup
        auth = Firebase.auth

        // Check current user on startup
        checkCurrentUser()

        // Only setup fragments if not redirecting
        if (savedInstanceState == null) {
            showLoginFragment()
        }
    }

    private fun checkCurrentUser() {
        auth.currentUser?.let {
            redirectToMain()
        }
    }

    private fun redirectToMain() {
        startActivity(Intent(this, MainActivity::class.java))
        finish()
    }

    fun showLoginFragment() {
        supportFragmentManager.beginTransaction()
            .replace(binding.fragmentContainer.id, LoginFragment().apply {
                onLoginSuccess = { redirectToMain() }
            })
            .commit()
    }

    fun showSignupFragment() {
        supportFragmentManager.beginTransaction()
            .replace(binding.fragmentContainer.id, SignupFragment().apply {
                onSignupSuccess = { showLoginFragment() } // Redirect to login screen
            })
            .addToBackStack("signup")
            .commit()
    }
}