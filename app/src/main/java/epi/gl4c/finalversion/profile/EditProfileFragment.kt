package epi.gl4c.finalversion.profile

import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import com.bumptech.glide.Glide
import com.cloudinary.android.MediaManager
import com.cloudinary.android.callback.ErrorInfo
import com.cloudinary.android.callback.UploadCallback
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import epi.gl4c.finalversion.databinding.BottomSheetImagePickerBinding
import epi.gl4c.finalversion.databinding.FragmentEditProfileBinding
import java.io.File

class EditProfileFragment : Fragment() {
    private var _binding: FragmentEditProfileBinding? = null
    private val binding get() = _binding!!

    private var imageUri: Uri? = null
    private lateinit var tempImageFile: File
    private var cloudinaryImageUrl: String? = null

    private lateinit var auth: FirebaseAuth
    private lateinit var userRef: DatabaseReference

    private val TAG = "EditProfileFragment"

    private val cameraLauncher = registerForActivityResult(
        ActivityResultContracts.TakePicture()
    ) { success ->
        if (success && imageUri != null) {
            Glide.with(this).load(imageUri).into(binding.imgProfile)
        } else {
            Toast.makeText(requireContext(), "Camera capture failed", Toast.LENGTH_SHORT).show()
        }
    }

    private val galleryLauncher = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let {
            imageUri = it
            Glide.with(this).load(it).into(binding.imgProfile)
        }
    }

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { perms ->
        val allGranted = perms.entries.all { it.value }
        if (allGranted) {
            Toast.makeText(requireContext(), "Permissions granted", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(requireContext(), "Permissions denied. Please enable them in settings.", Toast.LENGTH_LONG).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        auth = FirebaseAuth.getInstance()
        userRef = FirebaseDatabase.getInstance().getReference("users")

    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentEditProfileBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)


        tempImageFile = File(requireContext().externalCacheDir, "temp_image.jpg")
        val authority = "${requireContext().packageName}.provider"
        imageUri = FileProvider.getUriForFile(requireContext(), authority, tempImageFile)


        loadUserData()

        binding.tvChangePhoto.setOnClickListener {
            showImagePickerSheet()
        }

        binding.btnSave.setOnClickListener {
            binding.progressBar.visibility = View.VISIBLE
            binding.progressText.visibility = View.VISIBLE
            binding.btnSave.isEnabled = false

            if (imageUri != null && cloudinaryImageUrl == null) {
                uploadToCloudinary()
            } else {
                updateUser()
            }
        }
    }

    private fun loadUserData() {
        val uid = auth.currentUser?.uid ?: return

        userRef.child(uid).get().addOnSuccessListener { snapshot ->
            if (snapshot.exists()) {
                val username = snapshot.child("username").getValue(String::class.java) ?: ""
                val bio = snapshot.child("bio").getValue(String::class.java) ?: ""
                val gender = snapshot.child("gender").getValue(String::class.java) ?: ""
                val photoUrl = snapshot.child("photoUrl").getValue(String::class.java)

                // Set the values to views
                binding.etUsername.setText(username)
                binding.etBio.setText(bio)

                if (!photoUrl.isNullOrEmpty()) {
                    cloudinaryImageUrl = photoUrl
                    Glide.with(this).load(photoUrl).into(binding.imgProfile)
                }
            } else {
                Toast.makeText(requireContext(), "No profile data found", Toast.LENGTH_SHORT).show()
            }
        }.addOnFailureListener { e ->
            Toast.makeText(requireContext(), "Failed to load profile: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun showImagePickerSheet() {
        val dialog = BottomSheetDialog(requireContext())
        val sheetBinding = BottomSheetImagePickerBinding.inflate(layoutInflater)
        dialog.setContentView(sheetBinding.root)

        sheetBinding.btnCamera.setOnClickListener {
            requestPermissionsThen {
                cameraLauncher.launch(imageUri)
            }
            dialog.dismiss()
        }

        sheetBinding.btnGallery.setOnClickListener {
            requestPermissionsThen {
                galleryLauncher.launch("image/*")
            }
            dialog.dismiss()
        }

        dialog.show()
    }

    private fun requestPermissionsThen(after: () -> Unit) {
        val perms = mutableListOf(android.Manifest.permission.CAMERA)
        if (android.os.Build.VERSION.SDK_INT <= android.os.Build.VERSION_CODES.S_V2) {
            perms.add(android.Manifest.permission.READ_EXTERNAL_STORAGE)
        }

        val neededPermissions = perms.filter {
            ContextCompat.checkSelfPermission(requireContext(), it) != PackageManager.PERMISSION_GRANTED
        }

        if (neededPermissions.isEmpty()) {
            after()
        } else {
            permissionLauncher.launch(neededPermissions.toTypedArray())
        }
    }

    private fun uploadToCloudinary() {
        if (imageUri == null) {
            Toast.makeText(requireContext(), "Please select an image", Toast.LENGTH_SHORT).show()
            binding.progressBar.visibility = View.GONE
            binding.progressText.visibility = View.GONE
            binding.btnSave.isEnabled = true
            return
        }

        binding.progressText.text = "Uploading image..."

        MediaManager.get().upload(imageUri)
            .unsigned("my_unsigned_preset") // Make sure your Cloudinary unsigned preset is correct
            .callback(object : UploadCallback {
                override fun onStart(requestId: String?) {
                    Log.d(TAG, "Upload started: $requestId")
                }

                override fun onProgress(requestId: String?, bytes: Long, totalBytes: Long) {
                    val percent = (bytes * 100 / totalBytes).toInt()
                    binding.progressText.text = "Uploading: $percent%"
                }

                override fun onSuccess(requestId: String?, resultData: MutableMap<Any?, Any?>?) {
                    cloudinaryImageUrl = resultData?.get("secure_url") as? String
                    if (cloudinaryImageUrl != null) {
                        Toast.makeText(requireContext(), "Image uploaded!", Toast.LENGTH_SHORT).show()
                        updateUser()
                    } else {
                        Toast.makeText(requireContext(), "Upload succeeded but no URL found", Toast.LENGTH_SHORT).show()
                        binding.progressBar.visibility = View.GONE
                        binding.progressText.visibility = View.GONE
                        binding.btnSave.isEnabled = true
                    }
                }

                override fun onError(requestId: String?, error: ErrorInfo?) {
                    Toast.makeText(requireContext(), "Upload failed: ${error?.description}", Toast.LENGTH_LONG).show()
                    binding.progressBar.visibility = View.GONE
                    binding.progressText.visibility = View.GONE
                    binding.btnSave.isEnabled = true
                }

                override fun onReschedule(requestId: String?, error: ErrorInfo?) {
                    Log.d(TAG, "Upload rescheduled")
                }
            })
            .dispatch()
    }
    private fun updateUser() {
        val uid = auth.currentUser?.uid ?: return
        val username = binding.etUsername.text.toString().trim()
        val bio = binding.etBio.text.toString().trim()

        if (username.isEmpty()) {
            Toast.makeText(requireContext(), "Username cannot be empty", Toast.LENGTH_SHORT).show()
            binding.progressBar.visibility = View.GONE
            binding.progressText.visibility = View.GONE
            binding.btnSave.isEnabled = true
            return
        }

        val userMap = mutableMapOf<String, Any>(
            "username" to username,
            "bio" to bio,
            "isProfileComplete" to true
        )
        cloudinaryImageUrl?.let { userMap["photoUrl"] = it }

        userRef.child(uid).updateChildren(userMap)
            .addOnSuccessListener {
                Toast.makeText(requireContext(), "Profile updated successfully!", Toast.LENGTH_SHORT).show()
                binding.progressBar.visibility = View.GONE
                binding.progressText.visibility = View.GONE
                binding.btnSave.isEnabled = true
            }
            .addOnFailureListener { e ->
                Toast.makeText(requireContext(), "Failed to update profile: ${e.message}", Toast.LENGTH_SHORT).show()
                binding.progressBar.visibility = View.GONE
                binding.progressText.visibility = View.GONE
                binding.btnSave.isEnabled = true
            }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
