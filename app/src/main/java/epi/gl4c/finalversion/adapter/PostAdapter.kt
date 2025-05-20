package epi.gl4c.finalversion.adapter

import android.content.Context
import android.content.Intent
import android.text.format.DateUtils
import android.util.Log
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import android.view.LayoutInflater
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.recyclerview.widget.LinearLayoutManager
import epi.gl4c.finalversion.R
import epi.gl4c.finalversion.model.Post
import com.bumptech.glide.Glide
import com.google.android.gms.tasks.Task
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import epi.gl4c.finalversion.auth.LoginActivity
import epi.gl4c.finalversion.model.Comment
import com.google.android.material.dialog.MaterialAlertDialogBuilder

class PostAdapter (
    private val context: Context,
    private val publications: MutableList<Post>
) : RecyclerView.Adapter<PostAdapter.PublicationViewHolder>() {

    private val mAuth: FirebaseAuth = FirebaseAuth.getInstance()
    private val likesRef: DatabaseReference = FirebaseDatabase.getInstance().getReference("likes")
    private val commentsRef: DatabaseReference = FirebaseDatabase.getInstance().getReference("publications")
    private val usersRef: DatabaseReference = FirebaseDatabase.getInstance().getReference("users")
    private val publicationsRef: DatabaseReference = FirebaseDatabase.getInstance().getReference("publications")

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PublicationViewHolder {
        val view: View =
            LayoutInflater.from(context).inflate(R.layout.item_publication, parent, false)
        return PublicationViewHolder(view)
    }

    override fun onBindViewHolder(holder: PublicationViewHolder, position: Int) {
        val publication = publications[position]
        val currentUserId = mAuth.currentUser?.uid ?: ""

        holder.tvCaption.text = publication.getCaption()

        // Load publication image
        if (!publication.getImageUrl().isNullOrEmpty()) {
            holder.ivPublicationImage.visibility = View.VISIBLE
            Glide.with(context)
                .load(publication.getImageUrl())
                .placeholder(R.drawable.placeholder_image)
                .into(holder.ivPublicationImage)
        } else {
            holder.ivPublicationImage.visibility = View.GONE
        }

        // Load username and user avatar from Firebase using the user ID from the publication
        val userId1 = publication.getUserId()
        if (userId1 != null) {
            usersRef.child(userId1).addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val username = snapshot.child("username").getValue(String::class.java)
                    val photoUrl = snapshot.child("photoUrl").getValue(String::class.java)

                    holder.tvUsername.text = username ?: "User"

                    if (!photoUrl.isNullOrEmpty()) {
                        Glide.with(context)
                            .load(photoUrl)
                            .placeholder(R.drawable.ic_person)  // a default avatar image in your drawable
                            .circleCrop() // optional, to make avatar circular
                            .into(holder.ivUserAvatar)
                    } else {
                        holder.ivUserAvatar.setImageResource(R.drawable.ic_person)
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    Log.e("FirebaseError", "Could not fetch user data: ${error.message}")
                    holder.tvUsername.text = "User"
                    holder.ivUserAvatar.setImageResource(R.drawable.ic_person)
                }
            })
        } else {
            holder.tvUsername.text = "User"
            holder.ivUserAvatar.setImageResource(R.drawable.ic_person)
        }

        // Load timestamp
        holder.tvTimestamp.setText(
            DateUtils.getRelativeTimeSpanString(
                publication.getTimestamp(), System.currentTimeMillis(), DateUtils.MINUTE_IN_MILLIS
            )
        )

        // Show delete icon only for the connected user's posts
        if (publication.getUserId() != null && publication.getUserId().equals(currentUserId)) {
            holder.ivDeletePost.setVisibility(View.VISIBLE)
        } else {
            holder.ivDeletePost.setVisibility(View.GONE)
        }

        // Handle delete icon click
        holder.ivDeletePost.setOnClickListener {
            val adapterPosition = holder.adapterPosition
            if (adapterPosition != RecyclerView.NO_POSITION) {
                deletePublication(publication.getIdOrDefault(), adapterPosition)
            }
        }

        // Setup like button
        updateLikeButton(holder.btnLike, publication.getIdOrDefault())

        holder.btnLike.setOnClickListener {
            val userId = mAuth.currentUser?.uid
            if (userId != null) {
                val pubId = publication.getIdOrDefault()
                likesRef.child(pubId).child(userId)
                    .addListenerForSingleValueEvent(object : ValueEventListener {
                        override fun onDataChange(snapshot: DataSnapshot) {
                            if (snapshot.exists()) {
                                likesRef.child(pubId).child(userId).removeValue()
                            } else {
                                likesRef.child(pubId).child(userId).setValue(true)
                            }
                            updateLikeButton(holder.btnLike, pubId)
                        }

                        override fun onCancelled(error: DatabaseError) {}
                    })
            } else {
                context.startActivity(Intent(context, LoginActivity::class.java))
            }
        }

        // Setup comments
        val commentList = ArrayList<Comment>()
        publication.getComments()?.values?.let { commentList.addAll(it) }
        commentList.sortWith { c1, c2 -> c2.timestamp.compareTo(c1.timestamp) }

        val commentAdapter = CommentAdapter(context, commentList)
        holder.rvComments.layoutManager = LinearLayoutManager(context)
        holder.rvComments.adapter = commentAdapter

        // Post comment
        holder.btnPostComment.setOnClickListener {
            val commentText = holder.etNewComment.text.toString().trim()
            if (commentText.isEmpty()) return@setOnClickListener

            val userId = mAuth.currentUser?.uid
            if (userId != null) {
                val comment = Comment().apply {
                    this.userId = userId
                    this.text = commentText
                    this.timestamp = System.currentTimeMillis()
                }

                publication.getIdOrDefault()?.let { pubId ->
                    commentsRef.child(pubId).child("comments").push().key?.let { commentId ->
                        commentsRef.child(pubId).child("comments").child(commentId)
                            .setValue(comment)
                            .addOnSuccessListener { holder.etNewComment.text?.clear() }
                    }
                }
            } else {
                context.startActivity(Intent(context, LoginActivity::class.java))
            }
        }
    }

    private fun updateLikeButton(button: MaterialButton, publicationId: String) {
        if (publicationId.isEmpty()) {
            button.text = "0 J'aime"
            button.icon = context.getDrawable(R.drawable.ic_like_outline)
            button.isSelected = false
            return
        }
        
        likesRef.child(publicationId).addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val likeCount = snapshot.childrenCount
                button.text = "$likeCount J'aime"

                val currentUserId = mAuth.currentUser?.uid
                val isLiked = currentUserId != null && snapshot.hasChild(currentUserId)
                
                button.isSelected = isLiked
                button.icon = context.getDrawable(
                    if (isLiked) R.drawable.ic_like_filled else R.drawable.ic_like_outline
                )
                button.setIconTintResource(R.color.like_button_color)
                button.setTextColor(context.getColorStateList(R.color.like_button_color))
            }

            override fun onCancelled(error: DatabaseError) {
                button.text = "0 J'aime"
                button.icon = context.getDrawable(R.drawable.ic_like_outline)
                button.isSelected = false
            }
        })
    }

    private fun deletePublication(publicationId: String?, position: Int) {
        if (publicationId == null) {
            Toast.makeText(context, "Error: Invalid post ID", Toast.LENGTH_SHORT).show()
            return
        }

        MaterialAlertDialogBuilder(context)
            .setTitle(R.string.delete_post_title)
            .setMessage(R.string.delete_post_message)
            .setIcon(R.drawable.ic_delete)
            .setPositiveButton(R.string.yes) { dialog, _ ->
                deletePostFromFirebase(publicationId, position)
                dialog.dismiss()
            }
            .setNegativeButton(R.string.no) { dialog, _ ->
                dialog.dismiss()
            }
            .setCancelable(true)
            .create()
            .show()
    }

    private fun deletePostFromFirebase(publicationId: String, position: Int) {
        publicationsRef.child(publicationId).removeValue()
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    publications.removeAt(position)
                    notifyItemRemoved(position)
                    notifyItemRangeChanged(position, publications.size)
                    Toast.makeText(context, R.string.delete_post_success, Toast.LENGTH_SHORT).show()
                    
                    // Delete associated likes and comments
                    likesRef.child(publicationId).removeValue()
                } else {
                    Toast.makeText(
                        context,
                        context.getString(R.string.delete_post_error, task.exception?.message),
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
    }

    override fun getItemCount(): Int {
        return publications!!.size
    }

    class PublicationViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val ivUserAvatar: ImageView = itemView.findViewById(R.id.ivUserAvatar)
        val ivPublicationImage: ImageView = itemView.findViewById(R.id.imageViewPublication)
        val ivDeletePost: ImageView = itemView.findViewById(R.id.ivDeletePost)
        val tvUsername: TextView = itemView.findViewById(R.id.name)
        val tvTimestamp: TextView = itemView.findViewById(R.id.tvTimestamp)
        val tvCaption: TextView = itemView.findViewById(R.id.tvCaption)
        val btnLike: MaterialButton = itemView.findViewById(R.id.btnLike)
        val btnPostComment: MaterialButton = itemView.findViewById(R.id.btnPostComment)
        val rvComments: RecyclerView = itemView.findViewById(R.id.rvComments)
        val etNewComment: TextInputEditText = itemView.findViewById(R.id.etNewComment)
    }
}
