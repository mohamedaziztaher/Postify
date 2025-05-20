package epi.gl4c.finalversion.main

import android.os.Bundle
import android.view.ContextMenu
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import epi.gl4c.finalversion.adapter.PostAdapter
import epi.gl4c.finalversion.databinding.FragmentHomeBinding
import epi.gl4c.finalversion.model.Comment
import epi.gl4c.finalversion.model.Post


class HomeFragment : Fragment() {
    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!
    private lateinit var publicationAdapter: PostAdapter
    private lateinit var publicationList: MutableList<Post>
    private lateinit var publicationRef: DatabaseReference

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)

        binding.recyclerView.layoutManager = LinearLayoutManager(requireContext())
        publicationList = ArrayList()
        publicationAdapter = PostAdapter(requireContext(), publicationList)
        binding.recyclerView.adapter = publicationAdapter

        publicationRef = FirebaseDatabase.getInstance().getReference("publications")

        loadPublications()

        return binding.root
    }

    private fun loadPublications() {
        publicationRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                publicationList.clear()

                for (pubSnapshot in snapshot.children) {
                    val pub: Post? =
                        pubSnapshot.getValue<Post>(Post::class.java)
                    if (pub != null) {
                        pub.setId(pubSnapshot.key)

                        val comments: MutableMap<String, Comment> = HashMap()
                        val commentsSnapshot = pubSnapshot.child("comments")
                        for (commentSnapshot in commentsSnapshot.children) {
                            val comment = commentSnapshot.getValue(
                                Comment::class.java
                            )
                            if (comment != null) {
                                commentSnapshot.key?.let { key ->
                                    comments[key] = comment
                                }
                            }
                        }
                        pub.setComments(comments)

                        publicationList.add(pub)
                    }
                }

                publicationList.sortWith { p1: Post, p2: Post ->
                    p2.getTimestamp().compareTo(p1.getTimestamp())
                }
                publicationAdapter.notifyDataSetChanged()
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(
                    context,
                    "Erreur de chargement: " + error.message,
                    Toast.LENGTH_SHORT
                ).show()
            }
        })
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}