package epi.gl4c.finalversion.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.BaseAdapter
import com.bumptech.glide.Glide
import epi.gl4c.finalversion.R

class UserPostsAdapter(
    private val context: Context,
    private val posts: List<String>
) : BaseAdapter() {

    override fun getCount(): Int = posts.size

    override fun getItem(position: Int): Any = posts[position]

    override fun getItemId(position: Int): Long = position.toLong()

    override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {
        val view = convertView ?: LayoutInflater.from(context)
            .inflate(R.layout.item_post, parent, false)

        val imageView = view.findViewById<ImageView>(R.id.post_image)
        Glide.with(context)
            .load(posts[position])
            .placeholder(R.drawable.placeholder_image)
            .into(imageView)

        return view
    }
}
