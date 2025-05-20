package epi.gl4c.finalversion.model

import java.util.HashMap

class Post {
    private var id: String? = null
    private var userId: String? = null
    private var caption: String? = null
    private var imageUrl: String? = null
    private var timestamp: Long = 0
    private var comments: Map<String, Comment>? = null

    // Default constructor
    constructor() {
        this.comments = HashMap()
    }

    // Parameterized constructor matching the one used in AddPostFragment
    constructor(
        id: String,
        userId: String,
        caption: String,
        imageUrl: String,
        timestamp: Long,
        comments: HashMap<String, Any>
    ) {
        this.id = id
        this.userId = userId
        this.caption = caption
        this.imageUrl = imageUrl
        this.timestamp = timestamp
        // Convert the HashMap<String, Any> to HashMap<String, Commentaire> if needed
        // For now, just assign an empty HashMap
        this.comments = HashMap()
    }

    // Original parameterized constructor
    constructor(userId: String?, caption: String?, imageUrl: String?, timestamp: Long) {
        this.userId = userId
        this.caption = caption
        this.imageUrl = imageUrl
        this.timestamp = timestamp
        this.comments = HashMap()
    }

    // Update getId to return non-null value with default
    fun getIdOrDefault(): String {
        return id ?: ""
    }

    fun setId(id: String?) {
        this.id = id
    }

    fun getComments(): Map<String, Comment>? {
        return comments
    }

    fun setComments(comments: Map<String, Comment>?) {
        this.comments = comments ?: HashMap()
    }


    fun getImageUrl(): String? {
        return imageUrl
    }

    fun setImageUrl(imageUrl: String?) {
        this.imageUrl = imageUrl
    }

    fun getCaption(): String? {
        return caption
    }

    fun setCaption(caption: String?) {
        this.caption = caption
    }

    fun getUserId(): String? {
        return userId
    }

    fun setUserId(userId: String?) {
        this.userId = userId
    }

    fun getTimestamp(): Long {
        return timestamp
    }

    fun setTimestamp(timestamp: Long) {
        this.timestamp = timestamp
    }
}