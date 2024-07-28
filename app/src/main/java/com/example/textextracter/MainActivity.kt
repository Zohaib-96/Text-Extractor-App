package com.example.textextracter

import android.Manifest.permission.READ_EXTERNAL_STORAGE
import android.Manifest.permission.READ_MEDIA_IMAGES
import android.Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED
import android.content.ClipboardManager
import android.content.ContentResolver
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.ActionBar
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.textextracter.databinding.ActivityMainBinding
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.IOException

class MainActivity : AppCompatActivity() {
    lateinit var binding: ActivityMainBinding
    private val GALLERY_REQUEST_CODE = 102
    // Replace it with your Key
    private val apiKey = "Your-key"
    private lateinit var imageUri: Uri

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // Define ActionBar object
        val actionBar: ActionBar? = supportActionBar
        val colorDrawable = ColorDrawable(Color.parseColor("#0F9D58"))
        actionBar!!.setBackgroundDrawable(colorDrawable)


        // Register Permission
        checkAndRequestPermissions()

        // initial state of button is invisible
        binding.btnCamera.visibility = View.GONE
        binding.btnGallery.visibility = View.GONE
        binding.addFab.shrink()
        binding.addFab.setOnClickListener {
            binding.btnCamera.visibility = View.VISIBLE
            binding.btnGallery.visibility = View.VISIBLE
            binding.addFab.extend()
        }

        // Scrolling TextView
        //binding.imgText.movementMethod = ScrollingMovementMethod()


        // Pick Image From Camera
        imageUri = createImageUri()!!
        val cameraContract =
            registerForActivityResult(ActivityResultContracts.TakePicture()) { success ->
                if (success) {
                    val resizedBitmap = resizeBitmapUri(contentResolver, imageUri)
                    val imageBytes = resizedBitmap?.let { bitmapToByteArray(it) }
                    uploadImageToAzure(imageBytes!!)
                }
            }

        // Camera Button
        binding.btnCamera.setOnClickListener {
            cameraContract.launch(imageUri)
        }

        // Gallery Button
        binding.btnGallery.setOnClickListener {
            openGallery()
        }

        // set text to be selectable
        binding.imgText.setTextIsSelectable(true)

        // Tap Icon To Copy Text From TextView
        binding.copyText.setOnClickListener {
            val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            clipboard.text = binding.imgText.text
            Toast.makeText(this, "Copied to clipboard", Toast.LENGTH_SHORT).show()
        }

    }

    // Resize the Camera captured Image
    private fun resizeBitmapUri(contentResolver: ContentResolver, uri: Uri): Bitmap? {
        return try {
            val inputStream = contentResolver.openInputStream(uri)
            val originalBitmap = BitmapFactory.decodeStream(inputStream)
            val maxWidth = 1080
            val maxHeight = 1920
            val scaleWidth = maxWidth.toFloat() / originalBitmap.width
            val scaleHeight = maxHeight.toFloat() / originalBitmap.height
            val scale = Math.min(scaleWidth, scaleHeight)
            Bitmap.createScaledBitmap(
                originalBitmap,
                (originalBitmap.width * scale).toInt(),
                (originalBitmap.height * scale).toInt(),
                true
            )
        } catch (e: IOException) {
            e.printStackTrace()
            null
        }
    }

    // Open Gallery through Intent
    private fun openGallery() {
        val galleryIntent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        startActivityForResult(galleryIntent, GALLERY_REQUEST_CODE)
    }

    // Response from gallery
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == RESULT_OK) {
            if (requestCode == GALLERY_REQUEST_CODE) {
                data?.data?.let { uri ->
                    val imageBitmap = MediaStore.Images.Media.getBitmap(this.contentResolver, uri)
                    val galleryImageBytes = bitmapToByteArray(imageBitmap)
                    uploadImageToAzure(galleryImageBytes)
                }
            }
        }
    }

    // Store Image Using Content Provider
    private fun createImageUri(): Uri? {
        val image = File(applicationContext.filesDir, "my_images.png")
        return FileProvider.getUriForFile(
            applicationContext, "com.example.textextracter.fileprovider", image
        )
    }

    // Convert Bitmap Into ByteArray
    private fun bitmapToByteArray(bitmap: Bitmap): ByteArray {
        val stream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, stream)
        return stream.toByteArray()
    }

    // Upload Image to Azure OCR
    private fun uploadImageToAzure(imageBytes: ByteArray) {
        val requestBody =
            RequestBody.create("application/octet-stream".toMediaTypeOrNull(), imageBytes)
        val call = RetrofitInstance.azureOCRService.analyzeImage(apiKey, requestBody)
        call.enqueue(object : Callback<OCRResponse> {
            override fun onResponse(call: Call<OCRResponse>, response: Response<OCRResponse>) {
                if (response.isSuccessful) {
                    val ocrResponse = response.body()
                    Log.d("ExtractedText", " ocr text Response body ... $ocrResponse")
                    ocrResponse?.let { ocr ->
                        val extractedText = ocr.regions.joinToString("\n") { region ->
                            region.lines.joinToString("\n") { line ->
                                line.words.joinToString(" ") { it.text }
                            }
                        }
                        binding.imgText.text = extractedText

                        binding.btnCamera.visibility = View.GONE
                        binding.btnGallery.visibility = View.GONE
                        Log.d("ExtractedText", "Text Extraction success... $extractedText")

                    }
                } else {
                    Log.d(
                        "ExtractedText", " Ocr Response Failed... ${response.errorBody()?.string()}"
                    )
                }
            }

            override fun onFailure(call: Call<OCRResponse>, t: Throwable) {
                t.printStackTrace()
                Log.d("ExtractedText", "Text Extraction Failed... $t")
            }
        })
    }
    // Request Permission
    private fun checkAndRequestPermissions() {
        val requestPermissions =
            registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { results ->
                results.forEach { (permission, isGranted) ->
                    if (!isGranted) {
                        Toast.makeText(this, "Permission $permission denied", Toast.LENGTH_SHORT)
                            .show()
                        Log.d("PermissionsForApp", "Permission $permission denied")
                    } else {
                        Log.d("PermissionsForApp", "Permission $permission granted")
                    }
                }
            }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            requestPermissions.launch(arrayOf(READ_MEDIA_IMAGES, READ_MEDIA_VISUAL_USER_SELECTED))
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            requestPermissions.launch(arrayOf(READ_MEDIA_IMAGES))
        } else {
            requestPermissions.launch(arrayOf(READ_EXTERNAL_STORAGE))
        }
    }
}

