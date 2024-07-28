# **Overview**
The Text Extractor App is an Android application that extracts text from images using Azure OCR. The app supports capturing images from the camera or selecting from the gallery, handling both printed and handwritten text. Additionally, it supports multiple languages, making it versatile for various use cases.

## **Features**
- **Capture Image from Camera**: Users can capture images directly using the device's camera.
- **Select Image from Gallery**: Users can select images from the device's gallery.
- **Extract Text**: The app extracts both printed and handwritten text from images.
- **Multi-Language Support**: Supports text extraction in multiple languages.

   <img width="188" alt="XML Latout" src="https://github.com/user-attachments/assets/9b41a94b-d871-4a0e-ae71-4ab32456964e">

## **Setup**
### **Set up Azure Cognitive Services**
**Azure OCR Configuration:**
- Sign up for an Azure account and create an Azure Computer Vision resource.
- Note down the endpoint and subscription key.
- Add Azure OCR credentials:
  - In the app's code, locate the section where the Azure OCR service is called.
  - Replace the placeholder API key and endpoint with your actual Azure OCR subscription key and endpoint.

### **Choose an Image Source**
- **Camera**: Capture a new image.
- **Gallery**: Select an existing image from the gallery.
- **Extract text**: The app processes the image and extracts the text.
- **View results**: The extracted text is displayed overlaying the image.

## **Code Structure**
- **MainActivity.kt**: Handles the main logic for image selection and text extraction.
- **AzureOCRService.kt**: Retrofit service for making API calls to Azure OCR.
- **OCRResponse.kt**: Data model for handling the response from Azure OCR.
- **activity_main.xml**: Layout file for the main activity, including UI components like buttons and text views.	

## **Add Dependencies**
Add the following dependencies in your `build.gradle` file:

```groovy
implementation("com.squareup.retrofit2:retrofit:2.11.0")
implementation("com.squareup.retrofit2:converter-gson:2.11.0")
implementation("com.squareup.okhttp3:okhttp:4.9.0")
val work_version = "2.9.0"
// Kotlin + coroutines
implementation("androidx.work:work-runtime-ktx:$work_version")


   

Request Permissions
 - Add the necessary permissions to your AndroidManifest.xml:

<uses-permission android:name="android.permission.READ_EXTERNAL_STORAGE"/>
<uses-permission android:name="android.permission.READ_MEDIA_IMAGES"/>
<uses-permission android:name="android.permission.READ_MEDIA_VISUAL_USER_SELECTED"/>
<uses-permission android:name="android.permission.INTERNET"/>


 // Request Permission In Main Activity
private fun checkAndRequestPermissions() {
    val requestPermissions =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { results ->
            results.forEach { (permission, isGranted) ->
                if (!isGranted) {
                    Toast.makeText(this, "Permission $permission denied", Toast.LENGTH_SHORT).show()
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



   
