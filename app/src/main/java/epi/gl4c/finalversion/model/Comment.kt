package epi.gl4c.finalversion.model

class Comment {
    var userId: String? = null
    var text: String? = null
    var timestamp: Long = 0

    constructor()

    constructor(userId: String?, text: String?, timestamp: Long) {
        this.userId = userId
        this.text = text
        this.timestamp = timestamp
    }
}