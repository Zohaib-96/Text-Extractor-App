import com.example.textextracter.OCRResponse
import okhttp3.RequestBody
import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.Url

interface AzureOCRService {
    @POST("vision/v3.1/ocr")
    fun analyzeImage(
        @Header("Ocp-Apim-Subscription-Key") apiKey: String,
        @Body image: RequestBody
    ): Call<OCRResponse>
}