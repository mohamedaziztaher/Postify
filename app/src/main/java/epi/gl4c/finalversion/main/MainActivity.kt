package epi.gl4c.finalversion.main

import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import epi.gl4c.finalversion.R
import epi.gl4c.finalversion.auth.LoginActivity
import epi.gl4c.finalversion.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private val auth: FirebaseAuth by lazy { FirebaseAuth.getInstance() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupToolbar()
        setupBottomNavigation()
        loadDefaultFragment(savedInstanceState)
    }

    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayShowTitleEnabled(false)
    }

    private fun setupBottomNavigation() {
        binding.bottomNavigation.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home -> {
                    replaceFragment(HomeFragment())
                    true
                }
                R.id.fab_add -> {
                    replaceFragment(AddPostFragment(), addToBackStack = true)
                    true
                }
                R.id.nav_profile -> {
                    handleProfileNavigation()
                    true
                }
                else -> false
            }
        }
    }

    private fun replaceFragment(fragment: androidx.fragment.app.Fragment, addToBackStack: Boolean = false) {
        supportFragmentManager.beginTransaction().apply {
            replace(R.id.fragment_container, fragment)
            if (addToBackStack) addToBackStack(null)
            commit()
        }
    }

    private fun handleProfileNavigation() {
        if (auth.currentUser != null) {
            replaceFragment(ProfileFragment())
        } else {
            navigateToLogin(clearTask = true)
        }
    }

    private fun loadDefaultFragment(savedInstanceState: Bundle?) {
        if (savedInstanceState == null) {
            replaceFragment(HomeFragment())
            binding.bottomNavigation.selectedItemId = R.id.nav_home
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.toolbar_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.action_logout -> {
                handleLogout()
                return true
            }
        }
        return super.onOptionsItemSelected(item)
    }

    private fun handleLogout() {
        auth.currentUser?.let {
            auth.signOut()
            Toast.makeText(this, "Logged out successfully", Toast.LENGTH_SHORT).show()
        }
        navigateToLogin(clearTask = true)
    }

    override fun onStart() {
        super.onStart()
        if (auth.currentUser == null) {
            navigateToLogin(clearTask = true)
        }
    }

    private fun navigateToLogin(clearTask: Boolean = false) {
        Intent(this, LoginActivity::class.java).apply {
            if (clearTask) {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            }
            startActivity(this)
        }
        finish()
    }
}