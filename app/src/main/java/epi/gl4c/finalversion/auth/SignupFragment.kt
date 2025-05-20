package epi.gl4c.finalversion.auth

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.ktx.Firebase
import epi.gl4c.finalversion.databinding.FragmentSignupBinding
import epi.gl4c.finalversion.model.User

class SignupFragment : Fragment() {
    private var _binding: FragmentSignupBinding? = null
    private val binding get() = _binding!!
    private lateinit var auth: FirebaseAuth
    private lateinit var databaseRef: DatabaseReference

    var onSignupSuccess: (() -> Unit)? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSignupBinding.inflate(inflater, container, false)
        auth = Firebase.auth
        databaseRef = FirebaseDatabase.getInstance().getReference("users")
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.signupButton.setOnClickListener {
            val username = binding.usernameEditText.text.toString().trim()
            val email = binding.emailEditText.text.toString().trim()
            val password = binding.passwordEditText.text.toString().trim()
            val confirmPassword = binding.confirmPasswordEditText.text.toString().trim()

            when {
                username.isEmpty() -> binding.usernameInput.error = "Username required"
                email.isEmpty() -> binding.emailInput.error = "Email required"
                password.isEmpty() -> binding.passwordInput.error = "Password required"
                password != confirmPassword -> binding.errorText.text = "Passwords don't match"
                else -> createUser(username, email, password)
            }
        }

        binding.loginText.setOnClickListener {
            activity?.supportFragmentManager?.popBackStack()
        }
    }

    private fun createUser(username: String, email: String, password: String) {
        binding.progressBar.visibility = View.VISIBLE
        binding.signupButton.isEnabled = false

        auth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    saveUserToDatabase(username, email)
                } else {
                    binding.progressBar.visibility = View.GONE
                    binding.signupButton.isEnabled = true
                    binding.errorText.text = task.exception?.message ?: "Signup failed"
                }
            }
    }

    private fun saveUserToDatabase(username: String, email: String) {
        val userId = auth.currentUser?.uid ?: return

        val user = User(
            id = userId,
            username = username,
            email = email,
            isProfileComplete = false,
            createdAt = System.currentTimeMillis()
        )

        databaseRef.child(userId).setValue(user)
            .addOnCompleteListener { task ->
                binding.progressBar.visibility = View.GONE
                binding.signupButton.isEnabled = true

                if (task.isSuccessful) {
                    onSignupSuccess?.invoke()
                } else {
                    binding.errorText.text = task.exception?.message ?: "Failed to save user data"
                }
            }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}