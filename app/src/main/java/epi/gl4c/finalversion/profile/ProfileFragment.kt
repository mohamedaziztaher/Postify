package epi.gl4c.finalversion.profile

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import epi.gl4c.finalversion.R

class ProfileFragment : Fragment() {
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_profile, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Check if we need to show the EditProfileFragment immediately
        val showEditProfile = arguments?.getBoolean("showEditProfile", false) ?: false
        if (showEditProfile) {
            showEditProfile()
        } else {
            childFragmentManager.beginTransaction()
                .replace(R.id.profile_main_container, UserDetailsFragment())
                .commit()
        }
    }

    fun showEditProfile() {
        childFragmentManager.beginTransaction()
            .replace(R.id.profile_main_container, EditProfileFragment())
            .addToBackStack(null) // allow back press to go back
            .commit()
    }
}