package epi.gl4c.finalversion.main

import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.*
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.fragment.app.Fragment
import com.bumptech.glide.Glide
import com.cloudinary.android.MediaManager
import com.cloudinary.android.callback.ErrorInfo
import com.cloudinary.android.callback.UploadCallback
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import epi.gl4c.finalversion.R
import epi.gl4c.finalversion.databinding.BottomSheetImagePickerBinding
import epi.gl4c.finalversion.databinding.FragmentAddPostBinding
import java.io.File


class AddPostFragment : Fragment() {

    private val TAG = "AddPostFragment"

    private var _binding: FragmentAddPostBinding? = null
    private val binding get() = _binding!!

    private var imageUri: Uri? = null
    private lateinit var tempImageFile: File
    private var cloudinaryImageUrl: String? = null  // Store the uploaded image URL

    private lateinit var auth: FirebaseAuth
    private lateinit var publicationsRef: DatabaseReference

    private val cameraLauncher = registerForActivityResult(
        ActivityResultContracts.TakePicture()
    ) { success ->
        if (success && imageUri != null) {
            Glide.with(this).load(imageUri).into(binding.imageView)
        } else {
            Toast.makeText(requireContext(), "Camera capture failed", Toast.LENGTH_SHORT).show()
        }
    }

    private val galleryLauncher = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let {
            imageUri = it
            Glide.with(this).load(it).into(binding.imageView)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        auth = FirebaseAuth.getInstance()
        publicationsRef = FirebaseDatabase.getInstance().getReference("publications")
        Log.d(TAG, "onCreate: Firebase reference: ${publicationsRef.toString()}")
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ) = FragmentAddPostBinding.inflate(inflater, container, false)
        .also { _binding = it }
        .root

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        Log.d(TAG, "onViewCreated: Preparing FileProvider URI and setting up listeners")

        // Prepare FileProvider URI
        tempImageFile = File(requireContext().externalCacheDir, "temp_image.jpg")
        val authority = "${requireContext().packageName}.provider"
        imageUri = FileProvider.getUriForFile(requireContext(), authority, tempImageFile)

        binding.btnChooseImage.setOnClickListener {
            Log.d(TAG, "onViewCreated: Choose Image button clicked")
            showImagePickerSheet()
        }



