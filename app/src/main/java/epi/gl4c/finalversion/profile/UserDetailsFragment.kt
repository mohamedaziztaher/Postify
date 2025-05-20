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
    private lateinit var postsRef: DatabaseReference
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
        postsRef = FirebaseDatabase.getInstance().getReference("publications") // Initialize postsRef here

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
        loadUserPosts(currentUser.uid) // Pass the current user ID

        editProfileBtn.setOnClickListener {
            (parentFragment as? ProfileFragment)?.showEditProfile()
        }

        return view
    }

    private fun loadUserData() {
        databaseRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (!isAdded) return  // Check if fragment is attached
                
                val username = snapshot.child("username").getValue(String::class.java) ?: "User"
                val bio = snapshot.child("bio").getValue(String::class.java) ?: ""
                val photoUrl = snapshot.child("photoUrl").getValue(String::class.java)

                Log.d("UserDetailsFragment", "Username: $username, Bio: $bio, ProfileImageUrl: $photoUrl")

                profileName.text = username
                profileBio.text = bio

                if (!isAdded) return  // Check again before loading image
                
                if (!photoUrl.isNullOrEmpty()) {
                    Glide.with(requireContext())
                        .load(photoUrl)
                        .placeholder(R.drawable.ic_person)
                        .error(R.drawable.ic_person)
                        .into(profileImage)
                }
            }

            override fun onCancelled(error: DatabaseError) {
                if (!isAdded) return
                Toast.makeText(requireContext(), "Failed to load user data", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun loadUserPosts(userId: String) {
        postsRef.orderByChild("userId").equalTo(userId)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    if (!isAdded) return  // Check if fragment is attached
                    
                    userPostsList.clear()
                    for (postSnapshot in snapshot.children) {
                        val imageUrl = postSnapshot.child("imageUrl").getValue(String::class.java)
                        if (!imageUrl.isNullOrEmpty()) {
                            userPostsList.add(imageUrl)
                        }
                    }
                    userPostsAdapter.notifyDataSetChanged()
                }

                override fun onCancelled(error: DatabaseError) {
                    if (!isAdded) return
                    Toast.makeText(requireContext(), "Failed to load posts", Toast.LENGTH_SHORT).show()
                }
            })
    }
}
