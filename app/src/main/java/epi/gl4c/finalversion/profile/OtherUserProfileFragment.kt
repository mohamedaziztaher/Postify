package epi.gl4c.finalversion.profile


import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import epi.gl4c.finalversion.adapter.UserPostsAdapter
import epi.gl4c.finalversion.databinding.FragmentUserDetailsBinding
import epi.gl4c.finalversion.R
import com.bumptech.glide.Glide


class OtherUserProfileFragment : Fragment() {
    private var _binding: FragmentUserDetailsBinding? = null
    private val binding get() = _binding!!
    private lateinit var databaseRef: DatabaseReference
    private lateinit var postsRef: DatabaseReference
    private lateinit var userPostsAdapter: UserPostsAdapter
    private val userPostsList = mutableListOf<String>()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentUserDetailsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        val userId = arguments?.getString("userId") ?: run {
            parentFragmentManager.popBackStack()
            return
        }

        setupViews()
        loadUserData(userId)
        loadUserPosts(userId)
    }

    private fun setupViews() {
        // Hide edit profile button for other users
        binding.btnEditProfile.visibility = View.GONE
        
        // Initialize posts grid
        userPostsAdapter = UserPostsAdapter(requireContext(), userPostsList)
        binding.postsGrid.adapter = userPostsAdapter
    }

    private fun loadUserData(userId: String) {
        databaseRef = FirebaseDatabase.getInstance().getReference("users").child(userId)
        databaseRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val username = snapshot.child("username").getValue(String::class.java) ?: "User"
                val bio = snapshot.child("bio").getValue(String::class.java) ?: ""
                val photoUrl = snapshot.child("photoUrl").getValue(String::class.java)

                binding.profileName.text = username
                binding.profileBio.text = bio

                if (!photoUrl.isNullOrEmpty()) {
                    Glide.with(requireContext())
                        .load(photoUrl)
                        .placeholder(R.drawable.ic_person)
                        .error(R.drawable.ic_person)
                        .into(binding.profileImage)
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(context, "Error loading user data", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun loadUserPosts(userId: String) {
        postsRef = FirebaseDatabase.getInstance().getReference("publications")
        postsRef.orderByChild("userId").equalTo(userId)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
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
                    Toast.makeText(context, "Error loading posts", Toast.LENGTH_SHORT).show()
                }
            })
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
