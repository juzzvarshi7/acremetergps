package com.example.acremetergps;

import android.content.Context;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class RetrofitClient {
    private static Retrofit retrofit = null;

    private static Context appContext;
    public static void init(Context context) {
        appContext = context.getApplicationContext(); // Store safe context
    }
    public static Retrofit getInstance() {
        if (retrofit == null) {
            String BASE_URL = new PreferenceManager(appContext).getServerUrl();
            Gson gson = new GsonBuilder()
                    .registerTypeAdapter(LogModel.class, new LogModelDeserializer())
                    .create();

            retrofit = new Retrofit.Builder()
                    .baseUrl(BASE_URL)
                    .addConverterFactory(GsonConverterFactory.create(gson)) // <-- pass custom gson here
                    .build();
        }
        return retrofit;
    }
}