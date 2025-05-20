package epi.gl4c.finalversion.main

import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.DividerItemDecoration
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import epi.gl4c.finalversion.R
import epi.gl4c.finalversion.adapter.PostAdapter
import epi.gl4c.finalversion.databinding.FragmentHomeBinding
import epi.gl4c.finalversion.model.Comment
import epi.gl4c.finalversion.model.Post
import epi.gl4c.finalversion.profile.ProfileFragment

class HomeFragment : Fragment() {
    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!
    private lateinit var publicationAdapter: PostAdapter
    private lateinit var publicationList: MutableList<Post>
    private lateinit var publicationRef: DatabaseReference
    private lateinit var databaseRef: DatabaseReference
    private lateinit var auth: FirebaseAuth

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupRecyclerView()
        initFirebase()
        loadPublications()

        val sharedPreferences = requireContext().getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
        val hasShownDialog = sharedPreferences.getBoolean("hasShownProfileDialog", false)

        if (!hasShownDialog) {
            databaseRef.child("profileComplete").get().addOnSuccessListener { snapshot ->
                val profileComplete = snapshot.getValue(Boolean::class.java) ?: false
                if (!profileComplete && isAdded) { // Ensure the fragment is still attached
                    showCompleteProfileDialog()
                    sharedPreferences.edit().putBoolean("hasShownProfileDialog", true).apply()
                }
            }.addOnFailureListener {
                if (isAdded) { // Ensure the fragment is still attached
                    Toast.makeText(requireContext(), "Failed to fetch profile status", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun setupRecyclerView() {
        binding.recyclerView.apply {
            layoutManager = LinearLayoutManager(requireContext())
            publicationList = ArrayList()
            publicationAdapter = PostAdapter(requireContext(), publicationList)
            adapter = publicationAdapter
            addItemDecoration(
                DividerItemDecoration(requireContext(), DividerItemDecoration.VERTICAL)
            )
        }
    }

    private fun initFirebase() {
        auth = FirebaseAuth.getInstance()
        val currentUser = auth.currentUser ?: return
        databaseRef = FirebaseDatabase.getInstance().getReference("users").child(currentUser.uid)
        publicationRef = FirebaseDatabase.getInstance().getReference("publications")
    }

    private fun loadPublications() {
        publicationRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (!isAdded || _binding == null) return  // Add null check

                publicationList.clear()
                for (pubSnapshot in snapshot.children) {
                    pubSnapshot.getValue(Post::class.java)?.let { pub ->
                        pub.setId(pubSnapshot.key)
                        loadComments(pubSnapshot, pub)
                        publicationList.add(pub)
                    }
                }
                publicationList.sortByDescending { it.getTimestamp() }
                publicationAdapter.notifyDataSetChanged()
                updateEmptyState()
            }

            override fun onCancelled(error: DatabaseError) {
                if (isAdded) {
                    Toast.makeText(requireContext(), "Error: ${error.message}", Toast.LENGTH_SHORT).show()
                }
            }
        })
    }

    private fun loadComments(pubSnapshot: DataSnapshot, pub: Post) {
        val comments = mutableMapOf<String, Comment>()
        pubSnapshot.child("comments").children.forEach { commentSnapshot ->
            commentSnapshot.getValue(Comment::class.java)?.let { comment ->
                commentSnapshot.key?.let { key -> comments[key] = comment }
            }
        }
        pub.setComments(comments)
    }

    private fun updateEmptyState() {
        if (!isAdded || _binding == null) return  // Add null check
        binding.emptyStateLayout.visibility = if (publicationList.isEmpty()) View.VISIBLE else View.GONE
    }

    private fun showCompleteProfileDialog() {
        val dialogView = LayoutInflater.from(requireContext())
            .inflate(R.layout.profile_completion_dialog, null)
        val dialog = android.app.AlertDialog.Builder(requireContext())
            .setView(dialogView)
            .setCancelable(false)
            .create()

        val btnCompleteProfile = dialogView.findViewById<Button>(R.id.btn_complete_now)
        val tvSkip = dialogView.findViewById<TextView>(R.id.tv_skip)

        btnCompleteProfile.setOnClickListener {
            Log.d("HomeFragment", "Complete Now button clicked") // Log button click
            if (isAdded && parentFragmentManager != null) {
                Log.d("HomeFragment", "Navigating to ProfileFragment") // Log navigation attempt
                val profileFragment = ProfileFragment().apply {
                    arguments = Bundle().apply {
                        putBoolean("showEditProfile", true)
                    }
                }
                parentFragmentManager.beginTransaction()
                    .replace(R.id.fragment_container, profileFragment)
                    .addToBackStack(null)
                    .commit()
            } else {
                Log.d("HomeFragment", "Fragment not attached or parentFragmentManager is null") // Log failure
            }
            dialog.dismiss()
        }

        tvSkip.setOnClickListener {
            Log.d("HomeFragment", "Skip button clicked") // Log skip button click
            dialog.dismiss()
        }

        dialog.show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
