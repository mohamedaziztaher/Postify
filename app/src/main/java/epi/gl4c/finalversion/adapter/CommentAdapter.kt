package epi.gl4c.finalversion.adapter

import android.content.Context
import android.text.format.DateUtils
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import epi.gl4c.finalversion.R
import epi.gl4c.finalversion.model.Comment

class CommentAdapter(
    private val context: Context,
    private val comments: List<Comment>
) : RecyclerView.Adapter<CommentAdapter.CommentViewHolder>() {

    private val usersRef: DatabaseReference = FirebaseDatabase.getInstance().getReference("users")

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CommentViewHolder {
        val view = LayoutInflater.from(context).inflate(R.layout.item_comment, parent, false)
        return CommentViewHolder(view)
    }

    override fun onBindViewHolder(holder: CommentViewHolder, position: Int) {
        val comment = comments[position]
        holder.tvComment.text = comment.text
        holder.tvTimestamp.text = DateUtils.getRelativeTimeSpanString(
            comment.timestamp,
            System.currentTimeMillis(),
            DateUtils.MINUTE_IN_MILLIS
        )

        // Fetch username from Firebase using the userId stored in the comment
        comment.userId?.let {
            usersRef.child(it).addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    // Get the full name from the user data
                    val username = snapshot.child("username").getValue(String::class.java)
                    holder.tvUsername.text = username ?: "User"
                }

                override fun onCancelled(error: DatabaseError) {
                    holder.tvUsername.text = "User"
                }
            })
        }

        // Set default avatar for the comment
        holder.ivAvatar.setImageResource(R.drawable.ic_person) // Default avatar
    }

    override fun getItemCount(): Int = comments.size

    class CommentViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val ivAvatar: ImageView = itemView.findViewById(R.id.ivCommentAvatar)
        val tvUsername: TextView = itemView.findViewById(R.id.tvCommentUsername)
        val tvComment: TextView = itemView.findViewById(R.id.tvCommentText)
        val tvTimestamp: TextView = itemView.findViewById(R.id.tvCommentTimestamp)
    }
}