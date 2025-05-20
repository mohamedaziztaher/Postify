package epi.gl4c.finalversion.profile

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.GridView
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import android.util.Log
import com.bumptech.glide.Glide
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import epi.gl4c.finalversion.R
import epi.gl4c.finalversion.adapter.UserPostsAdapter

class UserDetailsFragment : Fragment() {
    private lateinit var profileImage: ImageView
    private lateinit var profileName: TextView
    private lateinit var profileBio: TextView
    private lateinit var editProfileBtn: Button
    private lateinit var postsGrid: GridView

    private lateinit var databaseRef: DatabaseReference
    private lateinit var auth: FirebaseAuth

    private lateinit var userPostsAdapter: UserPostsAdapter
    private val userPostsList = mutableListOf<String>() // List to store post image URLs

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_user_details, container, false)

        // Initialize Firebase
        auth = FirebaseAuth.getInstance()
        val currentUser = auth.currentUser
        databaseRef = FirebaseDatabase.getInstance().getReference("users").child(currentUser!!.uid)

        // Bind views
        profileImage = view.findViewById(R.id.profile_image)
        profileName = view.findViewById(R.id.profile_name)
        profileBio = view.findViewById(R.id.profile_bio)
        editProfileBtn = view.findViewById(R.id.btn_edit_profile)
        postsGrid = view.findViewById(R.id.posts_grid)

        // Initialize adapter for posts grid
        userPostsAdapter = UserPostsAdapter(requireContext(), userPostsList)
        postsGrid.adapter = userPostsAdapter

        loadUserData()
        loadUserPosts()

        editProfileBtn.setOnClickListener {
            (parentFragment as? ProfileFragment)?.showEditProfile()
        }

        return view
    }

    private fun loadUserData() {
        databaseRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val username = snapshot.child("username").getValue(String::class.java) ?: "User"
                val bio = snapshot.child("bio").getValue(String::class.java) ?: ""
                val profileImageUrl = snapshot.child("photoUrl").getValue(String::class.java)

                // Log the retrieved user data
                Log.d("UserDetailsFragment", "Username: $username, Bio: $bio, ProfileImageUrl: $profileImageUrl")

                profileName.text = username
                profileBio.text = bio

                if (!profileImageUrl.isNullOrEmpty()) {
                    Glide.with(requireContext())
                        .load(profileImageUrl)
                        .placeholder(R.drawable.ic_person)
                        .error(R.drawable.ic_person)
                        .into(profileImage)
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(requireContext(), "Failed to load user data", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun loadUserPosts() {
        val currentUserId = auth.currentUser?.uid ?: return
        val postsRef = FirebaseDatabase.getInstance().getReference("publications")

        postsRef.orderByChild("userId").equalTo(currentUserId)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    userPostsList.clear()
                    for (postSnapshot in snapshot.children) {
                        val imageUrl = postSnapshot.child("photoUrl").getValue(String::class.java)
                        if (!imageUrl.isNullOrEmpty()) {
                            userPostsList.add(imageUrl)
                        }
                    }

                    // Log the retrieved posts
                    Log.d("UserDetailsFragment", "User Posts: $userPostsList")

                    userPostsAdapter.notifyDataSetChanged()
                }

                override fun onCancelled(error: DatabaseError) {
                    Toast.makeText(requireContext(), "Failed to load user posts", Toast.LENGTH_SHORT).show()
                }
            })
    }
}
