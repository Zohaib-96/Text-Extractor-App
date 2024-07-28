import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object RetrofitInstance {
    //- Replace `YOUR_AZURE_ENDPOINT` with your actual endpoint.
    private const val BASE_URL = "YOUR_AZURE_ENDPOINT"
    private val retrofit: Retrofit by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    val azureOCRService: AzureOCRService by lazy {
        retrofit.create(AzureOCRService::class.java)
    }
}
