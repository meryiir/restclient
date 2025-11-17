package ma.projet.restclient.config;

import java.util.concurrent.TimeUnit;

import okhttp3.OkHttpClient;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;
import retrofit2.converter.simplexml.SimpleXmlConverterFactory;

public class RetrofitClient {
    private static Retrofit retrofit = null;
    private static String currentFormat = null;
    // Changez le port ici si votre serveur utilise un autre port (ex: 8080, 8081, 3000, etc.)
    private static final String BASE_URL = "http://10.0.2.2:8080/api/";

    public static Retrofit getClient(String converterType) {
        // Vérifier si le Retrofit existant peut être réutilisé
        if (retrofit == null || !converterType.equals(currentFormat)) {
            currentFormat = converterType;
            
            // Configuration OkHttp avec timeout
            OkHttpClient okHttpClient = new OkHttpClient.Builder()
                    .connectTimeout(10, TimeUnit.SECONDS)
                    .readTimeout(30, TimeUnit.SECONDS)
                    .writeTimeout(30, TimeUnit.SECONDS)
                    .build();
            
            Retrofit.Builder builder = new Retrofit.Builder()
                    .baseUrl(BASE_URL)
                    .client(okHttpClient);

            // Ajouter le convertisseur approprié en fonction du type demandé
            if ("JSON".equals(converterType)) {
                builder.addConverterFactory(GsonConverterFactory.create());
            } else if ("XML".equals(converterType)) {
                builder.addConverterFactory(SimpleXmlConverterFactory.createNonStrict());
            }

            retrofit = builder.build();
        }
        return retrofit;
    }
}