        // Keep the original button for backward compatibility
        binding.btnUploadImage.setOnClickListener {
            Log.d(TAG, "onViewCreated: Upload Image button clicked")
            uploadToCloudinaryAndSave() // Keep original functionality
        }
    }

    private fun showImagePickerSheet() {
        val dialog = BottomSheetDialog(requireContext())
        val sheetB = BottomSheetImagePickerBinding.inflate(layoutInflater)
        dialog.setContentView(sheetB.root)

        sheetB.btnCamera.setOnClickListener {
            requestPermissionsThen {
                cameraLauncher.launch(imageUri)
            }
            dialog.dismiss()
        }

        sheetB.btnGallery.setOnClickListener {
            requestPermissionsThen {
                galleryLauncher.launch("image/*")
            }
            dialog.dismiss()
        }

        dialog.show()
    }

    private var pendingAction: (() -> Unit)? = null

    private fun requestPermissionsThen(after: () -> Unit) {
        Log.d(TAG, "requestPermissionsThen: Checking permissions")
        val perms = mutableListOf(android.Manifest.permission.CAMERA)

        // Add READ_EXTERNAL_STORAGE only for Android 12 and below
        if (android.os.Build.VERSION.SDK_INT <= android.os.Build.VERSION_CODES.S_V2) {
            perms.add(android.Manifest.permission.READ_EXTERNAL_STORAGE)
        }

        val neededPermissions = perms.filter {
            ContextCompat.checkSelfPermission(requireContext(), it) != PackageManager.PERMISSION_GRANTED
        }

        if (neededPermissions.isEmpty()) {
            Log.d(TAG, "requestPermissionsThen: All permissions already granted")
            // All permissions are already granted
            after()
        } else {
            Log.d(TAG, "requestPermissionsThen: Requesting permissions: $neededPermissions")
            // Request only the permissions that are not granted
            pendingAction = after
            permissionLauncher.launch(neededPermissions.toTypedArray())
        }
    }

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { perms ->
        val allGranted = perms.entries.all { it.value }
        if (allGranted) {
            Log.d(TAG, "permissionLauncher: All permissions granted")
            // All permissions granted, launch the pending action
            pendingAction?.invoke()
            pendingAction = null
        } else {
            Log.w(TAG, "permissionLauncher: Permissions denied")
            // At least one permission denied
            Toast.makeText(requireContext(), "Permissions denied. Please enable them in settings.", Toast.LENGTH_LONG).show()
        }
    }

    private fun uploadToCloudinary() {
        Log.d(TAG, "uploadToCloudinary: Starting Cloudinary upload")

        if (imageUri == null) {
            Log.w(TAG, "uploadToCloudinary: No image selected")
            Toast.makeText(requireContext(), "Please select an image", Toast.LENGTH_SHORT).show()
            return
        }

        binding.progressBar.visibility = View.VISIBLE
        binding.progressText.visibility = View.VISIBLE
        binding.progressText.text = "Uploading to Cloudinary…"

        MediaManager.get().upload(imageUri)
            .unsigned("my_unsigned_preset")
            .callback(object : UploadCallback {
                override fun onStart(requestId: String?) {
                    Log.d(TAG, "uploadToCloudinary: Upload started with requestId: $requestId")
                }

                override fun onProgress(requestId: String?, bytes: Long, totalBytes: Long) {
                    val pct = (bytes * 100 / totalBytes).toInt()
                    Log.d(TAG, "uploadToCloudinary: Upload progress: $pct%")
                    binding.progressText.text = "Uploading: $pct%"
                }

                override fun onSuccess(requestId: String?, resultData: MutableMap<Any?, Any?>?) {
                    val imageUrl = resultData?.get("secure_url") as? String
                    Log.d(TAG, "uploadToCloudinary: Upload successful, imageUrl: $imageUrl")

                    if (imageUrl != null) {
                        cloudinaryImageUrl = imageUrl
                        Toast.makeText(requireContext(), "Image uploaded successfully! Now you can publish.", Toast.LENGTH_SHORT).show()
                    } else {
                        Log.w(TAG, "uploadToCloudinary: Upload succeeded but no URL returned")
                        Toast.makeText(requireContext(), "Upload succeeded but no URL", Toast.LENGTH_SHORT).show()
                    }

                    binding.progressBar.visibility = View.GONE
                    binding.progressText.visibility = View.GONE
                }

                override fun onError(requestId: String?, error: ErrorInfo?) {
                    Log.e(TAG, "uploadToCloudinary: Upload failed: ${error?.description}")
                    Toast.makeText(requireContext(), "Upload failed: ${error?.description}", Toast.LENGTH_LONG).show()
                    binding.progressBar.visibility = View.GONE
                    binding.progressText.visibility = View.GONE
                }

                override fun onReschedule(requestId: String?, error: ErrorInfo?) {
                    Log.d(TAG, "uploadToCloudinary: Upload rescheduled")
                }
            })
            .dispatch()
    }

    private fun publishToFirebase() {
        val caption = binding.etCaption.text.toString().trim()
        Log.d(TAG, "publishToFirebase: Preparing to publish post with caption: $caption")

        if (cloudinaryImageUrl == null) {
            Log.w(TAG, "publishToFirebase: No image URL available")
            Toast.makeText(requireContext(), "Please upload an image to Cloudinary first", Toast.LENGTH_SHORT).show()
            return
        }

        if (caption.isEmpty()) {
            Log.w(TAG, "publishToFirebase: Caption is empty")
            Toast.makeText(requireContext(), "Please enter a caption", Toast.LENGTH_SHORT).show()
            return
        }

        binding.progressBar.visibility = View.VISIBLE
        binding.progressText.visibility = View.VISIBLE
        binding.progressText.text = "Publishing to Firebase…"

        savePublicationToFirebase(cloudinaryImageUrl!!, caption)
    }

    // Original method kept for backward compatibility
    private fun uploadToCloudinaryAndSave() {
        val caption = binding.etCaption.text.toString().trim()
        Log.d(TAG, "uploadToCloudinaryAndSave: Starting upload with caption: $caption")

        if (imageUri == null) {
            Log.w(TAG, "uploadToCloudinaryAndSave: No image selected")
            Toast.makeText(requireContext(), "Please select an image", Toast.LENGTH_SHORT).show()
            return
        }
        if (caption.isEmpty()) {
            Log.w(TAG, "uploadToCloudinaryAndSave: Caption is empty")
            Toast.makeText(requireContext(), "Please enter a caption", Toast.LENGTH_SHORT).show()
            return
        }

        binding.progressBar.visibility = View.VISIBLE
        binding.progressText.visibility = View.VISIBLE
        binding.progressText.text = "Uploading…"

        MediaManager.get().upload(imageUri)
            .unsigned("my_unsigned_preset")
            .callback(object : UploadCallback {
                override fun onStart(requestId: String?) {
                    Log.d(TAG, "uploadToCloudinaryAndSave: Upload started with requestId: $requestId")
                }

                override fun onProgress(requestId: String?, bytes: Long, totalBytes: Long) {
                    val pct = (bytes * 100 / totalBytes).toInt()
                    Log.d(TAG, "uploadToCloudinaryAndSave: Upload progress: $pct%")
                    binding.progressText.text = "Uploading: $pct%"
                }

                override fun onSuccess(requestId: String?, resultData: MutableMap<Any?, Any?>?) {
                    val imageUrl = resultData?.get("secure_url") as? String
                    Log.d(TAG, "uploadToCloudinaryAndSave: Upload successful, imageUrl: $imageUrl")
                    if (imageUrl != null) {
                        savePublicationToFirebase(imageUrl, caption)
                    } else {
                        Log.w(TAG, "uploadToCloudinaryAndSave: Upload succeeded but no URL returned")
                        Toast.makeText(requireContext(), "Upload succeeded but no URL", Toast.LENGTH_SHORT).show()
                    }
                    binding.progressBar.visibility = View.GONE
                    binding.progressText.visibility = View.GONE
                }

                override fun onError(requestId: String?, error: ErrorInfo?) {
                    Log.e(TAG, "uploadToCloudinaryAndSave: Upload failed: ${error?.description}")
                    Toast.makeText(requireContext(), "Upload failed: ${error?.description}", Toast.LENGTH_LONG).show()
                    binding.progressBar.visibility = View.GONE
                    binding.progressText.visibility = View.GONE
                }

                override fun onReschedule(requestId: String?, error: ErrorInfo?) {
                    Log.d(TAG, "uploadToCloudinaryAndSave: Upload rescheduled")
                }
            })
            .dispatch()
    }

    private fun savePublicationToFirebase(imageUrl: String, caption: String) {
        Log.d(TAG, "savePublicationToFirebase: Preparing to save post to Firebase")
        val user = auth.currentUser ?: run {
            Log.w(TAG, "savePublicationToFirebase: User not authenticated")
            Toast.makeText(requireContext(), "User not authenticated", Toast.LENGTH_SHORT).show()
            return
        }
        val postId = publicationsRef.push().key ?: run {
            Log.w(TAG, "savePublicationToFirebase: Could not generate post ID")
            Toast.makeText(requireContext(), "Could not generate post ID", Toast.LENGTH_SHORT).show()
            return
        }

        val publication = mapOf(
            "id" to postId,
            "userId" to user.uid,
            "caption" to caption,
            "imageUrl" to imageUrl,
            "timestamp" to System.currentTimeMillis(),
            "comments" to emptyMap<String, Any>()
        )

        Log.d(TAG, "savePublicationToFirebase: Post data: $publication")

        publicationsRef.child(postId)
            .setValue(publication)
            .addOnSuccessListener {
                Log.d(TAG, "Post saved successfully with ID: $postId")
                Toast.makeText(requireContext(), "Post saved!", Toast.LENGTH_SHORT).show()
                parentFragmentManager.beginTransaction()
                    .replace(R.id.fragment_container, HomeFragment())
                    .commit()
                requireActivity()
                    .findViewById<BottomNavigationView>(R.id.bottom_navigation)
                    ?.selectedItemId = R.id.nav_home
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Error saving post: ${e.message}")
                Toast.makeText(requireContext(), "Error saving post: ${e.message}", Toast.LENGTH_LONG).show()
            }
            .addOnCompleteListener {
                Log.d(TAG, "Post save task completed: ${it.isSuccessful}")
            }
            .also {
                binding.progressBar.visibility = View.GONE
                binding.progressText.visibility = View.GONE
            }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
