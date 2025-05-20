package epi.gl4c.finalversion.model


data class User(
    val id: String = "",
    val username: String = "",
    val email: String = "",
    val createdAt: Long = 0L,
    val photoUrl: String? = null,
    val bio: String? = null,
    val isProfileComplete: Boolean = false
)